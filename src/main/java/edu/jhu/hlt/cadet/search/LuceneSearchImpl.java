package edu.jhu.hlt.cadet.search;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.ImmutableList;

import edu.jhu.hlt.cadet.SearchServiceWrapper;
import edu.jhu.hlt.concrete.AnnotationMetadata;
import edu.jhu.hlt.concrete.UUID;
import edu.jhu.hlt.concrete.lucene.ConcreteLuceneConstants;
import edu.jhu.hlt.concrete.lucene.ConcreteLuceneSearcher;
import edu.jhu.hlt.concrete.search.Search;
import edu.jhu.hlt.concrete.search.SearchCapability;
import edu.jhu.hlt.concrete.search.SearchQuery;
import edu.jhu.hlt.concrete.search.SearchResult;
import edu.jhu.hlt.concrete.search.SearchResults;
import edu.jhu.hlt.concrete.search.SearchType;
import edu.jhu.hlt.concrete.services.ServiceInfo;
import edu.jhu.hlt.concrete.services.ServicesException;
import edu.jhu.hlt.concrete.util.Timing;
import edu.jhu.hlt.concrete.uuid.UUIDFactory;

public class LuceneSearchImpl implements Search.Iface, AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(LuceneSearchImpl.class);

  private Path p;
  private String langCode;
  private ConcreteLuceneSearcher search;
  private int maxDocs;

  private static final AnnotationMetadata getAMD() {
    return new AnnotationMetadata()
        .setTimestamp(Timing.currentLocalTime())
        .setKBest(1)
        .setTool("ScionLuceneSearchServer");
  }

  public LuceneSearchImpl(Path p, String langCode, int maxDocs) throws IOException {
    this.p = p;
    this.search = new ConcreteLuceneSearcher(p);
    this.langCode = langCode;
    this.maxDocs = maxDocs;
  }

  public LuceneSearchImpl(Path p, String langCode) throws IOException {
    this(p, langCode, 500);
  }

  @Override
  public ServiceInfo about() throws TException {
    ServiceInfo si = new ServiceInfo()
        .setName(LuceneSearchImpl.class.getSimpleName())
        .setVersion("latest")
        .setDescription("Serving up Lucene index at location: " + this.p.toString());
    return si;
  }

  @Override
  public boolean alive() throws TException {
    return true;
  }

  @Override
  public void close() throws IOException {
    this.search.close();
  }

  @Override
  public SearchResults search(SearchQuery query) throws ServicesException, TException {
    String rq = Optional.ofNullable(query.getRawQuery())
        .orElseThrow(() -> new ServicesException("This impl only uses rawQuery, which was unset."));
    try {
      SearchResults srs = new SearchResults()
          .setLang(this.langCode)
          .setUuid(UUIDFactory.newUUID())
          .setMetadata(getAMD());
      TopDocs td = this.search.search(rq, maxDocs);
      ScoreDoc[] sda = td.scoreDocs;
      for (ScoreDoc sd : sda) {
        Document d = this.search.get(sd.doc);
        SearchResult sr = new SearchResult();
        sr.setCommunicationId(d.get(ConcreteLuceneConstants.COMM_ID_FIELD));
        sr.setSentenceId(new UUID(d.get(ConcreteLuceneConstants.UUID_FIELD)));
        sr.setScore(sd.score);
        srs.addToSearchResults(sr);
      }

      return srs;
    } catch (IOException | ParseException  e) {
      throw new ServicesException("Caught exception processing query: " + e.getMessage());
    }
  }

  @Override
  public List<SearchCapability> getCapabilities() throws ServicesException, TException {
    SearchCapability sc = new SearchCapability();
    sc.setLang(this.langCode)
        .setType(SearchType.COMMUNICATIONS);
    return ImmutableList.of(sc);
  }

  /*
   *
   */
  @Override
  public List<String> getCorpora() throws ServicesException, TException {
    return ImmutableList.of();
  }

  private static class Opts {
    @Parameter(names = { "--port", "-p" }, description = "The port the server will listen on.")
    int port = 8077;

    @Parameter(names = { "--data",
        "-d" }, required = true, description = "Path to the data directory with the concrete comms.")
    String dataDir;

    @Parameter(names = { "--language", "-l" }, description = "The ISO 639-2/T three letter language code for corpus.")
    String languageCode = "eng";

    @Parameter(names = { "--help", "-h" }, help = true, description = "Print the usage information and exit.")
    boolean help;

    LuceneSearchImpl getImpl() throws IOException {
      return new LuceneSearchImpl(Paths.get(this.dataDir), this.languageCode);
    }
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

    try (LuceneSearchImpl lci = opts.getImpl();
        SearchServiceWrapper wrapper = new SearchServiceWrapper(lci, opts.port);) {
      wrapper.run();
    } catch (TException e) {
      LOGGER.error("Caught exception initializing server.", e);
    } catch (IOException e1) {
      LOGGER.error("Caught exception initializing Lucene implementation.", e1);
    }
  }
}
