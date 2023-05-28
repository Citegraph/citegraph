# Introduction

This folder contains scripts & programs to parse and ingest
public datasets into the graph database.

## Setup

This assumes you have an empty JanusGraph instance with any storage backend and
any index backend. If you are running on a single machine, the recommended approach
is Cassandra + Lucene. You can find the default configuration file under resources
folder. Run `GraphInitializer` to create the graph and set up schema.

## DBLP

We use DBLP-Citation-network V14 snapshot (2023-01-31) downloaded [here](https://www.aminer.org/citation).
Run `DblpParser` to load the vertices and edges. Optionally, you could also run `AuthorRefEdgeLoader` Spark
program to add edges between any two pair of authors if one has ever cited the other's works.