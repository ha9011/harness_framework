# Stage 1: builder
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

# 의존성 캐시 최대화: gradle 설정만 먼저 복사
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# 소스 복사 후 bootJar (테스트는 빌드에서 제외 — CI에서 별도 실행)
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# Stage 2: runtime
FROM eclipse-temurin:17-jre-jammy
RUN useradd -r -u 1001 -g root spring \
  && mkdir -p /app/logs \
  && chown -R spring /app
USER spring
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
