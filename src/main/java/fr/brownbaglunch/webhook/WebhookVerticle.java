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
import fr.brownbaglunch.webhook.data.GithubPrReader;
import fr.brownbaglunch.webhook.model.City;
import fr.brownbaglunch.webhook.model.GithubPrEvent;
import fr.brownbaglunch.webhook.model.Speaker;
import fr.brownbaglunch.webhook.util.KeyChecker;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
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
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * You may want to set the TOKEN system property that you set in GitHub
 */
public class WebhookVerticle extends AbstractVerticle {

    private final static Logger logger = LogManager.getLogger(WebhookVerticle.class);

    private final int port;
    private final String token;
    private final String root;
    private final String branch;
    private final String source;


    private WebClient client;

    WebhookVerticle(int port, String token, String root, String branch, String source) {
        this.port = port;
        this.token = token;
        this.root = root;
        this.branch = branch;
        this.source = source;
        logger.info("Starting HTTP server on port {}", this.port);
    }

    @Override
    public void start(Future<Void> futureVertx) {
        Router router = Router.router(vertx);

        // Create the WebClient to connect to GitHub
        client = WebClient.create(vertx);

        router.route(HttpMethod.GET, "/").handler(routingContext -> {
            // Write to the response and end it
            writeJsonResponse(routingContext, new JsonObject().put("message", "Vert.x is running!").encodePrettily());
        });
        router.route(HttpMethod.POST, "/").handler(BodyHandler.create()).handler(routingContext -> {
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
                String url = root + "/" + branch + source;
                logger.info("Reading data from {}", url);
                HttpRequest<Buffer> request = client.getAbs(url);
                request.send(handler -> {
                    if (handler.succeeded()) {
                        String githubData = handler.result().bodyAsString();
                        logger.trace(githubData);
                        try {
                            SharedData sd = vertx.sharedData();

                            LocalMap<String, City> cities = sd.getLocalMap("cities");
                            LocalMap<String, Speaker> speakers = sd.getLocalMap("speakers");
                            boolean failure = BblDataReader.fillSpeakersAndCities(githubData, speakers, cities);

                            EventBus eb = vertx.eventBus();
                            eb.publish("elasticsearch.index", "TODO Index data");

                            // Write to the response and end it
                            writeJsonResponse(routingContext,
                                    new JsonObject()
                                            .put("speakers", speakers.size())
                                            .put("cities", cities.size())
                                            .put("with_failures", failure)
                                            .encodePrettily());
                        } catch (IOException e) {
                            logger.error("Failed to parse JSON from ", url);
                            writeJsonResponse(routingContext,
                                    new JsonObject().put("message", "Failed to parse the JSON document.").encodePrettily());
                        }

                    }
                    if (handler.failed()) {
                        logger.error("Failed to read from ", url);
                        writeJsonResponse(routingContext,
                                new JsonObject().put("message", "Failed to read the content from " + url + ".").encodePrettily());
                    }
                });
            } else {
                // Write to the response and end it
                writeJsonResponse(routingContext, new JsonObject().put("message", "Signature Key is incorrect. We skip the update process.").encodePrettily());
            }
        });
        router.route(HttpMethod.POST, "/_validate").handler(BodyHandler.create()).handler(routingContext -> {
            logger.info("POST /_validate has been called.");
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
                // Convert the event to a PR
                try {
                    GithubPrEvent pr = GithubPrReader.toGithubEvent(body);

                    // We just check if the pr is still active
                    if ("opened".equals(pr.action) || "synchronize".equals(pr.action)) {
                        String url = pr.getUrl();
                        logger.info("Reading data from {}", url);
                        HttpRequest<Buffer> request = client.getAbs(url);
                        request.send(handler -> {
                            if (handler.succeeded()) {
                                String githubData = handler.result().bodyAsString();
                                logger.trace(githubData);
                                try {
                                    SharedData sd = vertx.sharedData();

                                    LocalMap<String, City> cities = sd.getLocalMap("cities");
                                    LocalMap<String, Speaker> speakers = sd.getLocalMap("speakers");
                                    boolean failure = BblDataReader.fillSpeakersAndCities(githubData, speakers, cities);

                                    // Write to the response and end it
                                    String json = new JsonObject()
                                            .put("message", "PR Checked. Everything seems ok.")
                                            .put("commit", pr.pull_request.head.sha)
                                            .put("speakers", speakers.size())
                                            .put("cities", cities.size())
                                            .put("with_failures", failure)
                                            .encodePrettily();
                                    writeJsonResponse(routingContext, json);
                                    logger.info("{}", json);
                                } catch (IOException e) {
                                    logger.error("Failed to parse JSON from ", url);
                                    writeJsonResponse(routingContext,
                                            new JsonObject().put("message", "Failed to parse the JSON document.").encodePrettily());
                                    logger.warn("Check the PR. Something seems wrong.", e);
                                }
                            }
                            if (handler.failed()) {
                                logger.error("Failed to read from ", url);
                                writeJsonResponse(routingContext,
                                        new JsonObject().put("message", "Failed to read the content from " + url + ".").encodePrettily());
                            }
                        });
                    } else {
                        writeJsonResponse(routingContext,
                                new JsonObject().put("message", "Not supported action [" + pr.action + "]. Skipped").encodePrettily());
                    }

                } catch (IOException e) {
                    logger.error("Failed to parse the PR JSON document", e);
                    writeJsonResponse(routingContext,
                            new JsonObject().put("message", "Failed to parse the PR JSON document.").encodePrettily());
                }

                // If possible but that is going to require to become an application
                // https://developer.github.com/v3/pulls/reviews/#submit-a-pull-request-review
                // POST /repos/:owner/:repo/pulls/:number/reviews/:id/events
            } else {
                // Write to the response and end it
                writeJsonResponse(routingContext, new JsonObject().put("message", "Signature Key is incorrect. We skip the update process.").encodePrettily());
            }
        });

        router.route(HttpMethod.POST, "/_stop").handler(routingContext -> {
            // This handler will be called to stop vertx

            // We check that the token is correct. In production, people need to send the right "X-Bblfr-Key: XXXX" value
            String signature = routingContext.request().getHeader("X-Bblfr-Key");
            boolean keyIsChecked;
            if (signature != null || token != null) {
                keyIsChecked = KeyChecker.testGithubToken("", signature, token);
            } else {
                logger.warn("Signature has not been verified. Probably Dev Mode.");
                keyIsChecked = true;
            }

            if (keyIsChecked) {
                // Write to the response and end it
                writeJsonResponse(routingContext, new JsonObject().put("message", "Stopping Vert.x!").encodePrettily());
                vertx.close();
            } else {
                // Write to the response and end it
                writeJsonResponse(routingContext, new JsonObject().put("message", "X-Bblfr-Key is incorrect.").encodePrettily());
            }
        });

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(port, ar -> {
                    if (ar.succeeded()) {
                        futureVertx.complete();
                    }
                    if (ar.failed()) {
                        futureVertx.fail("Can not start");
                    }
                });
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
