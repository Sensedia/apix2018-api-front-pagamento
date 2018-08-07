package com.sensedia.app;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;


/**
 * Configuration Class for RestTemplate
 * @author jpavin
 *
 */
public class RestTemplateConfiguration {
	
	/**
	 * Default timeout.
	 */
	private static Integer defaultTimeout = 30000;
	
	/**
	 * Default max connection pool.
	 */
	private static Integer defaultMaxConnPool = 30000;
	
	private RestTemplateConfiguration() {
	}
	
	/**
	 * Method that apply default configuration for proxy, timeout and max connection on rest template.
	 * @param restClient
	 */
	public static void setDefaultConfig(RestTemplate restClient){
		

		HttpClientBuilder builder = HttpClientBuilder.create()
				.useSystemProperties()
				.setMaxConnPerRoute(defaultMaxConnPool)
				.setMaxConnTotal(defaultMaxConnPool);

		HttpClient httpClient = builder.build();
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		factory.setHttpClient(httpClient);
	    restClient.setRequestFactory(factory);
	    restClient.setRequestFactory(new BufferingClientHttpRequestFactory(factory));
		
		 
	    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add(new LoggingRequestInterceptor());
		restClient.setInterceptors(interceptors);
	    
	}
	
	public static void setDefaultTimeout(RestTemplate restClient) {
		setTimeout(defaultTimeout, defaultTimeout, defaultTimeout, restClient);
	}
	
	/**
	 * Method that apply timeout configuration.
	 * 
	 * @param requestTimeout 
	 * @param connectTimeout 
	 * @param readTimeout 
	 * @param restClient
	 */
	public static void setTimeout(Integer requestTimeout, Integer connectTimeout, Integer readTimeout, RestTemplate restClient) {
		
		HttpComponentsClientHttpRequestFactory factory = (HttpComponentsClientHttpRequestFactory) restClient.getRequestFactory();
		
		//set the timeout to waiting an available connection from the pool 
	    factory.setConnectionRequestTimeout(requestTimeout);
	    //set the timeout to connect
	    factory.setConnectTimeout(connectTimeout);
	    //set the timeout to receive the response
	    factory.setReadTimeout(readTimeout);
		
		restClient.setRequestFactory(new BufferingClientHttpRequestFactory(factory));
		 
	    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add(new LoggingRequestInterceptor());
		restClient.setInterceptors(interceptors);
	}
}