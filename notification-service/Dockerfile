# Спочатку зібери jar (з папки lab/):
#   mvn -pl notification-service -am package -DskipTests
# Потім образ (з папки lab/):
#   docker build -t notification-service notification-service
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
