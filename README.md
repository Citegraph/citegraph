# Citegraph

## Quick Start

### Ingest Graph Data

See [README](backend/src/main/java/io/citegraph/data/README.md) for steps.

### Start Graph Database

Enter the root directory of JanusGraph distribution, run the following command
(please replace the absolute path accordingly, the config file is available in this
 project):

```
JAVA_OPTIONS="-DJANUSGRAPH_RELATION_DELIMITER=@" ./bin/janusgraph-server.sh console /home/azureuser/gremlin-server-cql.yaml
```

### Start Web App

Let's start by building the frontend artifact first.

```bash
cd frontend
npm run build
# optional - upload the built artifacts to VM
rsync -rave "ssh -i citegraph_key.pem" -r citegraph/frontend/dist azureuser@20.253.223.140:~/
cd ..
```

Next let's package the backend application to a JAR file.

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

In production, you may want to set up a reverse proxy like Nginx to
help you serve the static files, handle SSL and 301 redirect. A complete
example that enables 301 redirect from non-www to www version, and http to https version,
looks like this (put it under `/etc/nginx/conf.d`):

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
        root /home/azureuser/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}
```

Note the above steps assumes you have an SSL certificate installed in `/etc/letsencrypt/live/www.citegraph.io`.
If you don't, you can follow [this tutorial](https://dzone.com/articles/spring-boot-secured-by-lets-encrypt)
to generate and install one in your VM. 

## Roadmap

- Ingest and show number of citations for a given person
- Ingest and show coauthor relationships
- Run pagerank algorithm to attribute scores to authors and papers
- Allow users to filter papers (to exclude those papers wrongly attributed)
- Allow users to set up profiles (like google scholar)
