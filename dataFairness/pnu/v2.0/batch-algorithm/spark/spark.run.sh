#!/bin/bash

if [ $# -ne 1 ]; then
	echo "Usage: $0 filename"
	exit -1
fi

spark-submit --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.1.2 $1

