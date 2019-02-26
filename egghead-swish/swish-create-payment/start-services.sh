#!/bin/bash
  
ZOOKEEPER_HOME="/usr/local/Cellar/zookeeper/3.4.13"
KAFKA_HOME="/usr/local/Cellar/kafka/2.1.0"

${ZOOKEEPER_HOME}/bin/zkServer start

${KAFKA_HOME}/libexec/bin/kafka-server-start.sh -daemon /usr/local/etc/kafka/server.properties
