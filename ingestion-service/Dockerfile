FROM eclipse-temurin:17-jdk

LABEL authors="Ayoze"

COPY target/ingestion-service-1.0-SNAPSHOT.jar /app/ingestion.jar

WORKDIR /app

ENTRYPOINT ["java", "-jar", "ingestion.jar"]
