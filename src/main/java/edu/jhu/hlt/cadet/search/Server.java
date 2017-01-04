package edu.jhu.hlt.cadet.search;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

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

import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.access.FetchCommunicationService;
import edu.jhu.hlt.concrete.access.FetchRequest;
import edu.jhu.hlt.concrete.access.FetchResult;
import edu.jhu.hlt.concrete.lucene.ConcreteLuceneSearcher;
import edu.jhu.hlt.concrete.lucene.NaiveConcreteLuceneIndexer;
import edu.jhu.hlt.concrete.search.SearchService;

public class Server {
    private static Logger logger = LoggerFactory.getLogger(Server.class);

    private final int port;
    private final String indexDir;
    private final String languageCode;
    private SearchService.Processor<SearchService.Iface> processor;
    private final int fetchPort;
    private final String fetchHost;
    protected TTransport transport;
    protected TCompactProtocol protocol;

    public Server(int port, String indexDir, String languageCode, String fetchHost, int fetchPort) {
        this.port = port;
        this.indexDir = indexDir;
        this.languageCode = languageCode;
        this.fetchHost = fetchHost;
        this.fetchPort = fetchPort;
    }

    public void index() throws TException, IOException {
        NaiveConcreteLuceneIndexer indexer = new NaiveConcreteLuceneIndexer(Paths.get(indexDir));

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
        logger.info("Adding documents to index: " + numComms);
        long batchSize = 100;
        for (long offset = 0; offset < numComms; offset += batchSize) {
            List<String> ids = client.getCommunicationIDs(offset, batchSize);
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
            for (Communication comm : result.getCommunications()) {
                indexer.add(comm);
            }
        }
        factory.freeClient();
        indexer.close();
    }

    public void start() throws IOException {
        ConcreteLuceneSearcher searcher = new ConcreteLuceneSearcher(Paths.get(indexDir));
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
        String languageCode = "eng";

        @Parameter(names = {"--fh"}, required = true, description = "The host of the fetch service.")
        String fetchHost;

        @Parameter(names = {"--fp"}, required = true, description = "The port of the fetch service.")
        int fetchPort;

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

        Server server = new Server(opts.port, opts.indexDir, opts.languageCode, opts.fetchHost, opts.fetchPort);
        try {
            server.index();
        } catch (TException | IOException e) {
            System.err.println("Unable build search index: " + e.getMessage());
            System.exit(-1);
        }

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Unable to use search index - this shouldn't happen since we just built it");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
