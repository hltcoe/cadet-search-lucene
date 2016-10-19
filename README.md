[![build status](https://gitlab.hltcoe.jhu.edu/CADET/cadet-search-lucene/badges/master/build.svg)](https://gitlab.hltcoe.jhu.edu/CADET/cadet-search-lucene/commits/master)
---

File-based search for CADET
===============================
This server indexes Concrete files on disk using Lucene to support
rapid demos and testing.  The server implements the SearchService
API defined in concrete-services.

Building
---------------
It requires Java 8 and maven with access to the HLTCOE's maven server
(or manually installing the dependencies).

```bash
./build.sh
```

Running
--------------
The Concrete files must be in a single directory and be named
`[comm id].concrete.`

```bash
./start.sh -d /data/concrete_dir/ -p 8888
```

Indexing
----------------
Currently, this builds a memory-based index using the Lucene standard
analyzer.  The analyzer assumes that the Communications have been
segmented into Sections and Sentences, and uses the Sentence.textSpan
field to extract the sentence text from the Communication.text field.

Integration
----------------
CADET can use the project's built-in file-based implementations of
FetchCommunicationService and StoreCommunicationService to work with
this search server.  Please see the CADET README for instructions
about how to configure these services.
