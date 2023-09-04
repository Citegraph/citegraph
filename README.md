[![CiteGraph logo](citegraph.svg)](https://www.citegraph.io)

## Introduction

CiteGraph is an open-source online visualizer of citation networks. It was initially created as a demo
of [JanusGraph](https://janusgraph.org), a distributed graph database. The tech stack includes JanusGraph,
Spring Boot, Nginx, React, Remix (Server Side Rendering), Lucene, Cassandra, and Spark.


<img width="1818" alt="Screenshot 2023-08-27 at 1 05 05 AM" src="https://github.com/Citegraph/citegraph/assets/25746010/d915b6c2-b9e7-4ce8-8072-b833e0e79929">
<img width="1811" alt="Screenshot 2023-08-27 at 1 05 44 AM" src="https://github.com/Citegraph/citegraph/assets/25746010/793e9246-da54-4c8f-8099-fda1ef34a452">
<img width="1843" alt="Screenshot 2023-08-27 at 1 06 13 AM" src="https://github.com/Citegraph/citegraph/assets/25746010/99a91339-b90d-4e30-8081-06b3816ccd7f">


## Quick Start

Of course, only if you want to launch CiteGraph on your own. Otherwise, just visit [CiteGraph](https://www.citegraph.io) and have fun!

### Ingest Graph Data

See [README](backend/src/main/java/io/citegraph/data/README.md) for steps.

### Start Graph Database

Enter the root directory of JanusGraph distribution, run the following command
(please replace the absolute path accordingly, the config file is available [here](https://github.com/Citegraph/citegraph/blob/main/backend/src/main/resources/gremlin-server-cql.yaml):

```
JAVA_OPTIONS="-DJANUSGRAPH_RELATION_DELIMITER=@" ./bin/janusgraph-server.sh console /home/azureuser/gremlin-server-cql.yaml
```

### Start Backend

Let's package the backend application to a JAR file first.

```bash
cd backend
mvn clean package
# optional - upload the jar file to VM
scp -i citegraph_key.pem citegraph/backend/target/app-0.0.1-SNAPSHOT.jar azureuser@20.253.223.140:~/
```

We can then deploy the package to our preferred environment and run
it. For example, we can upload it to Azure Virtual Machine and run the
packaged jar file:

```bash
java -jar app-0.0.1-SNAPSHOT.jar
```

Now the web backend application runs on port 8080.

### Start Web Server

We use Remix for server-side rendering. Let's launch the web server that runs
on 3000 port (tested using node.js v16):

```bash
cd frontend
npm install
npm run start
```

In production, you may want to set up a reverse proxy like Nginx to
help you serve the static files, handle SSL and 301 redirect. A complete
example that enables 301 redirect from non-www to www version, and http to https version,
looks like this (put it in a file under `/etc/nginx/conf.d` directory):

```nginx
server {
    listen 80;
    server_name citegraph.io;

    location / {
        return 301 https://www.citegraph.io$request_uri;
    }
}

server {
    listen 80;
    server_name www.citegraph.io;

    location / {
        return 301 https://www.citegraph.io$request_uri;
    }
}

server {
    listen 443 ssl;
    server_name citegraph.io;

    ssl_certificate /etc/letsencrypt/live/www.citegraph.io/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/www.citegraph.io/privkey.pem;

    location / {
        return 301 https://www.citegraph.io$request_uri;
    }
}

server {
    listen 443 ssl;
    server_name www.citegraph.io;

    ssl_certificate /etc/letsencrypt/live/www.citegraph.io/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/www.citegraph.io/privkey.pem;

    location /apis {
        proxy_pass http://localhost:8080;
    }

    location / {
        proxy_pass http://localhost:3000;
    }
}
```

Note the above steps assumes you have an SSL certificate installed in `/etc/letsencrypt/live/www.citegraph.io`.
If you don't, you can follow [this tutorial](https://dzone.com/articles/spring-boot-secured-by-lets-encrypt)
to generate and install one in your VM. 
