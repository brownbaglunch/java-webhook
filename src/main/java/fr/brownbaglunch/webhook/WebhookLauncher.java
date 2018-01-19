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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class WebhookLauncher {

    private static final Logger logger = LogManager.getLogger(WebhookLauncher.class);

    public static void main(String[] args) throws IOException {
        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new ElasticsearchVerticle(Environment.target));
        vertx.deployVerticle(new WebhookVerticle(Environment.port, Environment.token, Environment.root, Environment.branch, Environment.source));

        vertx.deployVerticle(new ElasticsearchVerticle(Environment.target), id -> {
            logger.debug("Elasticsearch Verticle has been deployed {}", id);
        });

        vertx.deployVerticle(
                new WebhookVerticle(Environment.port, Environment.token, Environment.root, Environment.branch, Environment.source), id -> {
            logger.debug("Webhook Verticle has been deployed {}", id);
        });
    }
}
