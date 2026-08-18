FROM eclipse-temurin:21-jre
LABEL authors="gkk"
WORKDIR /app

COPY app.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]