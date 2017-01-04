#!/usr/bin/env bash

DIR=`dirname $0`
JAR=$(find $DIR/target/ -name 'cadet-search-lucene-fat*.jar')
java -cp $JAR edu.jhu.hlt.cadet.search.Server "$@"
