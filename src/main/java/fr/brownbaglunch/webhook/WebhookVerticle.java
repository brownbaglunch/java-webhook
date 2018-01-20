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
import io.vertx.core.DeploymentOptions;
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
 * This is the main Verticle. It controls the whole application.
 */
public class WebhookVerticle extends AbstractVerticle {

    private final static Logger logger = LogManager.getLogger(WebhookVerticle.class);

    private final String elasticsearchUrl;
    private final int webserverPort;
    private final String webserverToken;
    private final String webserverRoot;
    private final String webserverBranch;
    private final String webserverSource;

    public WebhookVerticle(String elasticsearchUrl, int webserverPort, String webserverToken, String webserverRoot, String webserverBranch, String webserverSource) {
        this.elasticsearchUrl = elasticsearchUrl;
        this.webserverPort = webserverPort;
        this.webserverToken = webserverToken;
        this.webserverRoot = webserverRoot;
        this.webserverBranch = webserverBranch;
        this.webserverSource = webserverSource;
    }

    @Override
    public void start(Future<Void> startFuture) {
        logger.debug("Starting Webhook Verticle");

        Future<String> elasticsearchVerticleDeployment = Future.future();
        vertx.deployVerticle(new ElasticsearchVerticle(elasticsearchUrl), elasticsearchVerticleDeployment.completer());

        elasticsearchVerticleDeployment.compose(id -> {
            Future<String> webServerVerticleDeployment = Future.future();
            vertx.deployVerticle(
                    new WebServerVerticle(webserverPort, webserverToken, webserverRoot, webserverBranch, webserverSource),
                    webServerVerticleDeployment.completer());

            return webServerVerticleDeployment;
        }).setHandler(ar -> {
            if (ar.succeeded()) {
                logger.debug("Webhook Verticle started");
                startFuture.complete();
            } else {
                logger.error("Failed starting Webhook Verticle", ar.cause());
                startFuture.fail(ar.cause());
            }
        });
    }
}
