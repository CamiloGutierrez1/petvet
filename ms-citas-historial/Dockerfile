FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY ms-citas-historial/pom.xml ms-citas-historial/pom.xml
COPY ms-citas-historial/src ms-citas-historial/src
RUN --mount=type=cache,id=petvet-maven,target=/root/.m2,sharing=locked \
    mvn -B -ntp -pl ms-citas-historial -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/ms-citas-historial/target/ms-citas-historial-0.0.1-SNAPSHOT.jar application.jar
USER 10001:10001
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
