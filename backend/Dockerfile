# Uso: docker build --build-arg MODULE=sri-service -t sri-service ./backend
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /build
COPY pom.xml .
COPY sri-service/pom.xml sri-service/pom.xml
COPY ant-service/pom.xml ant-service/pom.xml
COPY sri-service/src sri-service/src
COPY ant-service/src ant-service/src
RUN mvn -q -B -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /build/${MODULE}/target/${MODULE}-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
