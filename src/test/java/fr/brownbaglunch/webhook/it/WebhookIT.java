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

package fr.brownbaglunch.webhook.it;

import fr.brownbaglunch.webhook.WebhookVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WebhookIT extends AbstractIT {

    @Override
    WebhookVerticle buildWebhookVerticle() {
        // Create Webhook Vertice
        return new WebhookVerticle(
                ES_CLUSTER,
                HTTP_PORT, null, "http://localhost:" + (HTTP_PORT + 1), "gh-pages", "/speakers.js");
    }

    @Test
    void testRunning(Vertx vertx, VertxTestContext testContext) {
        // Create the WebClient to connect to our Vertx instance
        WebClient webClient = WebClient.create(vertx,
                new WebClientOptions()
                        .setDefaultHost("localhost")
                        .setDefaultPort(HTTP_PORT));

        logger.info("Reading data from {}", "/");
        HttpRequest<Buffer> request = webClient.get("/");
        request.send(testContext.succeeding(result -> {
            JsonObject json = result.bodyAsJsonObject();
            assertEquals("Vert.x is running!", json.getString("message"));
        }));
    }

    @Test
    void testCallGithub(Vertx vertx, VertxTestContext testContext) {
        // Create the WebClient to connect to our Vertx instance
        WebClient webClient = WebClient.create(vertx,
                new WebClientOptions()
                        .setDefaultHost("localhost")
                        .setDefaultPort(HTTP_PORT));

        logger.info("Reading data from {}", "/");
        HttpRequest<Buffer> request = webClient.post("/");
        Checkpoint searchSuccessful = testContext.checkpoint();
        request.send(testContext.succeeding(result -> {
            JsonObject json = result.bodyAsJsonObject();
            assertEquals((Integer) 2, json.getInteger("speakers"));
            assertEquals((Integer) 38, json.getInteger("cities"));
            assertEquals(false, json.getBoolean("with_failures"));

            // We can check that everything is available in elasticsearch
            vertx.setPeriodic(500, id -> {
                try {
                    logger.debug("  -> Checking Elasticsearch");
                    SearchResponse response = client.search(new SearchRequest("bblfr"));
                    if (response.getHits().totalHits == 2) {
                        searchSuccessful.flag();
                    }
                } catch (ElasticsearchStatusException ignored) {
                    // We can have an index not allocated yet when we start
                } catch (IOException e) {
                    testContext.failNow(e);
                }
            });
        }));
    }

    @Test
    void testWebhookVerticeHasBeenDeployed(Vertx vertx) {
        assertFalse(vertx.deploymentIDs().isEmpty());
    }
}
