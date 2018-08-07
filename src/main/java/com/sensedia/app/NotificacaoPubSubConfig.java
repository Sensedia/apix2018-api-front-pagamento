package com.sensedia.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.pubsub.core.PubSubOperations;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import com.google.cloud.pubsub.v1.AckReplyConsumer;

@Configuration
public class NotificacaoPubSubConfig {

	Logger LOGGER = LoggerFactory.getLogger(NotificacaoPubSubConfig.class);
	
	@Value("${pubsub.notificacao.topic:}")
	private String topic;
	
	@Value("${pubsub.notificacao.subscriptionId:}")
	private String subscriptionId;
	
	@Bean
	public MessageChannel pagamentosChannel() {
		return new DirectChannel();
	}

	@Bean
	public PubSubInboundChannelAdapter messageChannelAdapterNotificacao(
			@Qualifier("notificacaoInputChannel") MessageChannel inputChannel, PubSubOperations pubSubTemplate) {
		PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate,
				subscriptionId/*"pagamentos-notificacoes-out-sub"*/);
		adapter.setOutputChannel(inputChannel);
		adapter.setAckMode(AckMode.MANUAL);
		return adapter;
	}

	@Bean
	@ServiceActivator(inputChannel = "notificacaoInputChannel")
	public MessageHandler messageReceiverNotificacao() {
		return message -> {
			LOGGER.info(String.format("Message arrived! Payload: %s",message.getPayload()));
			AckReplyConsumer consumer = (AckReplyConsumer) message.getHeaders().get(GcpPubSubHeaders.ACKNOWLEDGEMENT);
			consumer.ack();
		};
	}

	@Bean
	@ServiceActivator(inputChannel = "notificacaoOutputChannel")
	public MessageHandler messageSenderNotificacao(PubSubOperations pubsubTemplate) {
		return new PubSubMessageHandler(pubsubTemplate, topic/*"notificacoes-in"*/);
	}

	@MessagingGateway(defaultRequestChannel = "notificacaoOutputChannel")
	public interface NotificacaoPubSubOutboundGateway {
		void sendToPubsub(String input);
	}

}