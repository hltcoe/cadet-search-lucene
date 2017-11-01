FROM hltcoe/concrete-java:v4.14.1

ENV CSL=/home/concrete/cadet-search-lucene

ADD . $CSL

USER root
RUN chown -R concrete:concrete $CSL
USER concrete

WORKDIR $CSL
RUN mvn -B clean package \
        -Dskiptests=true \
        -Dmaven.test.skip=true && \
    mv `find target -name "cadet-search-lucene-fat-*.jar"` \
        cadet-search-lucene.jar && \
    mvn -B clean && \
    rm -rf /home/concrete/.m2

ENTRYPOINT [ "java", "-jar", "cadet-search-lucene.jar" ]

CMD ["--help"]
