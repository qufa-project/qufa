#!/bin/bash

if [ $# -ne 1 ]; then
	echo "Usage: $0 topic"
	exit -1
fi

sudo docker exec -it kafka_kafka1_1 kafka-topics --create --topic $1 --zookeeper zookeeper1:2181,zookeeper2:2181,zookeeper3:2181 --replication-factor 3 --partitions 3
