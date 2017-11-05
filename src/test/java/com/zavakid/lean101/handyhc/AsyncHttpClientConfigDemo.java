package com.zavakid.lean101.handyhc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.util.PublicSuffixMatcher;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.ssl.SSLContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestExtFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * a demo config for asynchttpclient for product environment
 *
 * Created by zavakid on 2017-11-05
 */
@Configuration
public class AsyncHttpClientConfigDemo {

    private static final int SO_TIMEOUT = 8000;
    private static final int MAX_CONN_TOTAL = 5096;
    private static final int MAX_CONN_PER_ROUTE = 10;

    public static RequestConfig DEFAULT_CONFIG = RequestConfig.custom()
        .setConnectionRequestTimeout(-1) //从 conn manager 获取 connection 的超时时间
        .setAuthenticationEnabled(true)
        .setCircularRedirectsAllowed(false) // 禁止循环跳转
        .setConnectTimeout(SO_TIMEOUT) // 建立连接的时间
        .setContentCompressionEnabled(true) // 自动解压
        .setCookieSpec(null) //详见 org.apache.http.client.config.RequestConfig.getCookieSpec()
        .setExpectContinueEnabled(false) // 不使用 http state 100
        .setLocalAddress(null) // 系统自动选择
        .setMaxRedirects(4) // 最大跳转次数
        .setRedirectsEnabled(true)
        .setRelativeRedirectsAllowed(true) // 支持转跳返回相关路径
        .setSocketTimeout(SO_TIMEOUT) // read timeout
        .build();

    // 以下都是为了设置 poolingmgr 才进行 set 的，目的是为了修改
    // poolingmgr 创建 CPool 时能够修改 TTL
    // 详见 #5 ( https://git.oschina.net/coobobo/coobobo/issues/5 )
    private static final IOReactorConfig DEFAULT_IO_REACTOR_CONFIG = IOReactorConfig.custom()
        .setSoTimeout(SO_TIMEOUT)
        .setConnectTimeout(2 * SO_TIMEOUT)
        .setIoThreadCount(Runtime.getRuntime().availableProcessors())
        .build();
    private static final PublicSuffixMatcher PUBLIC_SUFFIX_MATCHER = PublicSuffixMatcherLoader.getDefault();
    private static final DefaultHostnameVerifier HOSTNAME_VERIFIER = new DefaultHostnameVerifier(PUBLIC_SUFFIX_MATCHER);
    private static final SSLContext SSL_CONTEXT = SSLContexts.createDefault();
    private static final SchemeIOSessionStrategy SSL_STRATEGY = new SSLIOSessionStrategy(
        SSL_CONTEXT, null, null, HOSTNAME_VERIFIER);

    @Bean(initMethod = "start", destroyMethod = "close")
    public CloseableHttpAsyncClient asyncHttpClient() throws Exception {

        List<Header> defaultHeaders = new ArrayList<>();

        defaultHeaders.add(
            new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"));
        defaultHeaders.add(new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6,id;q=0.4,zh-TW;q=0.2"));
        defaultHeaders.add(new BasicHeader("Cache-Control", "max-age=0"));
        defaultHeaders.add(new BasicHeader("Connection", "keep-alive"));

        final PoolingNHttpClientConnectionManager poolingmgr = poolingNHttpClientConnectionManager();

        final CloseableHttpAsyncClient asyncClient = HttpAsyncClients.custom()
            .setDefaultRequestConfig(DEFAULT_CONFIG) // request 没有设置的时候才使用此 config
            .setConnectionManagerShared(false) //不用在多个 client 中共享,系统保证只有一个 client
            .setDefaultIOReactorConfig(DEFAULT_IO_REACTOR_CONFIG)
            .setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
            .setMaxConnTotal(MAX_CONN_TOTAL)
            .setThreadFactory(new CustomizableThreadFactory("async-http-"))
            .setUserAgent(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/49.0.2623.87 Safari/537.36")
            .setDefaultHeaders(defaultHeaders)

            // 以下都是为了设置 poolingmgr 才进行 set 的，目的是为了修改
            // poolingmgr 创建 CPool 时能够修改 TTL
            // 详见 #5 ( https://git.oschina.net/coobobo/coobobo/issues/5 )
            .setPublicSuffixMatcher(PUBLIC_SUFFIX_MATCHER)
            .setSSLHostnameVerifier(HOSTNAME_VERIFIER)
            .setSSLContext(SSL_CONTEXT)
            .setSSLStrategy(SSL_STRATEGY)
            .setConnectionManager(poolingmgr)
            .build();

        return asyncClient;
    }

    // 详见 #5 ( https://git.oschina.net/coobobo/coobobo/issues/5 )
    @Bean
    public PoolingNHttpClientConnectionManager poolingNHttpClientConnectionManager() throws IOReactorException {
        final DefaultConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(
            DEFAULT_IO_REACTOR_CONFIG);
        final Registry<SchemeIOSessionStrategy> iosessionFactoryRegistry
            = RegistryBuilder.<SchemeIOSessionStrategy>create()
            .register("http", NoopIOSessionStrategy.INSTANCE)
            .register("https", SSL_STRATEGY)
            .build();

        // 重点在这里：就是为了设置 timeToLive 为一小时
        final PoolingNHttpClientConnectionManager poolingmgr = new PoolingNHttpClientConnectionManager(
            ioReactor, null, iosessionFactoryRegistry, null, null, 1000 * 3600, TimeUnit.MILLISECONDS);
        poolingmgr.setMaxTotal(MAX_CONN_TOTAL);
        poolingmgr.setDefaultMaxPerRoute(MAX_CONN_PER_ROUTE);
        return poolingmgr;
    }

    @Bean
    public AsyncClientHttpRequestFactory asyncClientHttpRequestFactory() throws Exception {
        return new HttpComponentsAsyncClientHttpRequestExtFactory(asyncHttpClient());
    }

    @Bean
    public AsyncRestTemplateExt asyncRestTemplate() throws Exception {
        return new AsyncRestTemplateExt(asyncClientHttpRequestFactory());
    }
}
