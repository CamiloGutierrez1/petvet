FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY ms-mascotas-duenos/pom.xml ms-mascotas-duenos/pom.xml
COPY ms-mascotas-duenos/src ms-mascotas-duenos/src
RUN --mount=type=cache,id=petvet-maven,target=/root/.m2,sharing=locked \
    mvn -B -ntp -f ms-mascotas-duenos/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/ms-mascotas-duenos/target/ms-mascotas-duenos-0.0.1-SNAPSHOT.jar application.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
