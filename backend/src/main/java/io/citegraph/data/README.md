# Introduction

This folder contains scripts & programs to parse and ingest
public datasets into the graph database.

## Setup

This assumes you have an empty JanusGraph instance with any storage backend and
any index backend. If you are running on a single machine, the recommended approach
is BerkeleyDB + Lucene. You can find the default configuration file under resources
folder. Run GraphInitializer to create the graph and set up schema.

## DBLP

We use DBLP-Citation-network V14 snapshot (2023-01-31) downloaded [here](https://www.aminer.org/citation).

