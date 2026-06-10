# Build (JDK)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle content-schema.json ./
COPY gradle gradle
COPY src src
RUN chmod +x gradlew \
    && ./gradlew bootJar --no-daemon -x test \
    && cp build/libs/board-svc-*-SNAPSHOT.jar /workspace/app.jar

# Run (JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/app.jar app.jar
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
