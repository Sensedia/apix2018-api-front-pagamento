package com.sensedia.app;

import com.sensedia.service.PagamentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.google.gson.Gson;

import com.sensedia.vo.SolicitacaoPagamentoOutput;

@Configuration
public class BoletosPubSubConfig {

	private static Logger LOGGER = LoggerFactory.getLogger(BoletosPubSubConfig.class);

	@Autowired
	private PagamentoService pagamentoService;

	@Autowired
	private Gson gson;
	
	@Value("${pubsub.boletos.topic:}")
	private String topic;
	
	@Value("${pubsub.boletos.subscriptionId:}")
	private String subscriptionId;
	
	@Bean
	public MessageChannel boletosChannel() {
		return new DirectChannel();
	}

	@Bean
	public PubSubInboundChannelAdapter messageChannelAdapterBoletos(
			@Qualifier("boletosInputChannel") MessageChannel inputChannel, PubSubOperations pubSubTemplate) {
		PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate,
				subscriptionId/*"pagamentos-boletos-out-sub"*/);
		adapter.setOutputChannel(inputChannel);
		adapter.setAckMode(AckMode.MANUAL);
		return adapter;
	}

	@Bean
	@ServiceActivator(inputChannel = "boletosInputChannel")
	public MessageHandler messageReceiverBoletos() {
		return message -> {
			LOGGER.info(String.format("Message arrived! Payload: %s",message.getPayload()));
			AckReplyConsumer consumer = (AckReplyConsumer) message.getHeaders().get(GcpPubSubHeaders.ACKNOWLEDGEMENT);
			consumer.ack();
			pagamentoService.processarPagamento(gson.fromJson(message.getPayload().toString(), SolicitacaoPagamentoOutput.class));
		};
	}

	@Bean
	@ServiceActivator(inputChannel = "boletosOutputChannel")
	public MessageHandler messageSenderBoletos(PubSubOperations pubsubTemplate) {
		return new PubSubMessageHandler(pubsubTemplate, topic/*"boletos-in"*/);
	}

	@MessagingGateway(defaultRequestChannel = "boletosOutputChannel")
	public interface BoletosPubSubOutboundGateway {
		void sendToPubsub(String input);
	}

}