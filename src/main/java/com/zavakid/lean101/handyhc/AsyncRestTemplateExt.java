package com.zavakid.lean101.handyhc;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRequestCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

/**
 *  扩展 AsyncRestTemplate, 支持 asyncHttpClient 的一些 callback
 *
 * Created by zavakid on 2017-11-05
 */
public class AsyncRestTemplateExt extends AsyncRestTemplate {

	public AsyncRestTemplateExt() {
	}

	public AsyncRestTemplateExt(AsyncListenableTaskExecutor taskExecutor) {
		super(taskExecutor);
	}

	public AsyncRestTemplateExt(AsyncClientHttpRequestFactory asyncRequestFactory) {
		super(asyncRequestFactory);
	}

	public AsyncRestTemplateExt(AsyncClientHttpRequestFactory asyncRequestFactory,
                                ClientHttpRequestFactory syncRequestFactory) {
		super(asyncRequestFactory, syncRequestFactory);
	}

	public AsyncRestTemplateExt(AsyncClientHttpRequestFactory requestFactory,
                                RestTemplate restTemplate) {
		super(requestFactory, restTemplate);
	}

	public <T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
                                                                AsyncHcGetRequestCallback getRequestCallback,
                                                                Object... uriVariables) {

		AsyncRequestCallback acceptHeaderRequestCallback = acceptHeaderRequestCallback(responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);

		AsyncRequestCallback requestCallback = acceptHeaderRequestCallback;
		if (getRequestCallback != null) {
			requestCallback = CompositeAsyncRequestCallback.create().add(acceptHeaderRequestCallback)
					.add(getRequestCallback.adapt());
		}

		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
	}

}
