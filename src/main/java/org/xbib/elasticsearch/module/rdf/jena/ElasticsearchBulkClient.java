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

import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

public class ElasticsearchBulkClient {

    private final static ESLogger logger = ESLoggerFactory.getLogger(ElasticsearchBulkClient.class.getName());

    private final Client client;

    private final BulkProcessor bulkProcessor;

    private final String index;

    private final String type;

    private volatile boolean closed = false;

    public ElasticsearchBulkClient(Client client, String index, String type) {
        this(client, index, type, 10000, Runtime.getRuntime().availableProcessors(),
                ByteSizeValue.parseBytesSizeValue("10m"), TimeValue.timeValueSeconds(5));
    }

    public ElasticsearchBulkClient(Client client, String index, String type,
                                   int maxActionsPerBulkRequest,
                                   int maxConcurrentBulkRequests,
                                   ByteSizeValue maxVolumePerBulkRequest,
                                   TimeValue flushInterval) {
        this.client = client;
        this.index = index;
        this.type = type;
        BulkProcessor.Listener listener = new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
                logger.debug("before bulk request: {}, {} requests", executionId, request.numberOfActions());
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                logger.debug("after bulk request: {}, hasFailures = {} ", executionId, response.hasFailures());
            }

            @Override
            public void afterBulk(long executionId, BulkRequest requst, Throwable failure) {
                closed = true;
            }
        };
        BulkProcessor.Builder builder = BulkProcessor.builder(client, listener)
                .setBulkActions(maxActionsPerBulkRequest)
                .setConcurrentRequests(maxConcurrentBulkRequests)
                .setFlushInterval(flushInterval);
        if (maxVolumePerBulkRequest != null) {
            builder.setBulkSize(maxVolumePerBulkRequest);
        }
        this.bulkProcessor = builder.build();
        this.closed = false;
    }

    public ElasticsearchBulkClient createIndex() throws IOException {
        try {
            CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(index);
            InputStream in = getClass().getResourceAsStream("mapping.json");
            if (in != null) {
                StringWriter sw = new StringWriter();
                Streams.copy(new InputStreamReader(in), sw);
                createIndexRequestBuilder.addMapping(type, sw.toString());
            }
            CreateIndexResponse response = createIndexRequestBuilder.execute().actionGet();
            if (!response.isAcknowledged()) {
                throw new ElasticsearchIllegalStateException("create index not acknowledged");
            }
        } catch (IndexAlreadyExistsException e) {
            // ignore
        }
        return this;
    }

    public ElasticsearchBulkClient refreshIndex() throws IOException {
        client.admin().indices().prepareRefresh(index).setForce(true).execute().actionGet();
        return this;
    }

    public ElasticsearchBulkClient index(XContentBuilder builder) {
        if (closed) {
            throw new ElasticsearchIllegalStateException("client is closed");
        }
        try {
            bulkProcessor.add(new IndexRequest(index).type(type).create(false).source(builder));
        } catch (Exception e) {
            closed = true;
        }
        return this;
    }

    public void close() {
        bulkProcessor.close();
        client.close();
    }
}
