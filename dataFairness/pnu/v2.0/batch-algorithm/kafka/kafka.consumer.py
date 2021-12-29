import argparse
import json
import sys
import time
from confluent_kafka import Consumer, KafkaError, KafkaException


def msg_process(msg):
	time_start = time.strftime("%Y-%m-%d %H:%M:%S")
	val = msg.value()
	dval = json.loads(val)
	print(time_start, dval)


def main():
	# ref: https://gist.github.com/mtpatter/f028c1b8477b6847cfbd664b3c17b117

	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument('topic', type=str, help='Name of the Kafka topic to stream.')

	args = parser.parse_args()

	conf = {
		'bootstrap.servers': 'localhost:9091,localhost:9092,localhost:9093',
		'default.topic.config': {'auto.offset.reset': 'smallest'},
		'group.id': 'consumer-1'
	}

	consumer = Consumer(conf)

	running = True

	try:
		while running:
			consumer.subscribe([args.topic])

			msg = consumer.poll(1)
			if msg is None:
				continue

			if msg.error():
				if msg.error().code() == KafkaError._PARTITION_EOF:
					# End of partition event
					sys.stderr.write('%% %s [%d] reached end at offset %d\n' %
									 (msg.topic(), msg.partition(), msg.offset()))
				elif msg.error().code() == KafkaError.UNKNOWN_TOPIC_OR_PART:
					sys.stderr.write('Topic unknown, creating %s topic\n' %
									 (args.topic))
				elif msg.error():
					raise KafkaException(msg.error())
			else:
				msg_process(msg)

	except KeyboardInterrupt:
		pass

	finally:
		# Close down consumer to commit final offsets.
		consumer.close()


if __name__ == "__main__":
	main()

