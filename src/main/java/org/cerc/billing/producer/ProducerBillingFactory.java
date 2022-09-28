package org.cerc.billing.producer;

import java.util.Objects;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;
import org.apache.kafka.common.serialization.StringSerializer;
import org.cerc.billing.auth.CustomAuthenticateGCPAzureFederatedCredentialsCallbackHandler;

import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

public class ProducerBillingFactory<K, V> {

	private String bootstrapServers;
	private String clientId;

	private String securityProtocol = "SASL_SSL";
	private String saslMechanism = "OAUTHBEARER";
	private String saslJaasConfig = String.format("%s %s",
			OAuthBearerLoginModule.class.getName(), "required;");
	private String saslLoginCallbackHandlerClass = CustomAuthenticateGCPAzureFederatedCredentialsCallbackHandler.class
			.getName();

	private String keySerializer = StringSerializer.class.getName();
	private String valueSerializer = ObjectMapperSerializer.class.getName();
	private String schemaRegistryUrl;
	private String valueSubjectNameStrategy = TopicRecordNameStrategy.class
			.getName();

	/**
	 * Reference:
	 * https://learn.microsoft.com/en-us/azure/event-hubs/apache-kafka-configurations
	 **/
	private Integer maxRequestSize = 1000000;
	private Integer retries = 2;
	private Integer requestTimeoutMs = 30000;
	private Integer metadataMaxIdleMs = 180000;
	private Integer lingerMs = 5;

	public static <K, V> ProducerBillingFactory<K, V> Builder() {
		return new ProducerBillingFactory<K, V>();
	}

	public ProducerBillingFactory<K, V> withMaxRequestSize(
			Integer maxRequestSize) {
		this.maxRequestSize = maxRequestSize;
		return this;
	}

	public ProducerBillingFactory<K, V> withRetries(Integer retries) {
		this.retries = retries;
		return this;
	}

	public ProducerBillingFactory<K, V> withRequestTimeoutMs(
			Integer requestTimeoutMs) {
		this.requestTimeoutMs = requestTimeoutMs;
		return this;
	}

	public ProducerBillingFactory<K, V> withMetadataMaxIdleMs(
			Integer metadataMaxIdleMs) {
		this.metadataMaxIdleMs = metadataMaxIdleMs;
		return this;
	}

	public ProducerBillingFactory<K, V> withLingerMs(Integer lingerMs) {
		this.lingerMs = lingerMs;
		return this;
	}

	private ProducerBillingFactory() {
	}

	public ProducerBillingFactory<K, V> withBootstrapServers(
			String bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
		return this;
	}

	public ProducerBillingFactory<K, V> withClientId(String clientId) {
		this.clientId = clientId;
		return this;
	}

	public ProducerBillingFactory<K, V> withSchemaRegistryUrl(
			String schemaRegistryUrl) {
		this.schemaRegistryUrl = schemaRegistryUrl;
		return this;
	}

	public ProducerBillingFactory<K, V> withSecurityProtocol(
			String securityProtocol) {
		this.securityProtocol = securityProtocol;
		return this;
	}

	public ProducerBillingFactory<K, V> withSaslJaasConfig(
			String saslJaasConfig) {
		this.saslJaasConfig = saslJaasConfig;
		return this;
	}

	public ProducerBillingFactory<K, V> withSaslMechanism(
			String saslMechanism) {
		this.saslMechanism = saslMechanism;
		return this;
	}

	public ProducerBillingFactory<K, V> withSaslLoginCallbackHandlerClass(
			String saslLoginCallbackHandlerClass) {
		this.saslLoginCallbackHandlerClass = saslLoginCallbackHandlerClass;
		return this;
	}

	public ProducerBillingFactory<K, V> withKeySerializer(
			String keySerializer) {
		this.keySerializer = keySerializer;
		return this;
	}

	public ProducerBillingFactory<K, V> withValueSerializer(
			String valueSerializer) {
		this.valueSerializer = valueSerializer;
		return this;
	}

	public KafkaProducerBilling<K, V> build() {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				valueSerializer);

		props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
				securityProtocol);
		props.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
		props.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
		props.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS,
				saslLoginCallbackHandlerClass);

		configSchemaRegistry(props);

		props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSize);
		props.put(ProducerConfig.RETRIES_CONFIG, retries);
		props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
		props.put(ProducerConfig.METADATA_MAX_IDLE_CONFIG, metadataMaxIdleMs);
		props.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
		props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
				getDeliveryTimeoutMs());

		return new KafkaProducerBilling<K, V>(props);
	}

	private Integer getDeliveryTimeoutMs() {
		return (requestTimeoutMs + lingerMs) * retries;
	}

	private void configSchemaRegistry(Properties props) {

		if (Objects.nonNull(valueSubjectNameStrategy)
				&& Objects.nonNull(schemaRegistryUrl)) {

			props.put("value.subject.name.strategy", valueSubjectNameStrategy);
			props.put("auto.register.schemas", "false");
			props.put("schema.registry.url", schemaRegistryUrl);
		}
	}
}
