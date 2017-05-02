package edu.jhu.hlt.cadet.search;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ServerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testIndexExistsWithEmptyDir() {
        String indexDir = folder.getRoot().getAbsolutePath();

        assertFalse(Server.indexExists(indexDir));
    }

}
