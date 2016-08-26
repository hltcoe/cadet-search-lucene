[![build status](https://gitlab.hltcoe.jhu.edu/CADET/cadet-search-lucene/badges/master/build.svg)](https://gitlab.hltcoe.jhu.edu/CADET/cadet-search-lucene/commits/master)
---

File-based search for CADET
===============================
This server indexes concrete files on disk using lucene to support
rapid demos and testing.

Building
---------------
It requires Java 8 and maven with access to the HLTCOE's maven server
(or manually installing the dependencies).

```bash
./build.sh
```

Running
--------------
The concrete files must be in a single directory and be named [comm id].concrete.
Support for tarballs will be added in the future.

```bash
./start.sh -d /data/concrete_dir/ -p 8888
```

Indexing
----------------
Currently, this builds a memory-based index using the standard analyzer.
Other indexes and analyzers will be supported in the future.

Integration
----------------
CADET must use the file-based retriever and sender to work with this
search server.
