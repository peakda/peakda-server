# jar 는 CI 러너가 Gradle 캐시를 재사용해 빌드한다.
# 컨테이너 안에서 다시 컴파일하면 러너에서 이미 끝낸 컴파일을 캐시 없이 반복하게 된다.
#
# JRE 패치 버전을 태그로 고정한다. 떠다니는 21-jre 는 빌드 시점마다 다른 JRE 가 들어가 어떤 버전이
# 배포됐는지 이력에 남지 않는다. 새 패치는 Dependabot 이 PR 로 올린다.
# 서버는 ARM64(OCI A1)라 새 태그의 arm64 이미지가 공개됐는지 확인한 뒤 머지한다.
FROM eclipse-temurin:21.0.13.1_2-jre-noble
WORKDIR /app

# compose healthcheck 가 /actuator/health/readiness 를 찌를 때 필요하다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

ARG JAR_FILE=build/libs/app.jar
COPY ${JAR_FILE} app.jar

# 2GB 인스턴스에서 PostgreSQL·Redis·Caddy 와 함께 뜬다.
# 컨테이너 mem_limit(1200m) 안에서 힙을 고정해 다른 컨테이너를 밀어내지 않게 한다.
ENV JAVA_OPTS="-Xmx768m -XX:MaxRAMPercentage=75.0"

# 프로파일은 배포 환경이 주입한다. 이미지에 고정하지 않는다.
ENV SPRING_PROFILES_ACTIVE=""

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
