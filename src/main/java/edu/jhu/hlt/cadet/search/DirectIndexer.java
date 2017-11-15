/*
 * Copyright 2012-2017 Johns Hopkins University HLTCOE. All rights reserved.
 * This software is released under the 2-clause BSD license.
 * See LICENSE in the project root directory.
 */
package edu.jhu.hlt.cadet.search;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.lucene.LuceneCommunicationIndexer;
import edu.jhu.hlt.concrete.lucene.NaiveConcreteLuceneIndexer;
import edu.jhu.hlt.concrete.lucene.pretokenized.TokenizedCommunicationIndexer;
import edu.jhu.hlt.concrete.serialization.CommunicationSerializer;
import edu.jhu.hlt.concrete.serialization.CompactCommunicationSerializer;
import edu.jhu.hlt.concrete.util.ConcreteException;
import edu.jhu.hlt.concrete.zip.ConcreteZipIO;

public class DirectIndexer implements Indexer {
    private static Logger logger = LoggerFactory.getLogger(DirectIndexer.class);

    private final String path;

    public DirectIndexer(String path) {
        this.path = path;
    }

    @Override
    public void index(Config config) throws IOException, TException {
        LuceneCommunicationIndexer indexer = null;
        if (config.useLuceneTokenizer) {
            indexer = new NaiveConcreteLuceneIndexer(config.indexDir);
        } else {
            indexer = new TokenizedCommunicationIndexer(config.indexDir);
        }

        if (path.endsWith(".zip")) {
            processZipFile(indexer, path, config.batchSize);
        } else {
            processDirectory(indexer, path, config.batchSize);
        }

        indexer.close();
    }

    private void processZipFile(LuceneCommunicationIndexer indexer, String path, int batchSize) throws IOException {
        int counter = 0;
        Iterable<Communication> comms = ConcreteZipIO.read(this.path);
        for (Communication c : comms) {
            indexer.add(c);
            counter++;
            if (counter % batchSize == 0) {
                logger.info("" + counter + " Processed");
            }
        }
        logger.info("" + counter + " Processed.");
    }

    private void processDirectory(LuceneCommunicationIndexer indexer, String path, int batchSize) throws IOException {
        CommunicationSerializer serializer = new CompactCommunicationSerializer();
        int counter = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path))) {
            for (Path entry : stream) {
                Communication comm = serializer.fromPath(entry);
                indexer.add(comm);
                counter++;
                if (counter % batchSize == 0) {
                    logger.info("" + counter + " Processed");
                }
            }
            logger.info("" + counter + " Processed.");
        } catch (ConcreteException e) {
            throw new IOException(e);
        }
    }
}
