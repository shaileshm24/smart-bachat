ARG BASE_IMAGE_VERSION

ARG BASE_IMAGE=eclipse-temurin:25-jdk-jammy
FROM ${BASE_IMAGE} AS builder

ARG ARTIFACT_VERSION
ARG ARTIFACT_JAR_PATH=./build/libs
ARG ARTIFACT_NAME=bachat-core-service
ARG ARTIFACT_JAR_NAME=${ARTIFACT_NAME}-${ARTIFACT_VERSION}.jar
ARG APP_DIR=/com/fyndna/app

USER root

WORKDIR ${APP_DIR}

COPY ${ARTIFACT_JAR_PATH}/${ARTIFACT_JAR_NAME} ${APP_DIR}/build/libs/

RUN java -Djarmode=layertools -jar ${APP_DIR}/build/libs/${ARTIFACT_JAR_NAME} extract

FROM ${BASE_IMAGE}

WORKDIR ${APP_DIR}


COPY --from=builder ${APP_DIR}/dependencies/ ${APP_DIR}/spring-boot-loader/ ${APP_DIR}/snapshot-dependencies/ ${APP_DIR}/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]