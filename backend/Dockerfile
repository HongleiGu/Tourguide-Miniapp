# syntax=docker/dockerfile:1

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY docker/settings.xml settings.xml
COPY pom.xml .
COPY src ./src
# Cache the local Maven repo across builds for fast rebuilds.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -s settings.xml -DskipTests clean package

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
