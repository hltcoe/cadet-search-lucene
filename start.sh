#!/usr/bin/env bash

DIR=`dirname $0`
JAR=$(find $DIR/target/ -name 'cadet-search-lucene*.jar')
java -cp $JAR:target/dependency/* edu.jhu.hlt.cadet.search.Server "$@"
