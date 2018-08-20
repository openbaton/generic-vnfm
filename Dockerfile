FROM openjdk:8-jdk as builder
COPY . /project
WORKDIR /project
RUN ./gradlew build

FROM openjdk:8-jre-alpine
COPY --from=builder /project/build/libs/*.jar /vnfm-generic.jar
RUN mkdir -p /var/log/openbaton
ENTRYPOINT ["java", "-jar", "/vnfm-generic.jar"]
