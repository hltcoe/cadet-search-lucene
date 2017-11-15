/*
 * Copyright 2012-2017 Johns Hopkins University HLTCOE. All rights reserved.
 * This software is released under the 2-clause BSD license.
 * See LICENSE in the project root directory.
 */
package edu.jhu.hlt.cadet.search;

import java.io.IOException;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.lucene.LuceneCommunicationIndexer;
import edu.jhu.hlt.concrete.lucene.NaiveConcreteLuceneIndexer;
import edu.jhu.hlt.concrete.lucene.pretokenized.TokenizedCommunicationIndexer;
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

        int counter = 0;
        int batchSize = 1000;
        Iterable<Communication> comms = ConcreteZipIO.read(this.path);
        for (Communication c : comms) {
            indexer.add(c);
            counter++;
            if (counter % batchSize == 0) {
                logger.info("" + counter + " Processed");
            }
        }
        logger.info("" + counter + " Processed.");
        indexer.close();
    }

}
