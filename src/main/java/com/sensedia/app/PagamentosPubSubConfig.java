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

import com.sensedia.vo.SolicitacaoPagamentoInput;

@Configuration
public class PagamentosPubSubConfig {

	private static Logger LOGGER = LoggerFactory.getLogger(PagamentosPubSubConfig.class);
	
	@Value("${pubsub.pagamentos.topic:}")
	private String topic;
	
	@Value("${pubsub.pagamentos.subscriptionId:}")
	private String subscriptionId;
	
	@Autowired
	private PagamentoService pagamentoService;

	@Autowired
	private Gson gson;
	@Bean
	public MessageChannel pagamentosChannel() {
		return new DirectChannel();
	}

	//PAGAMENTOS
	@Bean
	public PubSubInboundChannelAdapter messageChannelAdapterPagamentos(
			@Qualifier("pagamentosInputChannel") MessageChannel inputChannel, PubSubOperations pubSubTemplate) {
		PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate,
				subscriptionId/*"pagamentos-sub"*/);
		adapter.setOutputChannel(inputChannel);
		adapter.setAckMode(AckMode.MANUAL);
		return adapter;
	}

	@Bean
	@ServiceActivator(inputChannel = "pagamentosInputChannel")
	public MessageHandler messageReceiverPagamentos() {
		return message -> {
			LOGGER.info(String.format("Message arrived! Payload: %s",message.getPayload()));
			AckReplyConsumer consumer = (AckReplyConsumer) message.getHeaders().get(GcpPubSubHeaders.ACKNOWLEDGEMENT);
			consumer.ack();
			pagamentoService.processarValidador(gson.fromJson(message.getPayload().toString(), SolicitacaoPagamentoInput.class));
		};
	}

	@Bean
	@ServiceActivator(inputChannel = "pagamentosOutputChannel")
	public MessageHandler messageSenderPagamentos(PubSubOperations pubsubTemplate) {
		return new PubSubMessageHandler(pubsubTemplate, topic/*"pagamentos-out"*/);
	}

	@MessagingGateway(defaultRequestChannel = "pagamentosOutputChannel")
	public interface PagamentosPubSubOutboundGateway {
		void sendToPubsub(String text);
	}
}