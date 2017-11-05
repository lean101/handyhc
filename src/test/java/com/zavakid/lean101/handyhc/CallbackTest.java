package com.zavakid.lean101.handyhc;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author zavakid
 */
public class CallbackTest {

    private static AnnotationConfigApplicationContext application = null;

    @BeforeClass
    public static void setup() {
        application = new AnnotationConfigApplicationContext(AsyncHttpClientConfigDemo.class);
    }

    @AfterClass
    public static void down() {
        if (application != null) {
            application.close();
        }
    }

    @Test
    public void testRequestCallback() throws Exception {

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
