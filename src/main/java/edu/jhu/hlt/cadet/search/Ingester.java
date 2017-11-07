/*
 * Copyright 2012-2017 Johns Hopkins University HLTCOE. All rights reserved.
 * This software is released under the 2-clause BSD license.
 * See LICENSE in the project root directory.
 */
package edu.jhu.hlt.cadet.search;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.lucene.LuceneCommunicationIndexer;
import edu.jhu.hlt.concrete.lucene.pretokenized.TokenizedCommunicationIndexer;
import edu.jhu.hlt.concrete.zip.ConcreteZipIO;

/**
 * Directly build an index from files rather than iterating over fetch service.
 *
 * Supports zip file only.
 */
public class Ingester {

    private static class Opts {
        @Parameter(names = {"--path", "-p"}, required = true,
                        description = "File or directory to run the ingest on")
        String path;

        @Parameter(names = {"--dir", "-d"}, required = true,
                        description = "Path to the directory for the index.")
        String indexDir;

        @Parameter(names = {"--help", "-h"}, help = true,
                        description = "Print the usage information and exit.")
        boolean help;
    }

    public static void main(String[] args) throws IOException {
        Opts opts = new Opts();
        JCommander jc = null;
        try {
            jc = new JCommander(opts, args);
        } catch (ParameterException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(-1);
        }
        jc.setProgramName("./ingest.sh");
        if (opts.help) {
            jc.usage();
            return;
        }

        Path indexDir = Paths.get(opts.indexDir);
        Path ingestPath = Paths.get(opts.path);
        LuceneCommunicationIndexer indexer = new TokenizedCommunicationIndexer(indexDir);

        int counter = 0;
        int batchSize = 1000;
        Iterable<Communication> comms = ConcreteZipIO.read(opts.path);
        for (Communication c : comms) {
            indexer.add(c);
            counter++;
            if (counter % batchSize == 0) {
                System.out.println("" + counter + " Processed");
            }
        }
        indexer.close();
    }
}
