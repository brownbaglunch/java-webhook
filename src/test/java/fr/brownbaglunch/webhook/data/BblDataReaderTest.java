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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import fr.brownbaglunch.webhook.model.City;
import fr.brownbaglunch.webhook.model.Speaker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BblDataReaderTest {

    private static JsonNode root;
    private final Logger logger = LogManager.getLogger(BblDataReaderTest.class);

    @BeforeAll
    static void readRawData() throws URISyntaxException, IOException {
        URL url = BblDataReaderTest.class.getResource("/fr/brownbaglunch/webhook/speakers.js");
        Path path = Paths.get(url.toURI());
        String s = new String(Files.readAllBytes(path), "UTF-8");
        root = BblDataReader.readRawData(s);
    }

    @Test
    void testReadRaw() {
        assertEquals(2, root.size(), () -> {
            logger.error("Got this Raw JSON: {}", root.toString());
            return null;
        });
    }

    @Test
    void testReadSpeakers() {
        JsonNode speakers = BblDataReader.extractSpeakers(root);
        assertEquals(2, speakers.size(), () -> {
            logger.error("Got this: {}", speakers.toString());
            return null;
        });
    }

    @Test
    void testReadCities() {
        JsonNode cities = BblDataReader.extractCities(root);
        assertEquals(38, cities.size(), () -> {
            logger.error("Got this: {}", cities.toString());
            return null;
        });
    }

    @Test
    void testReadOneSpeaker() throws IOException {
        JsonNode speakers = BblDataReader.extractSpeakers(root);
        JsonNode jsonSpeaker = speakers.get(0);
        Speaker speaker = BblDataReader.toSpeaker(jsonSpeaker);
        checkSpeaker1(speaker);
    }

    @Test
    void testReadOneCity() throws IOException {
        JsonNode cities = BblDataReader.extractCities(root);
        String key = cities.fieldNames().next();
        JsonNode jsonCity = cities.get(key);
        City city = BblDataReader.toCity(jsonCity);
        assertEquals("Aix-en-Provence", city.name);
        assertEquals("img/villes/BBL_Aix-en-provence.png", city.ville_img);
        assertEquals(43.529742, city.lat);
        assertEquals(5.447427, city.lng);
    }

    @Test
    void testConvertAllAsBeans() throws IOException {
        Map<String, Speaker> speakers = new HashMap<>();
        Map<String, City> cities = new HashMap<>();

        BblDataReader.fillSpeakersAndCities(root, speakers, cities);
        assertEquals(38, cities.size());
        assertEquals(2, speakers.size());

        Speaker speaker1 = speakers.get("email1@domain.com");
        checkSpeaker1(speaker1);
        assertEquals(1, speaker1.locations.size());
        assertEquals("Paris", speaker1.locations.get(0).name);
        assertNotNull(speaker1.locations.get(0).ville_img);
        assertNotEquals(0.0, speaker1.locations.get(0).location.lat);
        assertNotEquals(0.0, speaker1.locations.get(0).location.lon);
    }

    private void checkSpeaker1(Speaker speaker) {
        assertEquals("François S", speaker.name);
        assertEquals("Consultant Java/Scala/&#955; | Manager technique ", speaker.bio);
        assertEquals("2013-02-09", speaker.since);
        assertEquals("https://url.to/picture.png", speaker.picture);
        assertEquals("Paris, remote", speaker.location);

        // Websites
        assertEquals(1, speaker.websites.size());
        assertEquals("Web", speaker.websites.get(0).name);
        assertEquals("http://awesome.blog.post/", speaker.websites.get(0).url);

        // Sessions
        assertEquals(2, speaker.sessions.size());
        assertEquals("Session 1", speaker.sessions.get(0).title);
        assertNotNull(speaker.sessions.get(0).content);
        assertNotEquals(0, speaker.sessions.get(0).content.length());
        assertEquals(2, speaker.sessions.get(0).lang.size());
        assertEquals("fr", speaker.sessions.get(0).lang.get(0));
        assertEquals("en", speaker.sessions.get(0).lang.get(1));
        assertEquals(2, speaker.sessions.get(0).tags.size());
        assertEquals("tag 1", speaker.sessions.get(0).tags.get(0));
        assertEquals("tag 2", speaker.sessions.get(0).tags.get(1));

        assertEquals("Session 2", speaker.sessions.get(1).title);
        assertNotNull(speaker.sessions.get(1).content);
        assertNotEquals(0, speaker.sessions.get(1).content.length());
        assertEquals(1, speaker.sessions.get(1).lang.size());
        assertEquals("fr", speaker.sessions.get(1).lang.get(0));
        assertEquals(1, speaker.sessions.get(1).tags.size());
        assertEquals("tag 1", speaker.sessions.get(1).tags.get(0));

        // Cities
        assertEquals(1, speaker.cities.size());
        assertEquals("Paris", speaker.cities.get(0));

        // Contacts
        assertNotNull(speaker.contacts);
        assertEquals("twittos1", speaker.contacts.twitter);
        assertEquals("email1@domain.com", speaker.contacts.mail);
    }

    @Test
    void testReadBadJsonWithTypo() throws IOException, URISyntaxException {
        URL url = BblDataReaderTest.class.getResource("/fr/brownbaglunch/webhook/pr-baggers-ko-typo.js");
        Path path = Paths.get(url.toURI());
        String s = new String(Files.readAllBytes(path), "UTF-8");
        assertThrows(JsonParseException.class, () -> BblDataReader.fillSpeakersAndCities(s, new HashMap<>(), new HashMap<>()));
    }

    @Test
    void testReadBadJsonWithMissingCity() throws IOException, URISyntaxException {
        URL url = BblDataReaderTest.class.getResource("/fr/brownbaglunch/webhook/pr-baggers-ko-missing-city.js");
        Path path = Paths.get(url.toURI());
        String s = new String(Files.readAllBytes(path), "UTF-8");
        assertTrue(BblDataReader.fillSpeakersAndCities(s, new HashMap<>(), new HashMap<>()));
    }

    @Test
    void testReadCorrectJson() throws IOException, URISyntaxException {
        URL url = BblDataReaderTest.class.getResource("/fr/brownbaglunch/webhook/speakers.js");
        Path path = Paths.get(url.toURI());
        String s = new String(Files.readAllBytes(path), "UTF-8");
        assertFalse(BblDataReader.fillSpeakersAndCities(s, new HashMap<>(), new HashMap<>()));
    }
}
