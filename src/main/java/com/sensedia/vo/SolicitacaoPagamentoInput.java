package com.sensedia.vo;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.cloud.Timestamp;
import com.google.type.LatLng;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolicitacaoPagamentoInput implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Long id;
	private final Double valor;
	private final StatusRetornoEnum status;
	private final Timestamp dataCriacao;
	private final LatLng geolocalizacao;
	private final String numeroCartao;
	private final String nomeCartao;
	private final String validadeCartao;
	private final String cvvCartao;
	private final String codigoPagamento;
	private final String usuarioCPF;
	private final String usuarioNome;
	private final String usuarioTelefone;
	private final String usuarioEmail;
	private List<SolicitacaoPagamentoDetalheOutput> detalhes;

	public void addDetalhes(SolicitacaoPagamentoDetalheOutput detalhe) {
		if (detalhes == null) {
			detalhes = new ArrayList<>();
		}
		detalhes.add(detalhe);
	}
	
	public Boolean isValid() {
		Long count = detalhes.stream().filter(detalhe -> detalhe.getStatus().equals(StatusRetornoEnum.ERROR) || detalhe.getStatus().equals(StatusRetornoEnum.FRAUDE)).count();
		return count == 0;
	}
	
}