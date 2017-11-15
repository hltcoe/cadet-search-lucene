/*
 * Copyright 2012-2017 Johns Hopkins University HLTCOE. All rights reserved.
 * This software is released under the 2-clause BSD license.
 * See LICENSE in the project root directory.
 */

package edu.jhu.hlt.cadet.search;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.jhu.hlt.cadet.search.Indexer.Config;

import edu.jhu.hlt.concrete.lucene.ConcreteLuceneSearcher;
import edu.jhu.hlt.concrete.lucene.LuceneCommunicationSearcher;
import edu.jhu.hlt.concrete.lucene.pretokenized.TokenizedCommunicationSearcher;
import edu.jhu.hlt.concrete.search.SearchService;

public class Server {
    private static Logger logger = LoggerFactory.getLogger(Server.class);

    private static final int DEFAULT_BATCH_SIZE = 250;

    private final int port;
    private final String indexDir;
    private final String languageCode;
    private final boolean useLuceneTokenizer;
    private SearchService.Processor<SearchService.Iface> processor;
    protected TTransport transport;
    protected TCompactProtocol protocol;

    public Server(int port, String indexDir, String languageCode, boolean useLuceneTokenizer) {
        this.port = port;
        this.indexDir = indexDir;
        this.languageCode = languageCode;
        this.useLuceneTokenizer = useLuceneTokenizer;
    }

    public void indexOverNetwork(int batchSize, String fetchHost, int fetchPort) throws TException, IOException {
        Indexer indexer = new NetworkIndexer(fetchHost, fetchPort);
        Config config = new Config();
        config.batchSize = batchSize;
        config.useLuceneTokenizer = this.useLuceneTokenizer;
        config.indexDir = Paths.get(this.indexDir);
        indexer.index(config);
    }

    public void indexFromFilesystem(int batchSize, String directPath) throws TException, IOException {
        Indexer indexer = new DirectIndexer(directPath);
        Config config = new Config();
        config.batchSize = batchSize;
        config.useLuceneTokenizer = this.useLuceneTokenizer;
        config.indexDir = Paths.get(this.indexDir);
        indexer.index(config);
    }

    public void start() throws IOException {
        LuceneCommunicationSearcher searcher = null;
        if (useLuceneTokenizer) {
            searcher = new ConcreteLuceneSearcher(Paths.get(indexDir));
        } else {
            searcher = new TokenizedCommunicationSearcher(Paths.get(indexDir));
        }
        processor = new SearchService.Processor<>(new LuceneSearchHandler(languageCode, searcher));
        Runnable instance = new Runnable() {
            @Override
            public void run() {
                try {
                    launch(processor);
                } catch (TTransportException e) {
                    logger.error("Failed to start server", e);
                }
            }
        };
        new Thread(instance).start();
    }

    static public boolean indexExists(String dirName) {
        boolean exists = false;
        try {
            Directory dir = FSDirectory.open(Paths.get(dirName));
            exists = DirectoryReader.indexExists(dir);
            dir.close();
        } catch (IOException e) {
            logger.info("Unable to test if lucene index exists");
        }
        return exists;
    }

    public void launch(SearchService.Processor<SearchService.Iface> processor)
                    throws TTransportException {
        TNonblockingServerTransport transport = new TNonblockingServerSocket(port);
        TNonblockingServer.Args serverArgs = new TNonblockingServer.Args(transport);
        serverArgs = serverArgs.processorFactory(new TProcessorFactory(processor));
        serverArgs = serverArgs.protocolFactory(new TCompactProtocol.Factory());
        serverArgs = serverArgs.transportFactory(new TFramedTransport.Factory(Integer.MAX_VALUE));
        serverArgs.maxReadBufferBytes = Long.MAX_VALUE;
        TNonblockingServer server = new TNonblockingServer(serverArgs);
        logger.info("Starting lucene search on port " + port);
        server.serve();
    }

    private static class Opts {
        @Parameter(names = {"--port", "-p"}, description = "The port the server will listen on.")
        int port = 8077;

        @Parameter(names = {"--dir", "-d"}, required = true,
                        description = "Path to the directory for the index.")
        String indexDir = "/tmp/";

        @Parameter(names = {"--language", "-l"},
                        description = "The ISO 639-2/T three letter language code for corpus.")
        String languageCode = null;

        @Parameter(names = {"--fh"}, description = "The host of the fetch service.")
        String fetchHost = "localhost";

        @Parameter(names = {"--fp"}, description = "The port of the fetch service.")
        int fetchPort;

        @Parameter(names = {"--direct"}, description = "Direct ingest from a zip file or directory")
        String directIngestPath;

        @Parameter(names = {"--batch"}, description = "Batch size for indexing from fetch service.")
        int batchSize = Server.DEFAULT_BATCH_SIZE;

        @Parameter(names = {"--lt"}, description = "Use Lucene tokenizer rather than tokenization in concrete.")
        boolean useLuceneTokenizer = false;

        @Parameter(names = {"--build-index", "-b"},
                        description = "Build index pulling documents from the fetch service. (default is to not build the index)")
        boolean buildIndex = false;

        @Parameter(names = {"--run-search", "-r"},
                        description = "Run search service. Requires the index. (default is to not run search)")
        boolean runSearch = false;

        @Parameter(names = {"--help", "-h"}, help = true,
                        description = "Print the usage information and exit.")
        boolean help;
    }

    public static void main(String[] args) {
        Opts opts = new Opts();
        JCommander jc = null;
        try {
            jc = new JCommander(opts, args);
        } catch (ParameterException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(-1);
        }
        jc.setProgramName("./start.sh");
        if (opts.help) {
            jc.usage();
            return;
        }

        // index, run, or both should be selected
        if (!opts.runSearch && !opts.buildIndex) {
            System.err.println("You must select with the -r run option or -b build index");
            System.exit(-1);
        }

        Server server = new Server(opts.port, opts.indexDir, opts.languageCode, opts.useLuceneTokenizer);
        if (opts.buildIndex) {
            try {
                if (opts.fetchPort > 0) {
                    // build index from a fetch service
                    server.indexOverNetwork(opts.batchSize, opts.fetchHost, opts.fetchPort);
                } else if (opts.directIngestPath != null) {
                    // build index from a zip file or directory
                    server.indexFromFilesystem(opts.batchSize, opts.directIngestPath);
                } else {
                    System.err.println("Either fetch or direct ingest params must be set");
                    System.exit(-1);
                }
            } catch (TException | IOException e) {
                System.err.println("Unable build search index: " + e.getMessage());
                System.exit(-1);
            }
        }

        if (opts.runSearch) {
            if (!Server.indexExists(opts.indexDir)) {
                System.err.println("Cannot run search as the index does not exist");
                System.exit(-1);
            }

            try {
                server.start();
            } catch (IOException e) {
                System.err.println("Unable to use search index.");
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
}
