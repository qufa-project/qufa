from pyspark.sql import SparkSession
from pyspark.sql.types import *
from pyspark.sql.functions import *

# ref: https://qkqhxla1.tistory.com/1143

bootstrap_servers = "localhost:9091,localhost:9092,localhost:9093"
topic = "algorithm-tpr"
spark = SparkSession \
	.builder \
	.appName("appName") \
	.master("local[*]") \
	.getOrCreate()
jsonSchema = StructType([
	StructField("name", StringType(), True), 
	StructField("age", IntegerType(), True), 
	StructField("height", DoubleType(), True)
])


def main():
	df = spark \
		.readStream \
		.format("kafka") \
		.option("kafka.bootstrap.servers", bootstrap_servers) \
		.option("subscribe", topic) \
		.option("startingOffsets", "earliest") \
		.option("endinggOffsets", "latest") \
		.load()

	query = df \
		.selectExpr("CAST(value AS STRING)") \
		.writeStream \
		.format("console") \
		.option("numRows", 50) \
		.option("truncate", False) \
		.outputMode("append") \
		.start() \
		.awaitTermination()


if __name__ == "__main__":
	main()

