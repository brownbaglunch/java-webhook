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

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.io.Streams;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.TestAbortedException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
class WebhookIT {

    private final static Logger logger = LogManager.getLogger(WebhookIT.class);
    private static RestHighLevelClient client = null;
    private final static String ES_CLUSTER = "http://localhost:9200";
    private final static int HTTP_PORT = 8085;
    private static HttpServer githubMockServer;

    @BeforeAll
    static void createRestClient(Vertx vertx, VertxTestContext testContext) throws IOException {
        logger.debug(" -> Create REST Client");
        client = new RestHighLevelClient(RestClient.builder(HttpHost.create(ES_CLUSTER)));
        try {
            logger.debug(" -> Test REST Client");
            MainResponse info = client.info();
            logger.info(" -> Connected to node {}, cluster {}, version {}", info.getNodeName(), info.getClusterName(), info.getVersion().toString());
            // Check that we have no data
            SearchResponse response = client.search(new SearchRequest("bblfr"));
            logger.warn("We already have {} documents existing in {}. They are going to be removed.",
                    response.getHits().totalHits, ES_CLUSTER);
        } catch (ConnectException e) {
            throw new TestAbortedException("Elasticsearch is not running at " + ES_CLUSTER + ". Skipping tests.");
        } catch (ElasticsearchStatusException e) {
            assertEquals(404, e.status().getStatus());
        }

        logger.debug(" -> Clean data");
        client.indices().deleteIndex(new DeleteIndexRequest("bblfr_speakers*"));

        // Create Elasticsearch Vertice
        ElasticsearchVerticle elasticsearchVerticle = new ElasticsearchVerticle(ES_CLUSTER);

        // Create Webhook Vertice
        WebhookVerticle webhookVerticle = new WebhookVerticle(HTTP_PORT, null, "http://localhost:" + (HTTP_PORT + 1), "gh-pages", "/speakers.js");

        vertx.deployVerticle(elasticsearchVerticle, testContext.succeeding(id -> {
            logger.info(" -> elasticsearch verticle deployed {}", id);
        }));

        vertx.deployVerticle(webhookVerticle, testContext.succeeding(id -> {
            logger.info(" -> webhook verticle deployed {}", id);
        }));

        // We are going to create also a Github Mock Webserver to serve the files we need
        githubMockServer = vertx.createHttpServer();

        Router router = Router.router(vertx);
        Route route = router.route(HttpMethod.GET, "/gh-pages/:filename");

        route.handler(routingContext -> {
            String filename = routingContext.request().getParam("filename");

            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            try {
                logger.warn("filename=[{}]", filename);
                InputStream in = WebhookIT.class.getResourceAsStream("/fr/brownbaglunch/webhook/" + filename);
                if (in == null) {
                    throw new FileNotFoundException();
                }
                String content = Streams.copyToString(new InputStreamReader(in));
                logger.debug("{}", content);
                response.end(content);
            } catch (IOException e) {
                response.setStatusCode(404);
                response.end("Failed");
            }
        });

        githubMockServer.requestHandler(router::accept);
        githubMockServer.listen(HTTP_PORT + 1);
    }

    @AfterAll
    static void stopRestClient() throws IOException {
        if (client != null) {
            logger.debug(" -> Stopping REST Client");
            client.close();
        }
        client = null;

        if (githubMockServer != null) {
            githubMockServer.close();
        }
        githubMockServer = null;
    }

    @Test
    void testRunning(Vertx vertx, VertxTestContext testContext) throws IOException {
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
    void testCallGithub(Vertx vertx, VertxTestContext testContext) throws IOException, InterruptedException {
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
}
