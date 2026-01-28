FROM docker.io/eclipse-temurin:17-jre-alpine

WORKDIR /app
RUN mkdir -p logs

COPY target/${artifactId}-${version}.jar ./app.jar
COPY src/main/resources/default-application.yml application.yml
COPY src/main/resources/default-logback-spring.xml logback-spring.xml
EXPOSE 1111
EXPOSE 8080
ENTRYPOINT ["java", \
    "-Xms32m", \
    "-Xmx64m", \
    "-XX:MaxMetaspaceSize=48m", \
    "-XX:+UseSerialGC", \
    "-jar", \
    "/app/app.jar"]