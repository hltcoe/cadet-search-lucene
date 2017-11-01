File-based search for CADET
===============================
This server indexes Concrete files from a Fetch service using Lucene to support
rapid demos and testing.  The server implements the SearchService
API defined in concrete-services.

Building
---------------
It requires Java 8 and maven.

```bash
mvn clean package
```

Running
--------------
The index is written to a directory which is specified with the -d flag.
You must have a running Fetch service and pass its parameters as fh and fp options.

```bash
./start.sh -d /index_dir/ -p 8888 --fh localhost --fp 9091 -b -r
```

To build the index, set the -b flag. To run the search service, set the -r flag.
Running the build option multiple times will add the same communications to the index multiple times.

For monolingual corpora, set the language code with the -l option. Use the language's 3 letter code.

More details can be found by passing the -h flag.

Docker
-------------
To build the image, run

```bash
docker build -t hltcoe/cadet-search-lucene .
```

To run the search app on port 9092 with a fetch service at port 9091:

```bash
docker run -d hltcoe/cadet-search-lucene -p 9092 -d /tmp --fh localhost --fp 9091
```
