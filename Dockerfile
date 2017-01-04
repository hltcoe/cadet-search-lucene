FROM openjdk:8-jre

WORKDIR /app
COPY target/*fat*.jar /app/search.jar

ENTRYPOINT [ "java", "-jar", "/app/search.jar" ]

