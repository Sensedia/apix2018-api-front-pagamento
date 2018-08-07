package com.sensedia.service.impl;

import com.sensedia.exception.PostDebitosFailedException;
import com.sensedia.service.PagamentosAPIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.sensedia.app.RestTemplateConfiguration;
import com.sensedia.vo.DebitosInput;

@Service
public class PagamentosAPIServiceImpl implements PagamentosAPIService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PagamentosAPIServiceImpl.class);

	private static final String CLIENT_ID = "clientId";

	private RestTemplate restTemplate;

	@Value("${rest.gw.clientId:}")
	private String clientId;

	@Value("${rest.gw.pagamentos.endpoint:}")
	private String endpoint;


	@Autowired
	public PagamentosAPIServiceImpl(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		RestTemplateConfiguration.setDefaultConfig(restTemplate);
	}

	/**
	 * Building the request header with authentication values
	 * 
	 * @return a {@link HttpHeaders} with the header
	 */
	private HttpHeaders gwHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(CLIENT_ID, clientId);
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	@Override
	public void postDebitos(DebitosInput input) throws PostDebitosFailedException {
		
		ResponseEntity<Void> responseEntity = null;
		
		try {
			responseEntity = restTemplate.exchange(endpoint,
					HttpMethod.POST,
					new HttpEntity<>(input, gwHeaders()),
					Void.class);
			
			if (responseEntity == null) {
				throw new PostDebitosFailedException("Falha ao executar post debito.");
			}
			
		} catch (HttpClientErrorException | HttpServerErrorException ex) {
			LOGGER.error(ex.getMessage(), ex);
			throw new PostDebitosFailedException(ex);
		}
		
	}

}