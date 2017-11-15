File-based search for CADET
===============================
This server indexes Concrete files from a Fetch service using Lucene to support rapid demos and testing.
The server implements the SearchService thrift API defined in Concrete.

Building
---------------
It requires Java 8 and maven.

```bash
mvn clean package
```

Running
--------------
Search has two modes: building an index and running the search service.
The index is built using the -b flag.
It can be built from a fetch service or from the file system (called direct).
The file system indexer can load a zip file or a directory of communications.

An example of building an index from a fetch service:
```bash
./start.sh -d /index_dir/ --fh localhost --fp 9091 -b
```

An example of building an index from a zip file:
```bash
./start.sh -d /index_dir/ --direct /home/me/data.zip -b
```

The index is written to a directory which is specified with the -d flag.

Running the build option multiple times will add the same communications to the index multiple times.

To build the index and run the search service on port 8888, use the -r and -b flags:
```
./start.sh -d /index_dir/ -p 8888 --fh localhost --fp 9091 -b -r
```

For monolingual corpora, set the language code with the -l option. Use the language's 3 letter code.

To use Lucene's tokenization rather than that found in the concrete files, pass the --lt flag.

More details can be found by passing the -h flag.

Docker
-------------
To build the image, run

```bash
docker build -t hltcoe/cadet-search-lucene .
```

To get help running the image:
```
docker run hltcoe/cadet-search-lucene
```

To run the search service on port 9092 with a fetch service at port 9091:

```bash
docker run -d hltcoe/cadet-search-lucene -p 9092 -d /tmp --fh localhost --fp 9091
```
Note: a firewall could prevent the search service running in the container reach
a fetch service running on the host as is required in the example above.

