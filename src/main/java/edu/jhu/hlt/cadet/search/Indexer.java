/*
 * Copyright 2012-2017 Johns Hopkins University HLTCOE. All rights reserved.
 * This software is released under the 2-clause BSD license.
 * See LICENSE in the project root directory.
 */
package edu.jhu.hlt.cadet.search;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.thrift.TException;

public interface Indexer {
    public void index(Config config) throws IOException, TException;

    public class Config {
        public boolean useLuceneTokenizer;
        public int batchSize;
        public Path indexDir;
    }
}
