# Multi-stage build for Quarkus backend
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/quarkus-app/lib/ /app/lib/
COPY --from=build /app/target/quarkus-app/*.jar /app/
COPY --from=build /app/target/quarkus-app/app/ /app/app/
COPY --from=build /app/target/quarkus-app/quarkus/ /app/quarkus/

ENV QUARKUS_HTTP_HOST=0.0.0.0
ENV PORT=8081
EXPOSE 8081

CMD ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "/app/quarkus-run.jar"]

