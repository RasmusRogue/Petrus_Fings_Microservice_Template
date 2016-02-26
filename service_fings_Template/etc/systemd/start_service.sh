#!/bin/bash

NAME=$1

APP_DIR=/opt/fullfacing/${NAME}

APP_CONFIG=/etc/fullfacing/${NAME}/application.conf
APP_LOG_CONFIG=/etc/fullfacing/${NAME}/logback.xml
APP_QUEUE_CONFIG=/etc/fullfacing/${NAME}/queue.properties
APP_MONGO_CONFIG=/etc/fullfacing/${NAME}/mongo.properties

APP_ARGS="-d64 -Xms128m -Xmx512m -Dlogback.configurationFile=${APP_LOG_CONFIG} -Dconfig.file=${APP_CONFIG} -Dmongo.file=${APP_MONGO_CONFIG} -Dqueue.file=${APP_QUEUE_CONFIG} ${JAVA_OPTS}"

#PID_FILE=/var/run/${NAME}.pid

java $APP_ARGS -jar ${APP_DIR}/${NAME}.jar