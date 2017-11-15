/*
 * Copyright 2012-2017 Johns Hopkins University HLTCOE. All rights reserved.
 * This software is released under the 2-clause BSD license.
 * See LICENSE in the project root directory.
 */
package edu.jhu.hlt.cadet.search;

import java.io.IOException;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.access.FetchCommunicationService;
import edu.jhu.hlt.concrete.access.FetchRequest;
import edu.jhu.hlt.concrete.access.FetchResult;
import edu.jhu.hlt.concrete.lucene.LuceneCommunicationIndexer;
import edu.jhu.hlt.concrete.lucene.NaiveConcreteLuceneIndexer;
import edu.jhu.hlt.concrete.lucene.pretokenized.TokenizedCommunicationIndexer;

public class NetworkIndexer implements Indexer {
    private static Logger logger = LoggerFactory.getLogger(NetworkIndexer.class);

    private final String fetchHost;
    private final int fetchPort;

    public NetworkIndexer(String host, int port) {
        fetchHost = host;
        fetchPort = port;
    }

    @Override
    public void index(Config config) throws IOException, TException {
        LuceneCommunicationIndexer indexer = null;
        if (config.useLuceneTokenizer) {
            indexer = new NaiveConcreteLuceneIndexer(config.indexDir);
        } else {
            indexer = new TokenizedCommunicationIndexer(config.indexDir);
        }

        FetchClientFactory factory = new FetchClientFactory();
        FetchCommunicationService.Client client = factory.createClient(fetchHost, fetchPort);
        try {
            if (!client.alive()) {
                indexer.close();
                throw new TException("Unable to talk to fetch service");
            }
        } catch (TTransportException e) {
            throw new TException("Unable to talk to fetch service");
        }
        long numComms = client.getCommunicationCount();
        long counter = 0;
        logger.info("Adding documents to index: " + numComms);
        for (long offset = 0; offset < numComms; offset += config.batchSize) {
            List<String> ids = client.getCommunicationIDs(offset, config.batchSize);
            if (ids == null) {
                indexer.close();
                throw new TException("Unable to get comm ids from fetch service");
            }
            FetchRequest request = new FetchRequest();
            request.setCommunicationIds(ids);
            FetchResult result = client.fetch(request);
            if (result == null) {
                indexer.close();
                throw new TException("Unable to get comms from fetch service");
            }
            counter += result.getCommunications().size();
            for (Communication comm : result.getCommunications()) {
                indexer.add(comm);
            }
            logger.info("Indexed " + counter + "/" + numComms + " Communications");
        }
        factory.freeClient();
        indexer.close();
    }

}
