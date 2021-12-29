import argparse
import csv
import json
import sys
import time
from dateutil.parser import parse
from confluent_kafka import Producer
import socket


def acked(err, msg):
	if err is not None:
		print("Failed to deliver message: %s: %s" % (str(msg.value()), str(err)))
	else:
		print("Message produced: %s" % (str(msg.value())))


def main():
	# ref: https://gist.github.com/mtpatter/db754baa0ea5cc8e9360d067390ceda2

	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument('filename', type=str,
						help='Time series csv file.')
	parser.add_argument('topic', type=str,
						help='Name of the Kafka topic to stream.')
	parser.add_argument('--speed', type=float, default=1, required=False,
						help='Speed up time series by a given multiplicative factor.')
	args = parser.parse_args()

	topic = args.topic
	p_key = args.filename

	conf = {
		'bootstrap.servers': 'localhost:9091,localhost:9092,localhost:9093',
		'client.id': 'consumer-1'
	}
	producer = Producer(conf)

	rdr = csv.reader(open(args.filename))
	next(rdr)  # Skip header
	firstline = True

	while True:

		try:

			if firstline is True:
				'''
				line1 = next(rdr, None)
				name, age, height = line1[0], float(line1[1]), float(line1[2])
				# Convert csv columns to key value pair
				result = {}
				result['name'] = name
				result['age'] = age
				result['height'] = height
				# Convert dict to json as message format
				jresult = json.dumps(result)
				firstline = False

				producer.produce(topic, key=p_key, value=jresult, callback=acked)
				'''
				line1 = next(rdr, None)
				producer.produce(topic, key=p_key, value=','.join(line1), callback=acked)

			else:
				'''
				line = next(rdr, None)
				name, age, height = line[0], float(line[1]), float(line[2])
				result = {}
				result['name'] = name
				result['age'] = age
				result['height'] = height
				jresult = json.dumps(result)

				producer.produce(topic, key=p_key, value=jresult, callback=acked)
				'''
				line = next(rdr, None)
				producer.produce(topic, key=p_key, value=','.join(line), callback=acked)

			producer.flush()

		except TypeError:
			sys.exit()


if __name__ == "__main__":
	main()

