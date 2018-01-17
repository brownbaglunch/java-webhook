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

package fr.brownbaglunch.webhook.data;

import fr.brownbaglunch.webhook.Environment;
import fr.brownbaglunch.webhook.model.GithubPrEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GithubPrReaderTest {

    private final Logger logger = LogManager.getLogger(GithubPrReaderTest.class);

    @Test
    void testReadCreatePr() throws IOException, URISyntaxException {
        URL url = GithubPrReaderTest.class.getResource("/fr/brownbaglunch/webhook/pr-create.json");
        Path path = Paths.get(url.toURI());
        String s = new String(Files.readAllBytes(path), "UTF-8");
        GithubPrEvent pr = GithubPrReader.toGithubEvent(s);

        assertEquals("opened", pr.action);
        assertEquals(1, pr.number);
        assertEquals("open", pr.pull_request.state);
        assertEquals("dadoonet-patch-1", pr.pull_request.head.ref);
        assertEquals("131722c9e4916060f0ebcd2f8b1685bc9696abae", pr.pull_request.head.sha);

        // Check the Link to the bagger.js file...
        assertEquals("https://raw.githubusercontent.com/brownbaglunch/bblfr_data/dadoonet-patch-1/baggers.js",
                pr.getUrl(Environment.DEFAULT_ROOT, Environment.DEFAULT_SOURCE));
    }

    @Test
    void testReadUpdatePr() throws IOException, URISyntaxException {
        URL url = GithubPrReaderTest.class.getResource("/fr/brownbaglunch/webhook/pr-update.json");
        Path path = Paths.get(url.toURI());
        String s = new String(Files.readAllBytes(path), "UTF-8");
        GithubPrEvent pr = GithubPrReader.toGithubEvent(s);

        assertEquals("synchronize", pr.action);
        assertEquals(1, pr.number);
        assertEquals("open", pr.pull_request.state);
        assertEquals("dadoonet-patch-1", pr.pull_request.head.ref);
        assertEquals("369a21bf1a3c34262a5db0aee40b54f70c3e6d97", pr.pull_request.head.sha);

        // Check the Link to the bagger.js file...
        assertEquals("https://raw.githubusercontent.com/brownbaglunch/bblfr_data/dadoonet-patch-1/baggers.js",
                pr.getUrl(Environment.DEFAULT_ROOT, Environment.DEFAULT_SOURCE));
    }
}
