[![build status](https://gitlab.hltcoe.jhu.edu/CADET/cadet-search-lucene/badges/master/build.svg)](https://gitlab.hltcoe.jhu.edu/CADET/cadet-search-lucene/commits/master)
---

File-based search for CADET
===============================
This server indexes Concrete files from a Fetch service using Lucene to support
rapid demos and testing.  The server implements the SearchService
API defined in concrete-services.

Building
---------------
It requires Java 8 and maven with access to the HLTCOE's maven server
(or manually installing the dependencies).

```bash
mvn clean package
```

Running
--------------
You must have a running Fetch service passing its parameters as fh and fp options.

```bash
./start.sh -d /index_dir/ -p 8888 -fh localhost -fp 9091
```

