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
package org.xbib.elasticsearch.rest.rdf.jena;

import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.SortCondition;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.expr.Expr;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedRDFStream;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.xbib.elasticsearch.module.rdf.jena.Datatypes;
import org.xbib.elasticsearch.module.rdf.jena.ElasticsearchBulkClient;
import org.xbib.elasticsearch.module.rdf.jena.ElasticsearchRDFDatasetGraph;
import org.xbib.elasticsearch.module.rdf.jena.Field;
import org.xbib.elasticsearch.module.rdf.jena.QueryDecoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.xbib.elasticsearch.module.rdf.jena.NTriples.asNt;
import static org.xbib.elasticsearch.module.rdf.jena.NTriples.asNtURI;

public class RestJenaAction extends BaseRestHandler implements Datatypes {

    @Inject
    public RestJenaAction(Settings settings, Client client, RestController controller) {
        super(settings, controller, client);
        controller.registerHandler(RestRequest.Method.GET, "/_jena/{index}/{type}", this);
        controller.registerHandler(RestRequest.Method.POST, "_jena/{index}/{type}", this);
        controller.registerHandler(RestRequest.Method.PUT, "/_jena/{index}/{type}", this);
    }

    @Override
    protected void handleRequest(final RestRequest request, RestChannel channel, Client client) throws Exception {
        try {
            final String accept = request.header("Accept") != null ?
                    request.header("Accept") : "text/plain";
            final String contentType = request.header("Content-Type") != null ?
                    request.header("Content-Type") : "text/plain";
            QueryDecoder decoder = new QueryDecoder(request.uri(),
                    request.content() != null ? request.content().toUtf8() : null);
            if (decoder.parameters().get("query", null) != null ||
                    decoder.parameters().get("q") != null) {
                String query = decoder.parameters().get("query", decoder.parameters().get("q", null));
                if (query == null) {
                    throw new ElasticsearchIllegalArgumentException("no query parameter");
                } else {
                    Query sparql = QueryFactory.create(query);
                    int from = (int)(sparql.getOffset() > 0 ? sparql.getOffset() : 0);
                    int size = (int)(sparql.getLimit() > 0 ? sparql.getLimit() : 10);
                    SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder(client)
                            .setFrom(from)
                            .setSize(size);
                    List<SortBuilder> sortBuilders = buildSort(sparql.getOrderBy());
                    if (sortBuilders != null) {
                        for (SortBuilder sortBuilder : sortBuilders) {
                            searchRequestBuilder.addSort(sortBuilder);
                        }
                    }
                    QueryExecution execution = QueryExecutionFactory.create(sparql,
                            DatasetFactory.create(new ElasticsearchRDFDatasetGraph(client,
                                    request.param("index"), request.param("type"), searchRequestBuilder)));
                    RestResponse response = null;
                    switch (sparql.getQueryType()) {
                        case Query.QueryTypeAsk:
                            response = output(execution.execAsk(), accept);
                            break;
                        case Query.QueryTypeSelect:
                            response = output(execution.execSelect(), accept);
                            break;
                        case Query.QueryTypeDescribe:
                            response = output(execution.execDescribe(), accept);
                            break;
                        case Query.QueryTypeConstruct:
                            response = output(execution.execConstruct(), accept);
                            break;
                    }
                    channel.sendResponse(response);
                }
            } else {
                if (request.content() == null) {
                    throw new ElasticsearchIllegalArgumentException("no content for upload");
                }
                final PipedRDFIterator<Triple> iterator = new PipedRDFIterator<Triple>();
                final PipedRDFStream<Triple> inputStream = new PipedTriplesStream(iterator);
                Runnable parser = new Runnable() {
                    @Override
                    public void run() {
                        logger.debug("parser starts, {} {}", contentType, request.content().length());
                        RDFDataMgr.parse(inputStream,
                                request.content().streamInput(),
                                RDFLanguages.contentTypeToLang(contentType));
                    }
                };
                ElasticsearchBulkClient bulkClient = new ElasticsearchBulkClient(client,
                        request.param("index"), request.param("type"));
                bulkClient.createIndex();
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(parser);
                int count = 0;
                while (iterator.hasNext()) {
                    final Triple triple = iterator.next();
                    XContentBuilder builder = jsonBuilder();
                    builder.startObject()
                            .field(Field.S, asNt(triple.getSubject()))
                            .field(Field.P, asNtURI(triple.getPredicate()));
                    String o = asNt(triple.getObject());
                    builder.field(Field.O, o);
                    Node object = triple.getObject();
                    if (object.isLiteral()) {
                        String lang = object.getLiteralLanguage();
                        if (lang != null && !lang.isEmpty()) {
                            builder.field(Field.LANG, lang);
                        }
                        String dataType = object.getLiteralDatatype() != null ?
                                object.getLiteralDatatype().getURI() : null;
                        Object value = object.getLiteralValue();
                        if (XSD_BOOLEAN.equals(dataType)) {
                            builder.field(Field.BOOLEAN_OBJECT, value);
                        } else if (XSD_DECIMAL.equals(dataType)
                                || XSD_DOUBLE.equals(dataType)
                                ) {
                            builder.field(Field.DOUBLE_OBJECT, value);
                        } else if (XSD_INT.equals(dataType)
                                || XSD_INTEGER.equals(dataType)
                                || XSD_LONG.equals(dataType)
                                ) {
                            builder.field(Field.LONG_OBJECT, value);
                        } else if (XSD_DATE.equals(dataType)
                                || XSD_DATETIME.equals(dataType)) {
                            builder.field(Field.DATE_OBJECT, ((XSDDateTime) value).asCalendar().getTime());
                        } else {
                            builder.field(Field.STRING_OBJECT, value);
                        }
                    } else {
                        builder.field(Field.STRING_OBJECT, o);
                    }
                    builder.endObject();
                    bulkClient.index(builder);
                    count++;
                }
                bulkClient.refreshIndex();
                bulkClient.close();
                XContentBuilder builder = jsonBuilder();
                builder.startObject().field("count", count).endObject();
                channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            try {
                channel.sendResponse(new BytesRestResponse(channel, e));
            } catch (IOException x) {
                // ignore
            }
        }
    }

    private List<SortBuilder> buildSort(List<SortCondition> orderby) {
        if (orderby == null || orderby.isEmpty()) {
            return null;
        }
        List<SortBuilder> sortBuilders = new ArrayList<SortBuilder>();
        for (SortCondition sortCondition : orderby) {
            Expr expr = sortCondition.getExpression();
            String varName = expr.getVarName();
            SortOrder sortOrder = sortCondition.getDirection() == Query.ORDER_ASCENDING ?
                    SortOrder.ASC : SortOrder.DESC;
            sortBuilders.add(SortBuilders.fieldSort(varName).order(sortOrder));
        }
        return sortBuilders;
    }

    private RestResponse output(boolean b, String contentType) throws IOException {
        BytesStreamOutput out = new BytesStreamOutput();
        switch (contentType) {
            case "text/plain" :
            case "text/csv" : {
                ResultSetFormatter.outputAsCSV(out, b);
                break;
            }
            case "text/tab-separated-values" : {
                ResultSetFormatter.outputAsTSV(out, b);
                break;
            }
            case "application/sparql-results+xml" : {
                ResultSetFormatter.outputAsXML(out, b);
                break;
            }
            case "application/sparql-results+json" : {
                ResultSetFormatter.outputAsJSON(out, b);
                break;
            }
        }
        return new BytesRestResponse(RestStatus.OK, contentType, out.bytes(), false);
    }

    private RestResponse output(ResultSet resultSet, String contentType) {
        if (resultSet == null) {
            return  new BytesRestResponse(RestStatus.NOT_FOUND);
        }
        BytesStreamOutput out = new BytesStreamOutput();
        switch (contentType) {
            case "text/plain" :
            case "text/csv" : {
                ResultSetFormatter.outputAsCSV(out, resultSet);
                break;
            }
            case "text/tab-separated-values" : {
                ResultSetFormatter.outputAsTSV(out, resultSet);
                break;
            }
            case "application/sparql-results+xml" : {
                ResultSetFormatter.outputAsXML(out, resultSet);
                break;
            }
            case "application/sparql-results+json" : {
                ResultSetFormatter.outputAsJSON(out, resultSet);
                break;
            }
            default : {
                ResultSetFormatter.outputAsJSON(out, resultSet);
                break;
            }
        }
        return new BytesRestResponse(RestStatus.OK, contentType, out.bytes(), false);
    }

    private RestResponse output(Model model, String contentType) {
        if (model == null) {
            return new BytesRestResponse(RestStatus.NOT_FOUND);
        }
        BytesStreamOutput out = new BytesStreamOutput();
        Lang lang = RDFLanguages.contentTypeToLang(contentType);
        if (lang == null) {
            lang = Lang.RDFJSON;
        }
        RDFDataMgr.write(out, model, lang);
        return new BytesRestResponse(RestStatus.OK, contentType, out.bytes(), false);
    }

}
