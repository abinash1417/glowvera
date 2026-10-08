# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home glowvera
COPY --from=build /app/target/glowvera-api-1.0.0.jar app.jar
USER glowvera
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
