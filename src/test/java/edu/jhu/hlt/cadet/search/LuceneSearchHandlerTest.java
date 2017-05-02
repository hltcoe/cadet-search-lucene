package edu.jhu.hlt.cadet.search;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.apache.thrift.TException;
import org.junit.Test;

import edu.jhu.hlt.concrete.lucene.ConcreteLuceneSearcher;
import edu.jhu.hlt.concrete.search.SearchQuery;
import edu.jhu.hlt.concrete.search.SearchResult;
import edu.jhu.hlt.concrete.services.ServicesException;

public class LuceneSearchHandlerTest {

    @Test
    public void testWithEmptyQuery() throws ServicesException, TException, IOException {
        ConcreteLuceneSearcher searcher = mock(ConcreteLuceneSearcher.class);
        LuceneSearchHandler handler = new LuceneSearchHandler("eng", searcher);
        SearchQuery query = new SearchQuery();
        query.setRawQuery(" ");

        SearchResult result = handler.search(query);

        assertEquals(0, result.getSearchResultItemsSize());

        handler.close();
    }

}
