from pyspark.sql import SparkSession
from pyspark.sql.functions import explode
from pyspark.sql.functions import split


def main():
	# ref: https://qkqhxla1.tistory.com/1143
	spark = SparkSession \
		.builder \
		.appName("appName") \
		.getOrCreate()

	df = spark \
		.readStream \
		.format("kafka") \
		.option("kafka.bootstrap.servers", "164.125.37.214:9091,164.125.37.214:9092,164.125.37.214:9093") \
		.option("subscribe", "algorithm-tpr") \
		.option("startingOffsets", "earliest") \
		.option("endinggOffsets", "latest") \
		.load()

	query = df \
		.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)") \
		.writeStream \
		.format("console") \
		.outputMode("append") \
		.start() \
		.awaitTermination()


if __name__ == "__main__":
	main()

