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
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.TestAbortedException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
abstract class AbstractIT {

    final static Logger staticLogger = LogManager.getLogger(AbstractIT.class);
    final Logger logger = LogManager.getLogger(this.getClass());
    static RestHighLevelClient client = null;
    final static String ES_CLUSTER = "http://localhost:9200";
    final static int HTTP_PORT = 8085;
    private static HttpServer githubMockServer;

    abstract WebhookVerticle buildWebhookVerticle();

    @BeforeAll
    static void createRestClient(Vertx vertx, VertxTestContext testContext) throws IOException {
        staticLogger.debug(" -> Create REST Client");
        client = new RestHighLevelClient(RestClient.builder(HttpHost.create(ES_CLUSTER)));
        try {
            staticLogger.debug(" -> Test REST Client");
            MainResponse info = client.info();
            staticLogger.info(" -> Connected to node {}, cluster {}, version {}", info.getNodeName(), info.getClusterName(), info.getVersion().toString());
            // Check that we have no data
            SearchResponse response = client.search(new SearchRequest("bblfr"));
            staticLogger.warn("We already have {} documents existing in {}. They are going to be removed.",
                    response.getHits().totalHits, ES_CLUSTER);
        } catch (ConnectException e) {
            throw new TestAbortedException("Elasticsearch is not running at " + ES_CLUSTER + ". Skipping tests.");
        } catch (ElasticsearchStatusException e) {
            assertEquals(404, e.status().getStatus());
        }

        staticLogger.debug(" -> Clean data");
        client.indices().deleteIndex(new DeleteIndexRequest("bblfr_speakers*"));

        // We are going to create also a Github Mock Webserver to serve the files we need
        githubMockServer = vertx.createHttpServer();

        Router router = Router.router(vertx);
        Route route = router.route(HttpMethod.GET, "/gh-pages/:filename");

        route.handler(routingContext -> {
            String filename = routingContext.request().getParam("filename");

            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            try {
                staticLogger.warn("filename=[{}]", filename);
                InputStream in = AbstractIT.class.getResourceAsStream("/fr/brownbaglunch/webhook/" + filename);
                if (in == null) {
                    throw new FileNotFoundException();
                }
                String content = Streams.copyToString(new InputStreamReader(in));
                staticLogger.debug("{}", content);
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
            staticLogger.debug(" -> Stopping REST Client");
            client.close();
        }
        client = null;

        if (githubMockServer != null) {
            githubMockServer.close();
        }
        githubMockServer = null;
    }

    @BeforeEach
    void startVertx(Vertx vertx, VertxTestContext testContext) {
        // Create Webhook Vertice
        WebhookVerticle webhookVerticle = buildWebhookVerticle();

        vertx.deployVerticle(webhookVerticle, testContext.succeeding(id -> {
            logger.info(" -> test webhook verticle deployed {}", id);
        }));
    }
}
