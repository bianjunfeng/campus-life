ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-17
ARG JRE_IMAGE=eclipse-temurin:17-jre

FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace

COPY campus-life-backend/pom.xml campus-life-backend/pom.xml
COPY campus-life-backend/src campus-life-backend/src

WORKDIR /workspace/campus-life-backend
RUN mvn -DskipTests clean package

FROM ${JRE_IMAGE}
WORKDIR /app

RUN useradd -r -u 10001 campus \
    && mkdir -p /app/config /app/data/uploads \
    && chown -R campus:campus /app

COPY --from=build /workspace/campus-life-backend/target/campus-life-backend-*.jar /app/app.jar

USER campus
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
