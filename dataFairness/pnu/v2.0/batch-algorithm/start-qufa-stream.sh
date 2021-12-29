#!/bin/bash

topic="algorithm-tpr"
spark_python="./spark/python/spark.demo.py"
kafka_csv_sample="./data/input/sample.csv"
kafka_csv_health="./data/input/before_fairness.csv"

function start_kafka() {
	echo -e "Enter Kafka Producer CSV number:\n[ 1 ] Health (91k lines)\n[ 2 ] Sample (15 lines)\n[ 9 ] Exit"
	read csv
	if [ $csv == "1" ]; then
		echo "Process Kafka Producer"
		sleep 1
		python3 ./kafka/kafka.producer.py $kafka_csv_health $topic
		start_kafka
	elif [ $csv == "2" ]; then
		echo "Process Kafka Producer"
		sleep 1
		python3 ./kafka/kafka.producer.py $kafka_csv_sample $topic
		start_kafka
	elif [ $csv == "9" ]; then
		start_service
	else
		start_kafka
	fi
}

function start_service() {
	echo -e "Enter service number:\n[ 1 ] Spark Streaming\n[ 2 ] Kafka Producer\n[ 9 ] Exit"
	read service
	if [ $service == "1" ]; then
		echo "Process Spark Streaming"
		sleep 1
		./spark/spark.run.sh $spark_python
	elif [ $service == "2" ]; then
		start_kafka
	elif [ $service == "9" ]; then
		exit -1
	else
		start_service
	fi
}

start_service

