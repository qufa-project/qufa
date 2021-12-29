from pyspark.context import SparkContext
from pyspark.sql.session import SparkSession
#from pyspark.streaming import StreamingContext
#from pyspark.streaming.kafka import KafkaUtils


def main():
	'''
	sc = SparkContext(appName="qufa")
	ssc = StreamingContext(sc, 5)
	message = KafkaUtils.createDirectStream(ssc, topics=["test"], kafkaParams={"metadata.broker.list":"localhost:9092"})
	messate.pprint()
	ssc.start()
	ssc.awaitTermination()
	'''

	spark = SparkSession \
		.builder \
		.appName("appName") \
		.getOrCreate()

	df = spark \
		.read \
		.format("kafka") \
		.option("kafka.bootstrap.servers", "164.125.37.214:9091,164.125.37.214:9092,164.125.37.214:9093") \
		.option("subscribe", "algorithm-tpr") \
		.load()
	df.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")


if __name__ == "__main__":
	main()

