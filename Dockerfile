FROM --platform=linux/amd64 openjdk:17

COPY ./target/patternsDemo-1.0-SNAPSHOT.jar /tmp

EXPOSE 8080

WORKDIR /tmp

ENTRYPOINT java -jar "patternsDemo-1.0-SNAPSHOT.jar"
