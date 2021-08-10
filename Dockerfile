FROM adoptopenjdk:11-jre-hotspot

ENV USER_JAVA_OPTS ""

ENV JAVA_OPTS -XX:+UnlockExperimentalVMOptions \
              -XX:+UseContainerSupport \
              -Djava.util.concurrent.ForkJoinPool.common.parallelism=2 \
              -Djava.security.egd=file:/dev/./urandom

WORKDIR /application

COPY target/ignite-client-*.jar /application/ignite-client.jar
COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT [ "/entrypoint.sh" ]
CMD java $JAVA_OPTS $USER_JAVA_OPTS -jar ignite-client.jar

EXPOSE 8080

ARG PROJECT_NAME
ARG VERSION
ARG BUILD_DATE
ARG PROJECT_URL
ARG COMMIT_SHA
ARG COMMIT_REF_NAME
ARG PIPELINE_URL

LABEL org.opencontainers.image.title="${PROJECT_NAME}" \
      org.opencontainers.image.version="${VERSION:-0.0.0-SNAPSHOT}" \
      org.opencontainers.image.created=${BUILD_DATE} \
      org.opencontainers.image.source="${PROJECT_URL:-local}" \
      org.opencontainers.image.ref.name="${COMMIT_REF_NAME}" \
      org.opencontainers.image.revision="${COMMIT_SHA}" \
      org.opencontainers.image.ci-pipeline-url=${PIPELINE_URL} \
      org.opencontainers.image.vendor="BI.ZONE"
