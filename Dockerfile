FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY security-common ./security-common
RUN mvn -f security-common/pom.xml -B -DskipTests install

COPY gst-service ./gst-service
RUN mvn -f gst-service/pom.xml -B -DskipTests package && cp /workspace/gst-service/target/*-SNAPSHOT.jar /workspace/gst-service/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/gst-service/app.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]
