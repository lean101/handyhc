package com.zavakid.lean101.handyhc;

import java.util.Optional;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import static org.assertj.core.api.Assertions.*;

/**
 * @author zavakid
 */
public class CallbackTest {

    @Test
    public void testRequestCallback() throws Exception {

        AnnotationConfigApplicationContext application = new AnnotationConfigApplicationContext(
            AsyncHttpClientConfigDemo.class);

        AsyncRestTemplateExt asyncRestTemplateExt = application.getBean(AsyncRestTemplateExt.class);

        ListenableFuture<ResponseEntity<String>> entity = asyncRestTemplateExt.getForEntity("http://httpbin.org/get",
            String.class,
            (request, context) -> request.addHeader("addHeader", "dummy"));

        ResponseEntity<String> responseEntity = entity.get();
        String body = responseEntity.getBody();

        // should parse json for precise assert
        assertThat(body).contains("\"Addheader\": \"dummy\"");

    }
}
