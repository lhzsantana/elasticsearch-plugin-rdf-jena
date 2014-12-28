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

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.DatasetGraphCaching;
import com.hp.hpl.jena.sparql.core.Quad;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;

import java.util.Iterator;

public class ElasticsearchRDFDatasetGraph extends DatasetGraphCaching {

    private final Client client;

    private final String index;

    private final String type;

    private final SearchRequestBuilder searchRequestBuilder;

    public ElasticsearchRDFDatasetGraph(Client client, String index, String type, SearchRequestBuilder searchRequestBuilder) {
        this.client = client;
        this.index = index;
        this.type = type;
        this.searchRequestBuilder = searchRequestBuilder;
    }

    @Override
    public Iterator<Node> listGraphNodes() {
        return namedGraphs.keys();
    }

    @Override
    protected void _close() {
    }

    @Override
    protected Graph _createNamedGraph(final Node graphNode) {
        return new ElasticsearchRDFGraph(graphNode, client, index, type, searchRequestBuilder);
    }

    @Override
    protected Graph _createDefaultGraph() {
        return new ElasticsearchRDFGraph(null, client, index, type, searchRequestBuilder);
    }

    @Override
    protected boolean _containsGraph(final Node graphNode) {
        return false;
    }

    @Override
    protected void addToDftGraph(final Node s, final Node p, final Node o) {
        getDefaultGraph().add(new Triple(s, p, o));
    }

    @Override
    protected void addToNamedGraph(final Node g, final Node s, final Node p, final Node o) {
    }

    @Override
    protected void deleteFromDftGraph(final Node s, final Node p, final Node o) {
        getDefaultGraph().delete(new Triple(s, p, o));
    }

    @Override
    protected void deleteFromNamedGraph(final Node g, final Node s, final Node p, final Node o) {
    }

    @Override
    protected Iterator<Quad> findInDftGraph(final Node s, final Node p, final Node o) {
        return triples2quads(Quad.tripleInQuad, getDefaultGraph().find(s, p, o));
    }

    @Override
    protected Iterator<Quad> findInSpecificNamedGraph(final Node g, final Node s, final Node p, final Node o) {
        return null;
    }

    @Override
    protected Iterator<Quad> findInAnyNamedGraphs(final Node s, final Node p, final Node o) {
        return null;
    }
}