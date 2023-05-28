# Introduction

This folder contains scripts & programs to parse and ingest
public datasets into the graph database.

## Setup

This assumes you have an empty JanusGraph instance with any storage backend and
any index backend. If you are running on a single machine, the recommended approach
is Cassandra + Lucene. You can find the default configuration file under resources
folder. Run `GraphInitializer` to create the graph and set up schema. Then ingest
the data you need. Currently, only `DBLP` data is supported.

### DBLP

We use DBLP-Citation-network V14 snapshot (2023-01-31) downloaded [here](https://www.aminer.org/citation).
Run `DblpParser` to load the vertices and edges. 

## Utilities

### AuthorRefEdgeLoader

You could run `AuthorRefEdgeLoader` Spark
program to add edges between any two pair of authors if one has ever cited the other's works.

### VertexCountRunner

You could run `VertexCountRunner` Spark program to count the number of vertices in the graph.

### SitemapGenerator

You could run `SitemapGenerator` to generate sitemaps for the website. Sitemaps are text
files containing web links. You can then submit the sitemaps to a search engine's crawler
if you want your website to be indexed by search engines.