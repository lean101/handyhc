package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpMethod;

/**
 * 配合生产 {@link HttpComponentsAsyncClientHttpRequestExt} 的 Factory
 *
 * 更多信息见 {@link HttpComponentsAsyncClientHttpRequestExt}
 *
 * Created by zavakid on 2017-11-05
 */
public class HttpComponentsAsyncClientHttpRequestExtFactory extends HttpComponentsAsyncClientHttpRequestFactory {

	public HttpComponentsAsyncClientHttpRequestExtFactory() {
	}

	public HttpComponentsAsyncClientHttpRequestExtFactory(
			CloseableHttpAsyncClient httpAsyncClient) {
		super(httpAsyncClient);
	}

	public HttpComponentsAsyncClientHttpRequestExtFactory(CloseableHttpClient httpClient,
														  CloseableHttpAsyncClient httpAsyncClient) {
		super(httpClient, httpAsyncClient);
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpAsyncClient asyncClient = getHttpAsyncClient();
		startAsyncClient();
		HttpUriRequest httpRequest = createHttpUriRequest(httpMethod, uri);
		postProcessHttpRequest(httpRequest);
		HttpContext context = createHttpContext(httpMethod, uri);
		if (context == null) {
			context = HttpClientContext.create();
		}
//		 Request configuration not set in the context
//		 修改,不进行 config 的设置
		if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
			// Use request configuration given by the user, when available
			RequestConfig config = null;
			if (httpRequest instanceof Configurable) {
				config = ((Configurable) httpRequest).getConfig();
			}
			if (config == null) {
				config = createRequestConfig(asyncClient);
			}
			if (config != null) {
				context.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
			}
		}
		return new HttpComponentsAsyncClientHttpRequestExt(asyncClient, httpRequest, context);
	}

	private void startAsyncClient() {
		CloseableHttpAsyncClient asyncClient = getHttpAsyncClient();
		if (!asyncClient.isRunning()) {
			asyncClient.start();
		}
	}
}
