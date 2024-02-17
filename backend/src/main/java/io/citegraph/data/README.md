# Introduction

This folder contains scripts & programs to parse and ingest
public datasets into the graph database.

## Setup

This assumes you have an empty JanusGraph instance with any storage backend and
any index backend. If you are running on a single machine, the recommended approach
is Cassandra + Lucene. You can find the default configuration file under resources
folder. Run `GraphInitializer` to create the graph and set up schema. Then ingest
the data you need. Currently, only `DBLP` data is supported.

WARNING: Remember to add `-DJANUSGRAPH_RELATION_DELIMITER=@` VM option before running
any program, because `DblpParser` uses UUID which has a collision with JanusGraph's
default relation delimiter.

### DBLP

We use DBLP-Citation-network V14 snapshot (2023-01-31) downloaded [here](https://www.aminer.org/citation).
Run `DblpParser` to load the vertices and edges. 

## Utilities

### AuthorRefEdgeLoader

You could run `AuthorRefEdgeLoader` Spark
program to add edges between any two pair of authors if one has ever cited the other's works.

### VertexEdgeCountRunner

You could run `VertexEdgeCountRunner` Spark program to count the number of vertices and edges
by their types in the graph.

### PageRankRunner

You could run `PageRankRunner` Spark program to calculate the pageranks of papers. It generates
pagerank values and dumps them into a specified folder in GraphSON format. You could then use
`GraphSONVertexPropertyLoader` to read those files and load them into graph.

### CoworkerEdgeLoader

You could run `CoworkerEdgeLoader` to add `collaborates` edges between any two pair of authors
if they ever coauthored the same paper.

### CommunityDetectionRunner

You could run `CommunityDetectionRunner` Spark program to calculate the research cluster of authors. It finds
the cluster of each author and dumps them into a specified folder in GraphSON format. You could then use
`GraphSONVertexPropertyLoader` to read those files and load them into graph. `CoworkerEdgeLoader` is the
prerequisite since it leverages the `collaborates` edges.

### VertexPropertyEnricher

You could run `VertexPropertyEnricher` to pre-compute some common traversals
and store the results as vertex properties. `AuthorRefEdgeLoader` is the prerequisite
of this program since it leverages the edges written by `AuthorRefEdgeLoader`.
`PageRankRunner` is also a prerequisite since it leverages the pageranks of paper
nodes to calculate pageranks of author nodes. See JavaDoc to see more details.

### SitemapGenerator

You could run `SitemapGenerator` to generate sitemaps for the website. Sitemaps are text
files containing web links. You can then submit the sitemaps to a search engine's crawler
if you want your website to be indexed by search engines.