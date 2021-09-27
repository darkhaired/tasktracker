FROM openjdk:8-jdk-alpine
RUN mkdir -p /app
ARG target_file_name
COPY ${target_file_name} /app/app.jar

ARG PROFILE=local
ARG JAVA_OPTS="-Xms512M -Xmx2G"

ENV PROFILE=${PROFILE}
ENV JAVA_OPTS=${JAVA_OPTS}

ENTRYPOINT java \
${JAVA_OPTS} \
-jar /app/app.jar \
--spring.config.location=classpath:/${PROFILE}/application.properties \
