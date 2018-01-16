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

package fr.brownbaglunch.webhook.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.shareddata.Shareable;

import java.util.ArrayList;
import java.util.List;

public class Speaker implements Shareable {

    // Available in existing model
    public String since;
    public String name;
    public String bio;
    public String picture;
    public String location;
    public List<Website> websites;
    public List<Session> sessions;
    public List<String> cities;
    public Contacts contacts;

    // Used to transform to Elasticsearch model
    public List<GeoLocation> locations = new ArrayList<>();

    public static class Website {
        public String name;
        public String url;
    }

    public static class Session {
        public String title;
        @JsonProperty("abstract")
        public String content;
        public List<String> tags;
        public List<String> lang;
    }

    public static class Contacts {
        public String twitter;
        public String mail;
    }

    public static class GeoLocation {
        public String name;
        public String ville_img;
        public Location location;
    }

    public static class Location {
        public double lat;
        public double lon;
    }
}
