from pyspark.context import SparkContext
from pyspark.sql.session import SparkSession

sc = SparkContext(appName="qufa")

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
	.load()

print("")
'''
print("sc.version:", sc.version)
print("sc.pythonVer:", sc.pythonVer)
print("sc.master:", sc.master)
print("spark.version:", spark.version)
'''

df.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
df.printSchema()


