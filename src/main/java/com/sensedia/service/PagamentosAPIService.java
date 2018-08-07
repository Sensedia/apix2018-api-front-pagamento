package com.sensedia.service;

import com.sensedia.exception.PostDebitosFailedException;
import com.sensedia.vo.DebitosInput;

public interface PagamentosAPIService {
	
	public void postDebitos(DebitosInput input) throws PostDebitosFailedException;

}
