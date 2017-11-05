package com.zavakid.lean101.handyhc;

import java.io.IOException;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestExt;
import org.springframework.web.client.AsyncRequestCallback;

/**
 * 处理 async http get 的 callback
 *
 * Created by zavakid on 2017-11-05
 */
public interface AsyncHcGetRequestCallback {

	void doWithRequest(final HttpGet request, final HttpContext context);

	// ===== helper ====
	default AsyncRequestCallback adapt() {
		return new AsyncHcGetRequestCallbackAdapter(this);
	}

	// ============
	class AsyncHcGetRequestCallbackAdapter implements AsyncRequestCallback {

		private final AsyncHcGetRequestCallback adaptee;

		AsyncHcGetRequestCallbackAdapter(AsyncHcGetRequestCallback adaptee) {
			this.adaptee = adaptee;
		}

		@Override
		public void doWithRequest(AsyncClientHttpRequest request) throws IOException {
			if (!(request instanceof HttpComponentsAsyncClientHttpRequestExt)) {
				return;
			}

			HttpComponentsAsyncClientHttpRequestExt hcRequest = (HttpComponentsAsyncClientHttpRequestExt) request;

			final HttpUriRequest httpRequest = hcRequest.getHttpRequest();
			if (!(httpRequest instanceof HttpGet)) {
				return;
			}

			final HttpGet get = (HttpGet) httpRequest;
			final HttpContext httpContext = hcRequest.getHttpContext();

			adaptee.doWithRequest(get, httpContext);

		}
	}

}
