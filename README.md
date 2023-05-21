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

```java
java -jar app-0.0.1-SNAPSHOT.jar
```

Now the web backend application runs on port 8080. You may need a
reverse proxy server to expose your website on port 80.
