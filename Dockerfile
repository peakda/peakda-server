FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test \
    && cp build/libs/*-SNAPSHOT.jar app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

# compose healthcheck 가 /actuator/health/readiness 를 찌를 때 필요하다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=builder /workspace/app.jar app.jar

# 2GB 인스턴스에서 PostgreSQL·Redis·Caddy 와 함께 뜬다.
# 컨테이너 mem_limit(1200m) 안에서 힙을 고정해 다른 컨테이너를 밀어내지 않게 한다.
ENV JAVA_OPTS="-Xmx768m -XX:MaxRAMPercentage=75.0"

# 프로파일은 배포 환경이 주입한다. 이미지에 고정하지 않는다.
ENV SPRING_PROFILES_ACTIVE=""

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
