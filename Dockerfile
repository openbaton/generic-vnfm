FROM openjdk:8-jdk as builder
COPY . /project
WORKDIR /project
RUN ./gradlew build -x test

FROM openjdk:8-jre-alpine
COPY --from=builder /project/build/libs/*.jar /vnfm-generic.jar
RUN mkdir -p /var/log/openbaton
COPY --from=builder /project/src/main/resources/application.properties /etc/openbaton/openbaton-vnfm-generic.properties
ENTRYPOINT ["java", "-jar", "/vnfm-generic.jar", "--spring.config.location=file:/etc/openbaton/openbaton-vnfm-generic.properties"]
