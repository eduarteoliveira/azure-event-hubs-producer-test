package org.cerc.billing.producer;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;

public class KafkaProducerBilling<K, V> extends KafkaProducer<K, V> {

	protected KafkaProducerBilling(Properties properties) {
		super(properties);
	}
}
