package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.FutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureCallbackRegistry;
import org.springframework.util.concurrent.SuccessCallback;

/**
 * 目的是为了能够获取到 httpClient, httpRequest 和 httpContext
 *
 * 扩展此类是因为  HttpComponentsAsyncClientHttpRequest 是 final
 *
 *
 * Created by zavakid on 2017-11-05
 */
public class HttpComponentsAsyncClientHttpRequestExt extends AbstractBufferingAsyncClientHttpRequest {

	private final HttpAsyncClient httpClient;

	private final HttpUriRequest httpRequest;

	private final HttpContext httpContext;

	HttpComponentsAsyncClientHttpRequestExt(HttpAsyncClient httpClient, HttpUriRequest httpRequest,
                                            HttpContext httpContext) {
		this.httpClient = httpClient;
		this.httpRequest = httpRequest;
		this.httpContext = httpContext;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.resolve(this.httpRequest.getMethod());
	}

	@Override
	public URI getURI() {
		return this.httpRequest.getURI();
	}

	// ext 改成 public
	public HttpContext getHttpContext() {
		return this.httpContext;
	}

	// ext 新增
	public HttpAsyncClient getHttpClient() {
		return httpClient;
	}

	// ext 新增
	public HttpUriRequest getHttpRequest() {
		return httpRequest;
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] bufferedOutput)
			throws IOException {

		HttpComponentsClientHttpRequest.addHeaders(this.httpRequest, headers);

		if (this.httpRequest instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) this.httpRequest;
			HttpEntity requestEntity = new NByteArrayEntity(bufferedOutput);
			entityEnclosingRequest.setEntity(requestEntity);
		}

		final HttpResponseFutureCallback callback = new HttpResponseFutureCallback();
		final Future<HttpResponse> futureResponse =
				this.httpClient.execute(this.httpRequest, this.httpContext, callback);
		return new ClientHttpResponseFuture(futureResponse, callback);
	}

	private static class HttpResponseFutureCallback implements FutureCallback<HttpResponse> {

		private final ListenableFutureCallbackRegistry<ClientHttpResponse> callbacks =
				new ListenableFutureCallbackRegistry<ClientHttpResponse>();

		public void addCallback(ListenableFutureCallback<? super ClientHttpResponse> callback) {
			this.callbacks.addCallback(callback);
		}

		public void addSuccessCallback(SuccessCallback<? super ClientHttpResponse> callback) {
			this.callbacks.addSuccessCallback(callback);
		}

		public void addFailureCallback(FailureCallback callback) {
			this.callbacks.addFailureCallback(callback);
		}

		@Override
		public void completed(HttpResponse result) {
			this.callbacks.success(new HttpComponentsAsyncClientHttpResponse(result));
		}

		@Override
		public void failed(Exception ex) {
			this.callbacks.failure(ex);
		}

		@Override
		public void cancelled() {
		}
	}

	private static class ClientHttpResponseFuture extends FutureAdapter<ClientHttpResponse, HttpResponse>
			implements ListenableFuture<ClientHttpResponse> {

		private final HttpResponseFutureCallback callback;

		public ClientHttpResponseFuture(Future<HttpResponse> futureResponse, HttpResponseFutureCallback callback) {
			super(futureResponse);
			this.callback = callback;
		}

		@Override
		protected ClientHttpResponse adapt(HttpResponse response) {
			return new HttpComponentsAsyncClientHttpResponse(response);
		}

		@Override
		public void addCallback(ListenableFutureCallback<? super ClientHttpResponse> callback) {
			this.callback.addCallback(callback);
		}

		@Override
		public void addCallback(SuccessCallback<? super ClientHttpResponse> successCallback,
				FailureCallback failureCallback) {
			this.callback.addSuccessCallback(successCallback);
			this.callback.addFailureCallback(failureCallback);
		}
	}
}
