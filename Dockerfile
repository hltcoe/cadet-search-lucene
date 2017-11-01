FROM maven

COPY pom.xml /opt/cadet-search-lucene/pom.xml
COPY src /opt/cadet-search-lucene/src

WORKDIR /opt/cadet-search-lucene
RUN mvn -B clean package \
        -Dskiptests=true \
        -Dmaven.test.skip=true && \
    mv `find target -name "cadet-search-lucene-fat-*.jar"` \
        cadet-search-lucene.jar && \
    mvn -B clean

ENTRYPOINT [ "java", "-jar", "cadet-search-lucene.jar" ]

CMD ["--help"]
