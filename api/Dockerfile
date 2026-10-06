## STAGE 1 - BUILD
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

## STAGE 2 - RUN
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system app
COPY --from=build /app/target/app.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60"
EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]