# syntax=docker/dockerfile:1.7
ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-17
ARG JRE_IMAGE=eclipse-temurin:17-jre

FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace

COPY deploy/docker/maven/settings.xml /usr/share/maven/conf/settings.xml
COPY campus-life-gateway/pom.xml campus-life-gateway/pom.xml
COPY campus-life-gateway/src campus-life-gateway/src

WORKDIR /workspace/campus-life-gateway
RUN --mount=type=cache,target=/root/.m2/repository mvn -DskipTests clean package

FROM ${JRE_IMAGE}
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -r -u 10001 campus \
    && mkdir -p /app/config \
    && chown -R campus:campus /app

COPY --from=build /workspace/campus-life-gateway/target/campus-life-gateway-*.jar /app/app.jar

USER campus
EXPOSE 8090
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
