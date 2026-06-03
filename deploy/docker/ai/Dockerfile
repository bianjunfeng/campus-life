ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-17
ARG JRE_IMAGE=eclipse-temurin:17-jre

FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace

COPY campus-life-ai/pom.xml campus-life-ai/pom.xml
COPY campus-life-ai/src campus-life-ai/src

WORKDIR /workspace/campus-life-ai
RUN mvn -DskipTests clean package

FROM ${JRE_IMAGE}
WORKDIR /app

RUN useradd -r -u 10001 campus \
    && mkdir -p /app/config /app/data/ai-knowledge \
    && chown -R campus:campus /app

COPY --from=build /workspace/campus-life-ai/target/campus-life-ai-*.jar /app/app.jar

USER campus
EXPOSE 8083
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
