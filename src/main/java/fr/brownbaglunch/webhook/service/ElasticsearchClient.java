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


import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;
import java.util.Collections;

import static org.elasticsearch.common.Strings.isNullOrEmpty;

/**
 * Simple Elasticsearch client over HTTP or HTTPS.
 * Only needed methods are exposed.
 */
public class ElasticsearchClient extends RestHighLevelClient {

    private static final Logger logger = LogManager.getLogger(ElasticsearchClient.class);

    ElasticsearchClient(String url) throws IOException {
        super(buildRestClient(url));
    }

    /**
     * Shutdown the internal REST Low Level client
     * @throws IOException In case of error
     */
    void shutdown() throws IOException {
        logger.debug("Closing REST client");
        close();
    }

    /**
     * Create an index template
     * @param template index template name
     * @param json template
     * @throws IOException In case of error
     */
    void createIndexTemplate(String template, String json) throws IOException {
        logger.debug("create index template [{}]", template);
        logger.trace("index template: [{}]", json);
        StringEntity entity = null;
        if (!isNullOrEmpty(json)) {
            entity = new StringEntity(json, ContentType.APPLICATION_JSON);
        }
        getLowLevelClient().performRequest("PUT", "/_template/" + template, Collections.emptyMap(), entity);
    }

    /**
     * Delete an index (removes all data)
     * @param index index name
     * @throws IOException In case of error
     */
    public void deleteIndex(String index) throws IOException {
        logger.debug("delete index [{}]", index);

        try {
            getLowLevelClient().performRequest("DELETE", "/" + index);
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                logger.debug("index [{}] does not exist", index);
                return;
            }
            throw e;
        }
    }

    public void switchAlias(String oldNames, String newName, String aliasName) throws IOException {
        logger.debug("switch alias [{}] to [{}]", aliasName, newName);
        String json = "{\n" +
                "    \"actions\" : [\n" +
                "        { \"remove\" : { \"index\" : \"" + oldNames + "\", \"alias\" : \"" + aliasName + "\" } },\n" +
                "        { \"add\" : { \"index\" : \"" + newName + "\", \"alias\" : \"" + aliasName + "\" } }\n" +
                "    ]\n" +
                "}\n";
        StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
        getLowLevelClient().performRequest("POST", "/_aliases", Collections.emptyMap(), entity);
    }

    static RestClientBuilder buildRestClient(String url) {
        String user = null;
        String password = null;
        final int schemeIdx = url.indexOf("://");
        final int userPasswordIdx = url.indexOf("@");
        String finalUrl;
        if (userPasswordIdx > 0) {
            String userPassword = url.substring(schemeIdx + 3, userPasswordIdx);
            final int userIdx = userPassword.indexOf(":");
            if (userIdx > 0) {
                user = userPassword.substring(0, userIdx);
                password = userPassword.substring(userIdx + 1);
            } else {
                user = userPassword;
            }
            finalUrl = url.substring(0, schemeIdx + 3) + url.substring(userPasswordIdx + 1);
        } else {
            finalUrl = url;
        }

        RestClientBuilder builder = RestClient.builder(HttpHost.create(finalUrl));
        if (user != null) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }
        return builder;
    }
}
