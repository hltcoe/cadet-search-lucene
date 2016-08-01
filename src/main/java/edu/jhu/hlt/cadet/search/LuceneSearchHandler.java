package edu.jhu.hlt.cadet.search;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.hlt.concrete.AnnotationMetadata;
import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.Section;
import edu.jhu.hlt.concrete.Sentence;
import edu.jhu.hlt.concrete.TextSpan;
import edu.jhu.hlt.concrete.UUID;
import edu.jhu.hlt.concrete.search.Search;
import edu.jhu.hlt.concrete.search.SearchQuery;
import edu.jhu.hlt.concrete.search.SearchResult;
import edu.jhu.hlt.concrete.search.SearchResults;
import edu.jhu.hlt.concrete.serialization.CompactCommunicationSerializer;
import edu.jhu.hlt.concrete.services.ServiceInfo;
import edu.jhu.hlt.concrete.services.ServicesException;
import edu.jhu.hlt.concrete.util.ConcreteException;
import edu.jhu.hlt.concrete.uuid.AnalyticUUIDGeneratorFactory;

public class LuceneSearchHandler implements Search.Iface,  AutoCloseable {
    private static Logger logger = LoggerFactory.getLogger(LuceneSearchHandler.class);

    private static final String FIELD_CONTENTS = "contents";
    private static final String FIELD_COMM_ID = "comm_id";
    private static final String FIELD_SENT_ID = "sent_id";

    public static final String EXTENSION = "concrete";

    private final AnalyticUUIDGeneratorFactory.AnalyticUUIDGenerator uuidGen;
    private final CompactCommunicationSerializer serializer;

    private final String dataDir;
    private Directory luceneDir;
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private static final int MAX_RESULTS = 500;

    public LuceneSearchHandler(String dataDir) {
        this.serializer = new CompactCommunicationSerializer();
        this.dataDir = dataDir;
        AnalyticUUIDGeneratorFactory f = new AnalyticUUIDGeneratorFactory();
        this.uuidGen = f.create();

        validateDirectory(dataDir);

        analyzer = new StandardAnalyzer();
        try {
            buildIndex();
            IndexReader reader = DirectoryReader.open(luceneDir);
            searcher = new IndexSearcher(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to build lucene index for search", e);
        }
    }

    private void validateDirectory(String dir) {
        File file = new File(dir);
        if (!file.exists()) {
            throw new RuntimeException("Directory " + dir + " does not exist");
        }
        if (!file.isDirectory()) {
            throw new RuntimeException(dir + " is not a directory");
        }        
    }

    private void buildIndex() throws IOException {
        luceneDir = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        IndexWriter writer = new IndexWriter(luceneDir, config);

        int count = 0;
        Communication comm;
        File dir = new File(dataDir);
        for (File file : dir.listFiles()) {
            try {
                comm = serializer.fromPath(Paths.get(file.getAbsolutePath()));
            } catch (ConcreteException e) {
                logger.warn("Cannot deserialize " + file.getName());
                continue;
            }

            if (addCommunication(writer, comm)) {
                count++;
            }
        }

        writer.close();
        logger.info("Completed building the lucene index with size " + count);
    }

    private boolean addCommunication(IndexWriter writer, Communication comm) {
        if (!comm.isSetText() || !comm.isSetSectionList()) {
            logger.warn("Concrete comm " + comm.getId() + " is invalid");
            return false;
        }
        Iterator<Section> sections = comm.getSectionListIterator();
        while (sections.hasNext()) {
            Section section = sections.next();
            if (!section.isSetSentenceList()) {
                logger.info("Empty section in " + comm.getId());
                continue;
            }
            Iterator<Sentence> sentences = section.getSentenceListIterator();
            while (sentences.hasNext()) {
                Sentence sentence = sentences.next();
                Document doc = new Document();
                doc.add(new StoredField(FIELD_COMM_ID, comm.getId()));
                doc.add(new StoredField(FIELD_SENT_ID, sentence.getUuid().getUuidString()));
                String content = getTextFromSpan(comm, sentence.getTextSpan());
                doc.add(new TextField(FIELD_CONTENTS, content, Store.YES));
                try {
                    writer.addDocument(doc);
                } catch (IOException e) {
                    logger.warn("Failed to add " + comm.getId() + " to the lucene index");
                    return false;
                }
            }
        }
        return true;
    }

    private String getTextFromSpan(Communication comm, TextSpan span) {
        return comm.getText().substring(span.getStart(), span.getEnding());
    }

    @Override
    public SearchResults search(SearchQuery query) throws ServicesException, TException {
        if (searcher == null) {
            throw new ServicesException("Unable to query lucene index");
        }

        SearchResults results = createResultsContainer(query);

        try {
            Query luceneQuery = createLuceneQuery(query.getRawQuery());
            TopDocs topDocs = searcher.search(luceneQuery, MAX_RESULTS);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document document = searcher.doc(scoreDoc.doc);
                SearchResult result = new SearchResult();
                result.setCommunicationId(document.get(FIELD_COMM_ID));
                result.setSentenceId(new UUID(document.get(FIELD_SENT_ID)));
                result.setScore(scoreDoc.score);
                results.addToSearchResults(result);
            }
        } catch (IOException e) {
            logger.warn("Could not read the lucene index for search");
            throw new ServicesException(e.getMessage());
        }

        return results;
    }

    private SearchResults createResultsContainer(SearchQuery query) {
        SearchResults results = new SearchResults();
        results.setUuid(uuidGen.next());
        results.setSearchQuery(query);
        AnnotationMetadata metadata = new AnnotationMetadata();
        metadata.setTool("Cadet Lucene Search");
        results.setMetadata(metadata);

        return results;
    }

    private Query createLuceneQuery(String queryText) throws ServicesException {
        QueryParser queryParser = new QueryParser(FIELD_CONTENTS, analyzer);
        try {
            return queryParser.parse(queryText);
        } catch (ParseException e) {
            logger.warn("Bad query string for lucene: " + queryText, e);
            throw new ServicesException("Cannot understand query: " + queryText);
        }
    }

    @Override
    public ServiceInfo about() throws TException {
        ServiceInfo info = new ServiceInfo("Cadet Lucene Search", "1.0.0");
        return info;
    }

    @Override
    public boolean alive() throws TException {
        return true;
    }

    @Override
    public void close() throws IOException {
        if (searcher != null) {
            searcher.getIndexReader().close();
        }
        luceneDir.close();
    }

}
