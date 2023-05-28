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

Let's start by building the frontend artifact first. You can skip
this step since the built artifacts are already checked into this
repository. If you made any modifications, please do build it and
copy the artifacts to the spring-boot app project as follows:

```bash
cd frontend
npm run build
cp -r dist ../backend/src/main/resources
cd ..
```

Then we are ready to package the application.

```bash
cd backend
mvn clean package
```

We can then deploy the package to our preferred environment and run
it. For example, we can upload it to Azure Virtual Machine and run the
packaged jar file:

```bash
sudo java -jar app-0.0.1-SNAPSHOT.jar
```

Now the web backend application runs on port 443. Note that this needs
privilege access, so we used `sudo` to launch the app. In production environment,
You may want to use a non-privileged port and a reverse proxy server to forward
traffic from 443 to your port.

Note the above steps assumes you have a SSL certificate installed in `/etc/letsencrypt/live/www.citegraph.io/keystore.p12`.
If you don't, you can follow [this tutorial](https://dzone.com/articles/spring-boot-secured-by-lets-encrypt)
to generate and install one in your VM. Alternatively, if you don't need SSL support,
simply change port 443 to 80 in your `application.properties`, and set `server.ssl.enabled=false`.

If you need 301 redirect from `non-www` prefix to `www` prefix, you could set it up
by yourself using `Apache` or `nginx`, or use a third-party forwarding service like
`redirect.pizza`.

## Roadmap

- Ingest and show number of citations for a given person
- Ingest and show coauthor relationships
- Run pagerank algorithm to attribute scores to authors and papers
- Allow users to filter papers (to exclude those papers wrongly attributed)
- Allow users to set up profiles (like google scholar)
