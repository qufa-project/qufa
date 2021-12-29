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
spark.sparkContext.setLogLevel("ERROR")


def process_row(row):
	print(row)


def main():
	df = spark \
		.readStream \
		.format("kafka") \
		.option("kafka.bootstrap.servers", bootstrap_servers) \
		.option("subscribe", topic) \
		.option("startingOffsets", "latest") \
		.option("endinggOffsets", "latest") \
		.load()

	'''
	query = df \
		.selectExpr("CAST(value AS STRING)", "timestamp") \
		.writeStream \
		.format("console") \
		.option("truncate", False) \
		.outputMode("append") \
		.start() \
		.awaitTermination()
	'''

	schema = "sex INT,age INT,cva INT,fcvayn INT,packyear INT,sd_idr2 INT,exerfq INT"
	df1 = df.selectExpr("CAST(value AS STRING)", "timestamp")
	df2 = df1.select(from_csv(col("value"), schema).alias(topic), "timestamp")
	df3 = df2.select(topic + ".*", "timestamp").createOrReplaceTempView("algorithm")
	sql = spark.sql("SELECT * FROM algorithm")
	query = sql \
		.writeStream \
		.outputMode("append") \
		.option("truncate", "false") \
		.format("memory") \
		.queryName("algorithm") \
		.foreach(process_row) \
		.start() \
		.awaitTermination()


if __name__ == "__main__":
	main()

