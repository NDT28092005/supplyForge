# --- Stage 1: compile
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn -q -e dependency:go-offline

COPY src ./src
RUN mvn -q -e -DskipTests package \
    && mv target/supplyforge-ai-*.jar /build/app.jar

# --- Stage 2: runtime (chỉ JRE — tiết kiệm RAM trên VPS)
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D appuser

COPY --from=builder /build/app.jar app.jar

USER appuser
EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=40.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
