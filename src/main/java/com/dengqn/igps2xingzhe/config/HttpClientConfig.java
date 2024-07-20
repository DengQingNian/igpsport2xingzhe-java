package com.dengqn.igps2xingzhe.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.MinimalHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author dengqn
 * @since 2024/7/20 15:22
 */
@Component
public class HttpClientConfig {
	@Autowired
	private IGPSport iGPSport;
	@Autowired
	private XingZhe xingZhe;

	@Bean
	public CookieStore cookieStore() {
		return new BasicCookieStore();
	}


	@Bean
	public HttpClient httpClient(CookieStore cookieStore) {
		return HttpClientBuilder.create()
				.setDefaultCookieStore(cookieStore)
				.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
				.build();
	}


}
