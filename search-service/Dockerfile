FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/search-service-1.0-SNAPSHOT.jar app.jar
EXPOSE 5703 5702 5701 7003
ENTRYPOINT ["java", "-jar", "app.jar"]
