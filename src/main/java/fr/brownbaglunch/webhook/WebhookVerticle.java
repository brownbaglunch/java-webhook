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
import fr.brownbaglunch.webhook.util.KeyChecker;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * You may want to set the TOKEN system property that you set in GitHub
 */
public class WebhookVerticle extends AbstractVerticle {

    private final static Logger logger = LogManager.getLogger(WebhookVerticle.class);

    private static final String DEFAULT_SOURCE = "https://raw.githubusercontent.com/brownbaglunch/bblfr_data/gh-pages/baggers.js";
    private static final String DEFAULT_TOKEN = null;
    private static final int DEFAULT_PORT = 8080;

    private final Vertx vertx;

    private final static String source = System.getenv().getOrDefault("SOURCE", System.getProperties().getProperty("SOURCE", DEFAULT_SOURCE));
    private final static String token = System.getenv().getOrDefault("TOKEN", System.getProperties().getProperty("TOKEN", DEFAULT_TOKEN));
    private final static Integer port = Integer.parseInt(System.getenv().getOrDefault("PORT", System.getProperties().getProperty("PORT", "" + DEFAULT_PORT)));
    private WebClient client;

    WebhookVerticle(Vertx vertx) throws IOException {
        this.vertx = vertx;
        logger.info("Starting HTTP server on port {}", port);
    }

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);

        // Create the WebClient to connect to GitHub
        client = WebClient.create(vertx);

        router.route(HttpMethod.GET, "/").handler(routingContext -> {
            // Write to the response and end it
            writeJsonResponse(routingContext, new JsonObject().put("message", "Vert.x is running!").encodePrettily());
        });
        router.route(HttpMethod.POST, "/").handler(routingContext -> {
            logger.debug("POST / has been called.");
            // We need to check the Token is set
            boolean keyIsChecked;
            String signature = routingContext.request().getHeader("X-Hub-Signature");
            String body = routingContext.getBodyAsString();
            logger.trace(body);
            if (signature != null || token != null) {
                keyIsChecked = KeyChecker.testGithubToken(body, signature, token);
            } else {
                logger.warn("Signature has not been verified. Probably Dev Mode.");
                keyIsChecked = true;
            }

            if (keyIsChecked) {
                // This handler will be called by github when data are changing
                logger.info("Reading data from {}", source);
                HttpRequest<Buffer> request = client.getAbs(source);
                request.send(handler -> {
                    if (handler.succeeded()) {
                        String githubData = handler.result().bodyAsString();
                        logger.trace(githubData);
                        try {
                            SharedData sd = vertx.sharedData();

                            LocalMap<String, City> cities = sd.getLocalMap("cities");
                            LocalMap<String, Speaker> speakers = sd.getLocalMap("speakers");
                            BblDataReader.fillSpeakersAndCities(githubData, speakers, cities);

                            EventBus eb = vertx.eventBus();
                            eb.publish("elasticsearch.index", "TODO Index data");

                            // Write to the response and end it
                            writeJsonResponse(routingContext, new JsonObject().put("speakers", speakers.size()).put("cities", cities.size()).encodePrettily());
                        } catch (IOException e) {
                            logger.error("Failed to parse JSON from ", source);
                        }

                    }
                    if (handler.failed()) {
                        logger.error("Failed to read from ", source);
                    }
                });
            } else {
                // Write to the response and end it
                writeJsonResponse(routingContext, new JsonObject().put("message", "Signature Key is incorrect. We skip the update process.").encodePrettily());
            }
        });

        router.route(HttpMethod.POST, "/_stop").handler(routingContext -> {
            // This handler will be called to stop vertx
            // Write to the response and end it
            writeJsonResponse(routingContext, new JsonObject().put("message", "Stopping Vert.x!").encodePrettily());
            vertx.close();
        });

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(port);
        logger.info("HTTP server started on port {}", port);
    }

    @Override
    public void stop() {
        if (client != null) {
            client.close();
        }
        logger.info("HTTP server stopped");
    }

    private static void writeJsonResponse(RoutingContext routingContext, String json) {
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json");
        response.putHeader("content-length", "" + json.length());
        logger.debug("Writing response: {}", json);
        response.write(json).end();
    }
}
