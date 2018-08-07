package com.sensedia.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.sensedia.app.AntifraudePubSubConfig.AntifraudePubSubOutboundGateway;
import com.sensedia.app.BoletosPubSubConfig.BoletosPubSubOutboundGateway;
import com.sensedia.app.NotificacaoPubSubConfig.NotificacaoPubSubOutboundGateway;
import com.sensedia.app.PagamentosPubSubConfig.PagamentosPubSubOutboundGateway;
import com.sensedia.app.ValidadorPubSubConfig.ValidadorPubSubOutboundGateway;
import com.sensedia.service.PagamentoService;
import com.sensedia.service.PagamentosAPIService;
import com.sensedia.vo.DebitosInput;
import com.sensedia.vo.SolicitacaoPagamentoDetalheOutput;
import com.sensedia.vo.SolicitacaoPagamentoInput;
import com.sensedia.vo.SolicitacaoPagamentoOutput;
import com.sensedia.vo.StatusRetornoEnum;

@Service
public class PagamentoServiceImpl implements PagamentoService {
	
	private static Logger LOGGER = LoggerFactory.getLogger(PagamentoServiceImpl.class);
	
	private final Gson gson;

	private static HashMap<Long, SolicitacaoPagamentoInput> solicitacoes = new HashMap<>();

	private final PagamentosPubSubOutboundGateway pagamentosPubSubOutboundGateway;

	private final ValidadorPubSubOutboundGateway validadorPubSubOutboundGateway;

	private final AntifraudePubSubOutboundGateway antifraudePubSubOutboundGateway;

	private final BoletosPubSubOutboundGateway boletosPubSubOutboundGateway;

	private final NotificacaoPubSubOutboundGateway notificacaoPubSubOutboundGateway;
	
	private final PagamentosAPIService pagamentosAPIService;

	@Autowired
	public PagamentoServiceImpl(PagamentosPubSubOutboundGateway pagamentosPubSubOutboundGateway,
			ValidadorPubSubOutboundGateway validadorPubSubOutboundGateway,
			AntifraudePubSubOutboundGateway antifraudePubSubOutboundGateway, 
			BoletosPubSubOutboundGateway boletosPubSubOutboundGateway, 			
			NotificacaoPubSubOutboundGateway notificacaoPubSubOutboundGateway, 
			Gson gson,
			PagamentosAPIService pagamentoAPIService) {
		this.pagamentosPubSubOutboundGateway = pagamentosPubSubOutboundGateway;
		this.validadorPubSubOutboundGateway = validadorPubSubOutboundGateway;
		this.antifraudePubSubOutboundGateway = antifraudePubSubOutboundGateway;
		this.boletosPubSubOutboundGateway = boletosPubSubOutboundGateway;
		this.notificacaoPubSubOutboundGateway = notificacaoPubSubOutboundGateway;
		this.gson = gson;
		this.pagamentosAPIService = pagamentoAPIService;
	}

	private void addDetalhesToSolicitacao(SolicitacaoPagamentoInput input, List<SolicitacaoPagamentoDetalheOutput> detalhes) {
		detalhes.forEach(retorno -> {
			input.addDetalhes(retorno);
		});
		solicitacoes.put(input.getId(), input);
	}
	
	private void addDetalheToSolicitacao(SolicitacaoPagamentoInput input, SolicitacaoPagamentoDetalheOutput detalhe) {
		input.addDetalhes(detalhe);
		solicitacoes.put(input.getId(), input);
	}
	
	@Override
	public void processarValidador(SolicitacaoPagamentoInput solicitacao) {
		LOGGER.info(String.format("[SOLICITACAO: {}]: iniciando processarValidador", solicitacao.getId()));
		validadorPubSubOutboundGateway.sendToPubsub(gson.toJson(solicitacao));
		solicitacoes.put(solicitacao.getId(), solicitacao);
	}

	@Override
	public void processarAntifraude(SolicitacaoPagamentoOutput solicitacao) {
		LOGGER.info(String.format("[SOLICITACAO: {}]: iniciando processarAntifraude", solicitacao.getId()));
		SolicitacaoPagamentoInput input = solicitacoes.get(solicitacao.getId());
		if (input != null) {
			addDetalhesToSolicitacao(input, new ArrayList<>(solicitacao.getRetornos()));
			antifraudePubSubOutboundGateway.sendToPubsub(gson.toJson(input));
		} else {
			LOGGER.info(String.format("[SOLICITACAO: {}]: Não encontrada.", solicitacao.getId()));
		}
		
	}

	@Override
	public void processarBoletos(SolicitacaoPagamentoOutput solicitacao) {
		LOGGER.info(String.format("[SOLICITACAO: {}]: iniciando processarBoletos", solicitacao.getId()));
		SolicitacaoPagamentoInput input = solicitacoes.get(solicitacao.getId());
		if (input != null) {
			addDetalhesToSolicitacao(input, new ArrayList<>(solicitacao.getRetornos()));
			boletosPubSubOutboundGateway.sendToPubsub(gson.toJson(input));
		} else {
			LOGGER.info(String.format("[SOLICITACAO: {}]: Não encontrada.", solicitacao.getId()));
		}
	}

	@Override
	public void processarPagamento(SolicitacaoPagamentoOutput solicitacao) {
		LOGGER.info(String.format("[SOLICITACAO: {}]: iniciando processarPagamento", solicitacao.getId()));
		SolicitacaoPagamentoInput input = solicitacoes.get(solicitacao.getId());
		if (input != null) {
			addDetalhesToSolicitacao(input, new ArrayList<>(solicitacao.getRetornos()));
			
			if (input.isValid()) {
				try {
					pagamentosAPIService.postDebitos(new DebitosInput(input.getUsuarioCPF(), input.getValor()));
				} catch (Exception e) {
					LOGGER.error(String.format("[SOLICITACAO: {}]: {}", solicitacao.getId(), e.getMessage()), e);
					addDetalheToSolicitacao(input, new SolicitacaoPagamentoDetalheOutput(StatusRetornoEnum.ERROR, "Falha ao efetuar o post pagamentos."));
				}
			}
			pagamentosPubSubOutboundGateway.sendToPubsub(gson.toJson(input));
			notificacaoPubSubOutboundGateway.sendToPubsub(gson.toJson(input));
		} else {
			LOGGER.info(String.format("[SOLICITACAO: {}]: Não encontrada.", solicitacao.getId()));
		}
	}

	@Override
	public void processarNotificacao(SolicitacaoPagamentoOutput solicitacao) {
		LOGGER.info(String.format("[SOLICITACAO: {}]: iniciando processarNotificacao", solicitacao.getId()));
		SolicitacaoPagamentoInput input = solicitacoes.get(solicitacao.getId());
		if (input != null) {
			addDetalhesToSolicitacao(input, new ArrayList<>(solicitacao.getRetornos()));
			pagamentosPubSubOutboundGateway.sendToPubsub(gson.toJson(input));
		} else {
			LOGGER.info(String.format("[SOLICITACAO: {}]: Não encontrada.", solicitacao.getId()));
		}
	}

}
