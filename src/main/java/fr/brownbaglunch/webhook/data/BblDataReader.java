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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.brownbaglunch.webhook.model.City;
import fr.brownbaglunch.webhook.model.Speaker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;

public class BblDataReader {
    private final static Logger logger = LogManager.getLogger(BblDataReader.class);
    private final static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static void enrichSpeakers(Map<String, Speaker> speakers, Map<String, City> cities) {
        for (Speaker speaker : speakers.values()) {
            enrichSpeaker(speaker, cities);
        }
    }

    private static void enrichSpeaker(Speaker speaker, Map<String, City> cities) {
        for (String cityName : speaker.cities) {
            // Find the city in the cities map
            City city = cities.get(cityName);
            if (city == null) {
                logger.error("City {} does not exist in ", cityName, cities.keySet());
            } else {
                Speaker.GeoLocation geoLocation = new Speaker.GeoLocation();
                geoLocation.name = city.name;
                geoLocation.ville_img = city.ville_img;
                geoLocation.location = new Speaker.Location();
                geoLocation.location.lat = city.lat;
                geoLocation.location.lon = city.lng;
                speaker.locations.add(geoLocation);
            }
        }
    }

    static JsonNode readRawData(String rawData) throws IOException {
        String body = rawData.replace("var data = ", "").replace("};", "}");
        return mapper.readTree(body);
    }

    static JsonNode extractSpeakers(JsonNode root) {
        return root.get("speakers");
    }

    static JsonNode extractCities(JsonNode root) {
        return root.get("cities");
    }

    static Speaker toSpeaker(JsonNode json) throws IOException {
        return mapper.readValue(json.toString(), Speaker.class);
    }

    static City toCity(JsonNode json) throws IOException {
        return mapper.readValue(json.toString(), City.class);
    }

    public static void fillSpeakersAndCities(String rawData, Map<String, Speaker> speakers, Map<String, City> cities) throws IOException {
        fillSpeakersAndCities(readRawData(rawData), speakers, cities);
    }

    static void fillSpeakersAndCities(JsonNode root, Map<String, Speaker> speakers, Map<String, City> cities) throws IOException {
        JsonNode jsonSpeakers = BblDataReader.extractSpeakers(root);

        for (JsonNode jsonSpeaker : jsonSpeakers) {
            Speaker speaker = BblDataReader.toSpeaker(jsonSpeaker);
            String key = speaker.contacts.mail;
            if (key == null) {
                // The speaker did not share any email so we won't be able to contact him.
                // Lets' fallback to the twitter handle
                key = "@" + speaker.contacts.twitter;
            }
            speakers.put(key, speaker);
        }

        JsonNode jsonCities = BblDataReader.extractCities(root);
        jsonCities.fieldNames().forEachRemaining((key) -> {
            try {
                cities.put(key, BblDataReader.toCity(jsonCities.get(key)));
            } catch (IOException e) {
                logger.error("Can not convert {} city to a JavaBean", key);
            }
        });

        BblDataReader.enrichSpeakers(speakers, cities);
    }

    public static String toJson(Speaker speaker) throws Exception {
        return mapper.writeValueAsString(speaker);
    }
}
