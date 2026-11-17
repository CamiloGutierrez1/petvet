FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY ms-notificaciones/pom.xml ms-notificaciones/pom.xml
COPY ms-notificaciones/src ms-notificaciones/src
RUN --mount=type=cache,id=petvet-maven,target=/root/.m2,sharing=locked \
    mvn -B -ntp -f ms-notificaciones/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/ms-notificaciones/target/ms-notificaciones-0.0.1-SNAPSHOT.jar application.jar
USER 10001:10001
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
