package com.sensedia.service;

import com.sensedia.vo.SolicitacaoPagamentoInput;
import com.sensedia.vo.SolicitacaoPagamentoOutput;

public interface PagamentoService {

	void processarValidador(SolicitacaoPagamentoInput solicitacao);
	void processarAntifraude(SolicitacaoPagamentoOutput solicitacao);
	void processarBoletos(SolicitacaoPagamentoOutput solicitacao);
	void processarPagamento(SolicitacaoPagamentoOutput solicitacao);
	void processarNotificacao(SolicitacaoPagamentoOutput solicitacao);

}
