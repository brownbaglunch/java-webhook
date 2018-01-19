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

package fr.brownbaglunch.webhook.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.io.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ElasticsearchClientManager {
    private final Logger logger = LogManager.getLogger(ElasticsearchClientManager.class);

    private ElasticsearchClient client = null;

    public ElasticsearchClientManager(String url) throws IOException {
        // Create an elasticsearch client
        client = new ElasticsearchClient(url);
    }

    public ElasticsearchClient client() {
        return client;
    }

    public String checkRunning() throws IOException {
        String version = client.info().getVersion().toString();
        logger.debug("Elasticsearch is running with version {}", version);
        return version;
    }

    /**
     * This is starting elasticsearch. Checking that it's up and running, creating index template...
     */
    public void start() throws Exception {
        String version = checkRunning();
        logger.info("Elasticsearch client started and connected to a cluster running version {}", version);

        // Overwrite index template
        InputStream in = getClass().getResourceAsStream("/fr/brownbaglunch/webhook/template.json");
        String template = Streams.copyToString(new InputStreamReader(in));
        logger.debug("Index template: {}", template);
        client.createIndexTemplate("bblfr", template);
    }

    public void close() {
        logger.debug("Closing Elasticsearch client manager");
        if (client != null) {
            try {
                client.shutdown();
            } catch (IOException e) {
                logger.warn("Can not close elasticsearch client", e);
            }
        }
    }
}
