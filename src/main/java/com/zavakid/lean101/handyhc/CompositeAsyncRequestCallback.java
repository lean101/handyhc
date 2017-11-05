package com.zavakid.lean101.handyhc;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.web.client.AsyncRequestCallback;

/**
 * Created by zavakid on 2017-11-05
 */
public class CompositeAsyncRequestCallback implements AsyncRequestCallback {

	private List<AsyncRequestCallback> callbacks = new LinkedList<>();

	public static CompositeAsyncRequestCallback create() {
		return new CompositeAsyncRequestCallback();
	}

	@Override
	public void doWithRequest(AsyncClientHttpRequest request) throws IOException {

		for (AsyncRequestCallback callback : callbacks) {
			callback.doWithRequest(request);
		}
	}

	public CompositeAsyncRequestCallback add(AsyncRequestCallback callback) {
		if (callback != null) {
			callbacks.add(callback);
		}

		return this;
	}

}
