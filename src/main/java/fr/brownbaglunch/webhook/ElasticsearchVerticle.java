/*
 * Licensed to Brownbaglunch.fr under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Brownbaglunch.fr licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fr.brownbaglunch.webhook;

import fr.brownbaglunch.webhook.data.BblDataReader;
import fr.brownbaglunch.webhook.model.City;
import fr.brownbaglunch.webhook.model.Speaker;
import fr.brownbaglunch.webhook.service.ElasticsearchClientManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.xcontent.XContentType;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Map;

/**
 * You need to set the TARGET system property to something like:
 * https://username:password@yourcluster.found.io:9243
 * It defaults to http://localhost:9200
 */
public class ElasticsearchVerticle extends AbstractVerticle {

    private static final Logger logger = LogManager.getLogger(ElasticsearchVerticle.class);

    private final String url;

    private ElasticsearchClientManager elasticsearchClientManager;

    ElasticsearchVerticle(String url) {
        this.url = url;
    }

    @Override
    public void start(Future<Void> futureVertx) {
        logger.debug("Starting Elasticsearch Verticle");

        // Create elasticsearch client
        elasticsearchClientManager = startElasticsearch(url);
        if (elasticsearchClientManager == null) {
            logger.info("Closing Vertx...");
            throw new NoNodeAvailableException("No node running at " + url);
        }

        logger.debug("Elasticsearch Client has been created");

        MessageConsumer<String> consumer = vertx.eventBus().consumer("elasticsearch.index");
        consumer.handler(this::handleMessage);
        futureVertx.complete();
    }

    @Override
    public void stop() {
        if (elasticsearchClientManager != null) {
            elasticsearchClientManager.close();
        }
        logger.debug("Elasticsearch Client stopped");
    }

    /**
     * We start elasticsearch and return true if ok
     * @return true if OK or false if KO
     */
    private static ElasticsearchClientManager startElasticsearch(String url) {
        ElasticsearchClientManager elasticsearchClientManager = null;
        try {
            elasticsearchClientManager = new ElasticsearchClientManager(url);
            // Create elasticsearch client
            elasticsearchClientManager.start();
            return elasticsearchClientManager;
        } catch (Exception e) {
            logger.error("Can not start elasticsearch client.", e);
            if (elasticsearchClientManager != null) {
                elasticsearchClientManager.close();
            }
            return null;
        }
    }

    private void handleMessage(Message<String> message) {
        logger.debug("ElasticsearchVerticle got a message from the bus: ", message.body());

        SharedData sd = vertx.sharedData();

        LocalMap<String, City> cities = sd.getLocalMap("cities");
        LocalMap<String, Speaker> speakers = sd.getLocalMap("speakers");

        logger.info("Let's index {} speakers and {} cities.", speakers.size(), cities.size());

        // Check that elasticsearch is still running, otherwise, reinit
        try {
            elasticsearchClientManager.checkRunning();
        } catch (Exception e) {
            // Elasticsearch is not running anymore. Let's restart it.
            elasticsearchClientManager.close();

            // Create elasticsearch client
            elasticsearchClientManager = startElasticsearch(url);
            if (elasticsearchClientManager == null) {
                logger.info("Closing Vertx...");
                vertx.close();
            }
        }

        String indexName = "bblfr_speakers_" + DateTime.now().toString("yyyyMMddHHmmssSSS");

        // This task can take some time so let's tell that to VertX
        vertx.<BulkResponse>executeBlocking(future -> {
            try {
                BulkRequest request = new BulkRequest();

                for (Map.Entry<String, Speaker> speakerEntry : speakers.entrySet()) {
                    String json = BblDataReader.toJson(speakerEntry.getValue());
                    logger.debug("Indexing {}", speakerEntry.getKey());
                    logger.trace(json);
                    request.add(new IndexRequest(indexName, "doc", speakerEntry.getKey()).source(json, XContentType.JSON));
                }
                request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);

                BulkResponse bulkResponse = elasticsearchClientManager.client().bulk(request);

                future.complete(bulkResponse);
            } catch (Exception e) {
                logger.error("Can not send to elasticsearch ", e);
            }
        }, res -> {
            BulkResponse bulkResponse = res.result();
            logger.info("Indexed in elasticsearch: {}", bulkResponse.hasFailures() ? "with failures" : "OK");
            int errors = 0;
            if (bulkResponse.hasFailures()) {
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.isFailed()) {
                        errors++;
                        logger.warn("Failed for {}: {} ", bulkItemResponse.getId(), bulkItemResponse.getFailureMessage());
                    }
                }
            }

            if (errors < 10) {
                try {
                    // Let say we have few errors, so let's still switch the alias
                    elasticsearchClientManager.client().switchAlias("bblfr_speaker*", indexName, "bblfr");

                    // And remove the old indices
                    elasticsearchClientManager.client().deleteIndex("bblfr_speaker*,-" + indexName);
                } catch (IOException e) {
                    logger.warn("Failed to switch the alias and delete the old index", e);
                }
            }
        });
    }
}
