/**
 *    Copyright 2014 Jörg Prante
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbib.elasticsearch.module.rdf.jena;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.GraphEvents;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.WrappedIterator;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.xbib.elasticsearch.module.rdf.jena.NTriples.asNt;
import static org.xbib.elasticsearch.module.rdf.jena.NTriples.asNtURI;

public class ElasticsearchRDFGraph extends GraphBase implements Datatypes {

    private final Node node;

    private final Client client;

    private final String index;

    private final String type;

    private final SearchRequestBuilder searchRequestBuilder;

    public ElasticsearchRDFGraph(Node node, Client client, String index, String type, SearchRequestBuilder searchRequestBuilder) {
        this.node = node;
        this.client = client;
        this.index = index;
        this.type = type;
        this.searchRequestBuilder = searchRequestBuilder;
    }

    @Override
    public void performAdd(final Triple triple) {
        // TODO
    }

    @Override
    public void performDelete(final Triple triple) {
        // TODO
    }

    @Override
    protected int graphBaseSize() {
        try {
            return (int) countTriples();
        } catch (IOException e) {
            //
            return -1;
        }
    }

    @Override
    public void clear() {
        getEventManager().notifyEvent(this, GraphEvents.removeAll);
    }

    @Override
    public ExtendedIterator<Triple> graphBaseFind(final TripleMatch pattern) {
        return WrappedIterator.createNoRemove(query(pattern));
    }

    private long countTriples() throws IOException {
        SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder(client)
                .setSearchType(SearchType.COUNT)
                .setQuery(QueryBuilders.matchAllQuery());
        return searchRequestBuilder.execute().actionGet().getHits().getTotalHits();
    }

    private Iterator<Triple> query(TripleMatch query) {
        BoolFilterBuilder filterBuilder = FilterBuilders.boolFilter();
        final Node s = query.getMatchSubject();
        final Node p = query.getMatchPredicate();
        final Node o = query.getMatchObject();
        if (s != null) {
            filterBuilder.must(termFilter(Field.S, asNt(s)));
        }
        if (p != null) {
            filterBuilder.must(termFilter(Field.P, asNt(p)));
        }
        if (o != null) {
            if (o.isLiteral()) {
                final String language = o.getLiteralLanguage();
                if (language != null && !language.isEmpty()) {
                    filterBuilder.must(termFilter(Field.LANG, language));
                }
                final String literalValue = o.getLiteralLexicalForm();
                final RDFDatatype dataType = o.getLiteralDatatype();
                if (dataType != null) {
                    final String uri = dataType.getURI();
                    if (XSD_BOOLEAN.equals(uri)) {
                        filterBuilder.must(termFilter(Field.BOOLEAN_OBJECT, literalValue));
                    } else if (XSD_INT.equals(uri) ||
                            XSD_INTEGER.equals(uri) ||
                            XSD_LONG.equals(uri)) {
                        filterBuilder.must(termFilter(Field.LONG_OBJECT, literalValue));
                    } else if (XSD_DECIMAL.equals(uri) ||
                            XSD_DOUBLE.equals(uri)) {
                        filterBuilder.must(termFilter(Field.DOUBLE_OBJECT, literalValue));
                    } else if (XSD_DATE.equals(uri) ||
                                    XSD_DATETIME.equals(uri)) {
                        filterBuilder.must(termFilter(Field.DATE_OBJECT, literalValue));
                    } else {
                        filterBuilder.must(termFilter(Field.STRING_OBJECT, literalValue));
                    }
                } else {
                    filterBuilder.must(termFilter(Field.STRING_OBJECT, literalValue));
                }
            } else {
                filterBuilder.must(termFilter(Field.STRING_OBJECT, asNt(o)));
            }
        }
        if (node != null) {
            filterBuilder.must(termFilter(Field.C, asNtURI(o)));
        }
        searchRequestBuilder.setSearchType(SearchType.SCAN)
                .setScroll(TimeValue.timeValueMillis(5000)) // should be a parameter
                .setSize(1000); // should be a parameter
        if (index != null) {
            searchRequestBuilder.setIndices(index);
        }
        if (type != null) {
            searchRequestBuilder.setTypes(type);
        }
        searchRequestBuilder.addFields(Field.S, Field.P, Field.O, Field.C,
                Field.BOOLEAN_OBJECT, Field.DOUBLE_OBJECT, Field.LONG_OBJECT,
                Field.DATE_OBJECT, Field.STRING_OBJECT, Field.LANG);
        if (filterBuilder.hasClauses()) {
            searchRequestBuilder.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filterBuilder));
        } else {
            searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery());
        }
        return new ScanScroll(searchRequestBuilder);
    }

    class ScanScroll implements Iterator<Triple> {

        SearchResponse searchResponse;

        List<Triple> triples;

        Iterator<Triple> iterator;

        ScanScroll(SearchRequestBuilder searchRequestBuilder) {
            searchResponse = searchRequestBuilder.execute().actionGet();
            triples = new ArrayList<Triple>();
            iterator = triples.iterator();
            scroll();
        }

        private void scroll() {
            if (searchResponse.getScrollId() != null) {
                searchResponse = client.prepareSearchScroll(searchResponse.getScrollId())
                        .setScroll(TimeValue.timeValueMillis(5000))
                        .execute().actionGet();
                SearchHits hits = searchResponse.getHits();
                triples = new ArrayList<Triple>();
                if (hits.getHits().length > 0) {
                    for (SearchHit hit : hits) {
                        triples.add(Triple.create(
                                NTriples.asURIorBlankNode((String) hit.field(Field.S).getValue()),
                                NTriples.asURI((String) hit.field(Field.P).getValue()),
                                NTriples.asNode((String) hit.field(Field.O).getValue())));
                    }
                }
                iterator = triples.iterator();
            }
        }

        @Override
        public boolean hasNext() {
            boolean b = iterator.hasNext();
            if (!b) {
                scroll();
                b = iterator.hasNext();
            }
            return b;
        }

        @Override
        public Triple next() {
            return iterator.next();
        }
    }

}