FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY . .

WORKDIR /workspace/backend/trend-agent/trend-agent
RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/backend/trend-agent/trend-agent/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
