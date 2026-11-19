# Спочатку зібери jar (з папки lab/):
#   mvn -pl clinic-service -am package -DskipTests
# Потім образ (з папки lab/):
#   docker build -t clinic-service clinic-service
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
