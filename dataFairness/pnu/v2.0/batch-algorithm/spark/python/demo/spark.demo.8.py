from pyspark.sql import SparkSession
from pyspark.sql.types import *
from pyspark.sql.functions import *
import time

# ref: https://qkqhxla1.tistory.com/1143

bootstrap_servers = "localhost:9091,localhost:9092,localhost:9093"
topic = "algorithm-tpr"
csv_data_path = "/Projects/qufa/stream/data/"
spark = SparkSession \
	.builder \
	.appName("appName") \
	.master("local[*]") \
	.getOrCreate()
spark.sparkContext.setLogLevel("ERROR")
schema1 = StructType([
	StructField('sex', IntegerType(), True),
	StructField('age', IntegerType(), True),
	StructField('cva', IntegerType(), True),
	StructField('fcvayn', IntegerType(), True),
	StructField('packyear', IntegerType(), True),
	StructField('sd_idr2', IntegerType(), True),
	StructField('exerfq', IntegerType(), True)
])


def foreach_function(row):
	print(row)


def foreach_batch_function(batch_df, batch_id):
	batch_df.coalesce(1).write.format("com.databricks.spark.csv").mode("append").save(csv_data_path + "output/")


def save_to_mysql(batch_df, batch_id):
	batch_df.write \
		.format("jdbc") \
		.option("driver", "org.mariadb.jdbc.Driver") \
		.option("url", "jdbc:mariadb://localhost:6606/qufa") \
		.option("dbtable", "algorithm") \
		.option("user", "qufa") \
		.option("password", "Qufa!234") \
		.save()
	'''
	db_credentials = {
		"user" : "qufa",
		"password" : "Qufa!234",
		"driver" : "org.mariadb.jdbc.Driver"
	}
	print("Printing batch_id: ")
	print(batch_id)
	print(batch_df.printSchema())
	processed_at = time.strftime("%Y-%m-%d %H:%M:%S")

	batch_df_final = batch_df \
			.withColumn("processed_at", lit(processed_at)) \
			.withColumn("batch_it", lit(batch_id))
	print(batch_df_final.printSchema())
	print("Printing before Msql table save: " + str(batch_id))
	batch_df_final.write \
		.jdbc(url = "jdbc:mariadb://localhost:6606/qufa",
			table = "algorithm",
			mode = "append",
			properties=db_credentials) 
	print("Printing after table save: " + str(batch_id))
	'''


def main():
	df = spark \
		.readStream \
		.format("kafka") \
		.option("kafka.bootstrap.servers", bootstrap_servers) \
		.option("subscribe", topic) \
		.load()
		# .option("startingOffsets", "latest") \
		# .option("endinggOffsets", "latest") \

	schema2 = "sex INT,age INT,cva INT,fcvayn INT,packyear INT,sd_idr2 INT,exerfq INT"
	df1 = df.selectExpr("CAST(value AS STRING)", "timestamp")
	df2 = df1.select(from_csv(col("value"), schema2).alias(topic), "timestamp")
	df3 = df2.select(topic + ".*", "timestamp").createOrReplaceTempView("algorithm")
	sql = spark.sql("SELECT * FROM algorithm")

	# csv
	DF1 = sql \
		.writeStream \
		.format("csv") \
		.outputMode("append") \
		.trigger(processingTime = "5 seconds") \
		.option("path", csv_data_path + "output/") \
		.option("checkpointLocation", csv_data_path + "checkpoint/") \
		.option("header", "true") \
		.start()
	'''
	# csv
	DF2 = sql \
		.writeStream \
		.option("format", "append") \
		.foreachBatch(foreach_batch_function) \
		.start()
	'''
	'''
	# jdbc
	DF3 = df \
		.writeStream \
		.option("format", "append") \
		.outputMode("update") \
		.foreachBatch(save_to_mysql) \
		.start()
	'''
	# console
	DF4 = sql \
		.writeStream \
		.format("memory") \
		.outputMode("append") \
		.option("truncate", "false") \
		.queryName("algorithm") \
		.foreach(foreach_function) \
		.start()
	DF4.awaitTermination()


if __name__ == "__main__":
	main()

