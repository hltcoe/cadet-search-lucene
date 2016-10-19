package edu.jhu.hlt.cadet.search;

import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.jhu.hlt.concrete.search.SearchService;

public class Server {
  private static Logger logger = LoggerFactory.getLogger(Server.class);

  private final int port;
  private final String dataDir;
  private final String languageCode;
  private SearchService.Processor<SearchService.Iface> processor;

  public Server(int port, String dataDir, String languageCode) {
    this.port = port;
    this.dataDir = dataDir;
    this.languageCode = languageCode;
  }

  public void start() {
    processor = new SearchService.Processor<>(new LuceneSearchHandler(dataDir, languageCode));
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

  public void launch(SearchService.Processor<SearchService.Iface> processor) throws TTransportException {
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
    @Parameter(names = { "--port", "-p" },
        description = "The port the server will listen on.")
    int port = 8077;

    @Parameter(names = { "--data",
        "-d" }, required = true,
        description = "Path to the data directory with the concrete comms.")
    String dataDir;

    @Parameter(names = { "--language", "-l" },
        description = "The ISO 639-2/T three letter language code for corpus.")
    String languageCode = "eng";

    @Parameter(names = { "--help", "-h" }, help = true,
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

    Server server = new Server(opts.port, opts.dataDir, opts.languageCode);
    server.start();
  }
}
