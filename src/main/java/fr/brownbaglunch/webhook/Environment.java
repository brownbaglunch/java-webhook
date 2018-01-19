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

public class Environment {
    public final static String DEFAULT_ROOT = "https://raw.githubusercontent.com/brownbaglunch/bblfr_data";
    public final static String DEFAULT_BRANCH = "gh-pages";
    public final static String DEFAULT_SOURCE = "/baggers.js";
    public final static String DEFAULT_TOKEN = null;
    public final static int DEFAULT_PORT = 8080;
    public final static String DEFAULT_TARGET="http://localhost:9200";

    public final static String root = System.getenv().getOrDefault("ROOT", System.getProperties().getProperty("ROOT", DEFAULT_ROOT));
    public final static String branch = System.getenv().getOrDefault("BRANCH", System.getProperties().getProperty("BRANCH", DEFAULT_BRANCH));
    public final static String source = System.getenv().getOrDefault("SOURCE", System.getProperties().getProperty("SOURCE", DEFAULT_SOURCE));
    public final static String token = System.getenv().getOrDefault("TOKEN", System.getProperties().getProperty("TOKEN", DEFAULT_TOKEN));
    public final static Integer port = Integer.parseInt(System.getenv().getOrDefault("PORT", System.getProperties().getProperty("PORT", "" + DEFAULT_PORT)));
    public final static String target = System.getenv().getOrDefault("TARGET", System.getProperties().getProperty("TARGET", DEFAULT_TARGET));
}
