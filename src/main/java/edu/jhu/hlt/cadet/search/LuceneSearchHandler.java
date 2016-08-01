package edu.jhu.hlt.cadet.search;

import java.util.ArrayList;

import org.apache.thrift.TException;

import edu.jhu.hlt.concrete.AnnotationMetadata;
import edu.jhu.hlt.concrete.search.Search;
import edu.jhu.hlt.concrete.search.SearchQuery;
import edu.jhu.hlt.concrete.search.SearchResult;
import edu.jhu.hlt.concrete.search.SearchResults;
import edu.jhu.hlt.concrete.services.ServiceInfo;
import edu.jhu.hlt.concrete.services.ServicesException;
import edu.jhu.hlt.concrete.uuid.AnalyticUUIDGeneratorFactory;

public class LuceneSearchHandler implements Search.Iface,  AutoCloseable {
    private final String dataDir;
    private final AnalyticUUIDGeneratorFactory.AnalyticUUIDGenerator uuidGen;

    public LuceneSearchHandler(String dataDir) {
        this.dataDir = dataDir;
        AnalyticUUIDGeneratorFactory f = new AnalyticUUIDGeneratorFactory();
        this.uuidGen = f.create();
    }

    @Override
    public SearchResults search(SearchQuery query) throws ServicesException, TException {
        SearchResults results = createResultsContainer(query);

        // get results from lucene
        results.setSearchResults(new ArrayList<SearchResult>());

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

    @Override
    public ServiceInfo about() throws TException {
        ServiceInfo info = new ServiceInfo("Lucene search", "1.0.0");
        return info;
    }

    @Override
    public boolean alive() throws TException {
        return true;
    }

    @Override
    public void close() {}

}
