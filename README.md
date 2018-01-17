# Github Webhook for Brownbaglunch

This project is a Java Application powered by VertX which listens to [Github Push Events](https://developer.github.com/v3/activity/events/types/#pushevent).

Each time something is pushed on [bblfr_data](https://github.com/brownbaglunch/bblfr_data), this application:

* fetch https://github.com/brownbaglunch/bblfr_data/blob/gh-pages/baggers.js
* split `data` in `baggers` and `cities` java beans
* enrich `baggers.cities` with geo location point and image of the associated city
* index `baggers` in elasticsearch. The index name is timestamped.
* move or create alias `bblfr` on the index name
* remove the old indices

Data are available in elasticsearch:

* [baggers](http://localhost:9200/bblfr/doc/_search?pretty)

## Run

To run it, you need to have Maven installed.

```sh
git clone https://github.com/brownbaglunch/java-webhook.git
cd java-webhook
mvn clean install
java -jar target/java-webhook-1.0-SNAPSHOT-fat.jar
```

It should says:

```
23:24:08,233 INFO  [f.b.w.ElasticsearchVerticle] Starting Elasticsearch Client
23:24:08,233 INFO  [f.b.w.WebhookVerticle] Starting HTTP server on port 8080
23:24:09,104 INFO  [f.b.w.WebhookVerticle] HTTP server started on port 8080
23:24:09,918 INFO  [f.b.w.s.ElasticsearchClientManager] Elasticsearch client started and connected to a cluster running version 6.1.1
```

Then just POST to http://localhost:8080/:

```sh
curl -XPOST localhost:8080/
```

And you should see:

```
23:24:12,916 INFO  [f.b.w.WebhookVerticle] Signature has not been verified. Probably Dev Mode.
23:24:12,916 INFO  [f.b.w.WebhookVerticle] Reading data from https://raw.githubusercontent.com/brownbaglunch/bblfr_data/gh-pages/baggers.js
23:24:16,832 INFO  [f.b.w.ElasticsearchVerticle] Let's index 243 speakers and 39 cities.
23:24:19,323 INFO  [f.b.w.ElasticsearchVerticle] Indexed in elasticsearch: OK
```

## Configuration

If you want to push to another cluster, you need to start the application with:

```sh
SOURCE=https://raw.githubusercontent.com/brownbaglunch/bblfr_data/gh-pages/baggers.js \
TARGET=https://username:password@yourcluster.found.io:9243 \
TOKEN=hhdquiHdsuqiudshqhiudhuebefbbcbzbczib \
    java -jar target/java-webhook-1.0-SNAPSHOT-fat.jar
```

`TOKEN` value is the one you defined in [Github Hooks](https://github.com/brownbaglunch/bblfr_data/settings/hooks/).
It can be `null` (default) in development mode.

If you want to change network settings, change `PORT` system variables:

```sh
PORT=9000 java -jar target/java-webhook-1.0-SNAPSHOT-fat.jar
```

By default, it will listen on `0.0.0.0`, port `8080`.

## Endpoints

### Status (GET /)

If you just want to check that the service is running, call:

```sh
curl -XGET localhost:8080
```

You will get back:

```json
{
  "message" : "Vert.x is running!"
}
```

### Deploy (POST /)

If you want to run the process of reading data from Github, then sending that to elasticsearch, you need to call:

```sh
curl -XPOST localhost:8080/
```

In production (when `TOKEN` is set), you need to provide the right SHA1 signature like:

```sh
curl -XPOST localhost:8080/_stop -H "X-Hub-Signature: sha1=22202b35f1482c1a8d3d0c3f6b3c46307792d409" -d '{
  // Your content here. The signature depends on it.
}'
```

You will get back:

```json
{
  "speakers" : 243,
  "cities" : 39
}
```

### Stop the server (POST /_stop)

In production (when `TOKEN` is set), you need to provide the right SHA1 signature like:

```sh
curl -XPOST localhost:8080/_stop -H "X-Bblfr-Key: sha1=22202b35f1482c1a8d3d0c3f6b3c46307792d409"
```

In development mode, just call:

```sh
curl -XPOST localhost:8080/_stop
```

## Deployment on Clevercloud

Connect to your [Clever-cloud console](https://console.clever-cloud.com/).
Create your Java application and define your variables in the Console:

```
SOURCE=https://raw.githubusercontent.com/brownbaglunch/bblfr_data/gh-pages/baggers.js
TARGET=http://bblfr:password@localhost:9200
TOKEN=12345678
PORT=8080
```

Note that `PORT` **must be** `8080`.

Add clever as a git remote repository (change `ID` below):

```ssh
git remote add clever git+ssh://git@push.par.clever-cloud.com/app_ID.git
```

Deploy!

```sh
git push -u clever master
```

Et voilà!

# License

```
This software is licensed under the Apache 2 license, quoted below.

Copyright 2011-2017 David Pilato

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```

