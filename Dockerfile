FROM openjdk:21-jdk-slim

RUN apt-get update && \
    apt-get install -y curl bash && \
    curl -L https://github.com/sbt/sbt/releases/download/v1.9.7/sbt-1.9.7.tgz | tar xz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/bin/sbt

WORKDIR /app

COPY build.sbt /app/
COPY project /app/project
COPY src /app/src

RUN sbt compile

CMD ["sbt", "run"]