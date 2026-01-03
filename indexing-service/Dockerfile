FROM eclipse-temurin:17-jdk

LABEL authors="Fabio"

COPY target/indexing-service-1.0-SNAPSHOT.jar /app/indexing.jar

WORKDIR /app

EXPOSE 5703 5702 5701 7002

ENTRYPOINT ["java", "-jar", "indexing.jar"]
