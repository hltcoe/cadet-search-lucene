/*
 * Copyright 2012-2018 Johns Hopkins University HLTCOE. All rights reserved.
 * This software is released under the 2-clause BSD license.
 * See LICENSE in the project root directory.
 */
package edu.jhu.hlt.cadet.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import edu.jhu.hlt.concrete.lucene.ConcreteLuceneConstants;

/**
 * Dump an index to stdout.
 * 
 * The format is:
 * doc id (sent uuid) : [term1] [term2] ...
 * 
 * To run: ./dump.sh [index dir]
 */
public class Dump {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: ./dump.sh [index directory]");
            System.exit(-1);
        }

        Path p = Paths.get(args[0]);
        if (!Files.exists(p) || !Files.isDirectory(p)) {
            throw new IOException("Index directory does not exist: " + args[0]);
        }

        Directory luceneDir = FSDirectory.open(p);
        IndexReader reader = DirectoryReader.open(luceneDir);
        for (int i=0; i<reader.maxDoc(); i++) {
            Document doc = reader.document(i);
            String docId = doc.get(ConcreteLuceneConstants.COMM_ID_FIELD);
            String sentId = doc.get(ConcreteLuceneConstants.SENT_UUID_FIELD);
            Terms terms = reader.getTermVector(i, ConcreteLuceneConstants.TEXT_FIELD);
            if (terms != null) {
                TermsEnum iterms = terms.iterator();
                BytesRef text;
                System.out.print(String.format("%s (%s) : ", docId, sentId));
                while ((text = iterms.next()) != null) {
                    System.out.print(text.utf8ToString() + " ");
                }
                System.out.println();
            } else {
                System.out.println(String.format("%s (%s) : NO_TERMS", docId, sentId));
            }
        }
        reader.close();
        luceneDir.close();
    }

}
