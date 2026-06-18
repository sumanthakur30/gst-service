FROM sugamflow-common-libs:local AS build
WORKDIR /workspace

COPY docker/maven-docker-settings.xml /root/.m2/settings.xml
COPY docker/mvn-package-retry.sh /usr/local/bin/mvn-package-retry.sh
COPY gst-service ./gst-service
RUN sed -i 's/\r$//' /usr/local/bin/mvn-package-retry.sh \
    && chmod +x /usr/local/bin/mvn-package-retry.sh \
    && sh /usr/local/bin/mvn-package-retry.sh gst-service/pom.xml \
    && cp /workspace/gst-service/target/*-SNAPSHOT.jar /workspace/gst-service/app.jar

FROM sugamflow-jre:local
WORKDIR /app
COPY --from=build /workspace/gst-service/app.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]