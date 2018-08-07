package com.sensedia.vo;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolicitacaoPagamentoDetalheOutput implements Serializable {

	private static final long serialVersionUID = 1L;

	private final StatusRetornoEnum status;
	private final String mensagem;

}