FROM gradle:4.9-jdk-alpine

USER root

RUN apk add --no-cache bash

COPY . /opt/

WORKDIR /opt

RUN gradle build
RUN gradle shadowJar

RUN chgrp 0 /opt; chmod -R g=u /opt

USER 1001

CMD ["java", "-jar", "/opt/build/libs/consumer-1.0-SNAPSHOT-all.jar"]
