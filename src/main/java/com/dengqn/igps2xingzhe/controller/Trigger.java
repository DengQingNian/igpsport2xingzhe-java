package com.dengqn.igps2xingzhe.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.AsymmetricAlgorithm;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.json.JSONUtil;
import com.dengqn.igps2xingzhe.config.IGPSport;
import com.dengqn.igps2xingzhe.config.XingZhe;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.security.SecurityUtil;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * 同步
 *
 * @author dengqn
 * @since 2024/7/20 11:21
 */
@Slf4j
@RestController
@RequestMapping("/api/trigger")
public class Trigger {

	@Autowired
	private HttpClient httpClient;
	@Autowired
	private CookieStore cookieStore;


	@Autowired
	private XingZhe xingZhe;
	@Autowired
	private IGPSport igpSport;

	@GetMapping("/sync/igps2xingzhe")
	public ResponseEntity<String> onSyncIGPS2XingZhe() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		// 1. 登录行者
//		xingzheLogin();
		// 2. 登录igps
		igpsLogin();

		return ResponseEntity.ok("ok");
	}

	private void igpsLogin() throws IOException {
		// 3. igps 登录 formData
		UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(CollUtil.toList(
				new BasicNameValuePair("username", igpSport.getUsername()),
				new BasicNameValuePair("password", igpSport.getPassword())
		));

		ClassicRequestBuilder igpsLoginReq = ClassicRequestBuilder
				.post("https://my.igpsport.com/Auth/Login")
				.setEntity(formEntity);

		CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(igpsLoginReq.build());
		log.info("igps login: {}", IoUtil.read(response.getEntity().getContent()));
	}

	private void xingzheLogin() throws IOException {
		// 1、获取行者平台的cookie
		HttpResponse executed = httpClient.execute(ClassicRequestBuilder.get("https://www.imxingzhe.com/user/login").build());
		System.out.println(executed.getCode());
		// 2、计算加密后的密码参数
		RSA rsa = new RSA(AsymmetricAlgorithm.RSA_ECB_PKCS1.getValue(), null, xingZhe.getPubKey());
		String rd = cookieStore.getCookies()
				.stream().filter(a -> a.getName().equals("rd")).findFirst()
				.map(Cookie::getValue).orElse("");
		String encryptBase64 = rsa.encryptBase64(xingZhe.getPassword() + ";" + rd, KeyType.PublicKey);

		Map<String, Object> loginData = new HashMap<>();
		loginData.put("account", xingZhe.getUsername());
		loginData.put("password", encryptBase64);
		loginData.put("source", "web");

		CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(ClassicRequestBuilder
				.post("https://www.imxingzhe.com/api/v4/account/login")
				.setEntity(JSONUtil.toJsonPrettyStr(loginData), ContentType.APPLICATION_JSON)
				.build());
		log.info("xingzhe login: {}", IoUtil.read(response.getEntity().getContent()));
	}


}
