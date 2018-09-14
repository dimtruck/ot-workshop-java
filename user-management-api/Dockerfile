FROM openjdk:8

COPY . /opt

WORKDIR /opt

EXPOSE 8080

RUN ./gradlew build

CMD ./gradlew bootRun