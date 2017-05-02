package edu.jhu.hlt.cadet.search;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.hlt.concrete.AnnotationMetadata;
import edu.jhu.hlt.concrete.UUID;
import edu.jhu.hlt.concrete.lucene.ConcreteLuceneConstants;
import edu.jhu.hlt.concrete.lucene.ConcreteLuceneSearcher;
import edu.jhu.hlt.concrete.search.SearchService;
import edu.jhu.hlt.concrete.search.SearchCapability;
import edu.jhu.hlt.concrete.search.SearchQuery;
import edu.jhu.hlt.concrete.search.SearchResult;
import edu.jhu.hlt.concrete.search.SearchResultItem;
import edu.jhu.hlt.concrete.search.SearchType;
import edu.jhu.hlt.concrete.services.ServiceInfo;
import edu.jhu.hlt.concrete.services.ServicesException;
import edu.jhu.hlt.concrete.uuid.AnalyticUUIDGeneratorFactory;

public class LuceneSearchHandler implements SearchService.Iface, AutoCloseable {
    private static Logger logger = LoggerFactory.getLogger(LuceneSearchHandler.class);

    private final AnalyticUUIDGeneratorFactory.AnalyticUUIDGenerator uuidGen;

    private final String languageCode;
    private final ConcreteLuceneSearcher searcher;
    private static final int MAX_RESULTS = 500;

    public LuceneSearchHandler(String languageCode, ConcreteLuceneSearcher searcher) {
        this.searcher = searcher;
        this.languageCode = languageCode;
        AnalyticUUIDGeneratorFactory f = new AnalyticUUIDGeneratorFactory();
        this.uuidGen = f.create();
    }

    @Override
    public SearchResult search(SearchQuery query) throws ServicesException, TException {
        if (searcher == null) {
            throw new ServicesException("Unable to query lucene index");
        }

        SearchResult results = createResultsContainer(query);

        if (query.getRawQuery().trim().equals("")) {
            logger.info("Short circuiting an empty query");
            results.setSearchResultItems(new ArrayList<SearchResultItem>());
            return results;
        }

        logger.info("Search query: " + query.getRawQuery());

        try {
            List<Document> docs = null;
            try {
                docs = searcher.searchDocuments(query.getRawQuery(), MAX_RESULTS);
            } catch (ParseException e) {
                logger.warn("Could not parse query: " + query.getRawQuery());
                throw new ServicesException("Unable to parse query: " + query.getRawQuery());
            }
            // the UI cannot handle a null list
            if (docs.isEmpty()) {
                results.setSearchResultItems(new ArrayList<SearchResultItem>());
            }
            for (Document doc : docs) {
                SearchResultItem result = new SearchResultItem();
                result.setCommunicationId(doc.get(ConcreteLuceneConstants.COMM_ID_FIELD));
                result.setSentenceId(new UUID(doc.get(ConcreteLuceneConstants.UUID_FIELD)));
                result.setScore(0);
                results.addToSearchResultItems(result);
            }
        } catch (IOException e) {
            logger.warn("Could not read the lucene index for search");
            throw new ServicesException(e.getMessage());
        }

        logger.info("Returning " + results.getSearchResultItemsSize() + " results");

        return results;
    }

    private SearchResult createResultsContainer(SearchQuery query) {
        SearchResult results = new SearchResult();
        results.setUuid(uuidGen.next());
        results.setSearchQuery(query);
        AnnotationMetadata metadata = new AnnotationMetadata();
        metadata.setTool("Cadet Lucene Search");
        results.setMetadata(metadata);

        return results;
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
    }

    @Override
    public List<SearchCapability> getCapabilities() throws ServicesException, TException {
        List<SearchCapability> capabilities = new ArrayList<>();

        SearchCapability communicationsCapability = new SearchCapability();
        communicationsCapability.setLang(this.languageCode);
        communicationsCapability.setType(SearchType.COMMUNICATIONS);
        capabilities.add(communicationsCapability);

        SearchCapability sentencesCapability = new SearchCapability();
        sentencesCapability.setLang(this.languageCode);
        sentencesCapability.setType(SearchType.SENTENCES);
        capabilities.add(sentencesCapability);

        return capabilities;
    }

    @Override
    public List<String> getCorpora() throws ServicesException, TException {
        List<String> corpora = new ArrayList<>();
        // TODO
        return corpora;
    }
}
