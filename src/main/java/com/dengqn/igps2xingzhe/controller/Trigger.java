package com.dengqn.igps2xingzhe.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.AsymmetricAlgorithm;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.dengqn.igps2xingzhe.config.Giant;
import com.dengqn.igps2xingzhe.config.IGPSport;
import com.dengqn.igps2xingzhe.config.XingZhe;
import com.dengqn.igps2xingzhe.vo.IGPSActivity;
import com.dengqn.igps2xingzhe.vo.IGPSActivityResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.security.SecurityUtil;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    @Value("${dataDir:/data}")
    private String fileDir;

    @Autowired
    private Giant giant;
    @Autowired
    private XingZhe xingZhe;
    @Autowired
    private IGPSport igpSport;

    private static final String GIANT_TOKEN_KEY = "GIANT_TOKEN_KEY";
    private static final Map<String, Object> contextCache = new ConcurrentHashMap<>(4);

    @GetMapping("/sync/igps2xingzhe")
    public ResponseEntity<String> onSyncIGPS2XingZhe() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // 1. 登录行者
        xingzheLogin();
        // 2. 登录igps
        igpsLogin();
        // 3. 登录捷安特
        giantLogin();
        // 3. 获取igps活动
        syncIgpsActicities();

        return ResponseEntity.ok("ok");
    }

    private void giantLogin() throws IOException {
        if (!giant.getEnable()) {
            return;
        }
        // clear cookie
        httpClient.execute(ClassicRequestBuilder.get("https://ridelife.giant.com.cn/web/login.html/index.php/api/login").build());
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(CollUtil.toList(
                new BasicNameValuePair("username", giant.getUsername()),
                new BasicNameValuePair("password", giant.getPassword())
        ));
        ClassicRequestBuilder igpsLoginReq = ClassicRequestBuilder
                .post("https://ridelife.giant.com.cn/index.php/api/login")
                .setEntity(formEntity);
        CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(igpsLoginReq.build());
        String responseData = new String(IoUtil.readBytes(response.getEntity().getContent()));
        String token = JSONUtil.parseObj(responseData).getStr("user_token");
        contextCache.put(GIANT_TOKEN_KEY, token);
    }

    private void syncIgpsActicities() throws IOException {
        CloseableHttpResponse activityResp = (CloseableHttpResponse) httpClient.execute(ClassicRequestBuilder.get("https://my.igpsport.com/Activity/MyActivityList").build());
        String activityRespStr = new String(IoUtil.readBytes(activityResp.getEntity().getContent()));
        IGPSActivityResp activityRespData = JSONUtil.toBean(activityRespStr, IGPSActivityResp.class, JSONConfig.create()
                .setIgnoreCase(true)
                .isIgnoreError());
        for (IGPSActivity activity : activityRespData.getItem()) {
            // 1. 判断是否本地有文件
            String filePath = fileDir + File.separator + activity.getFileName();

            if (FileUtil.exist(filePath)) {
                log.info("文件已存在" + filePath);
                continue;
            }
            // 下载文件
            downloadFileFromIgps(activity, filePath);
            // 上传到xingzhe
            uploadToXingZhe(activity, filePath);
            // sync to giant app
            uploadToGiant(filePath);
        }
    }

    private void downloadFileFromIgps(IGPSActivity activity, String filePath) throws IOException {
        String downloadUrl = "https://my.igpsport.com/fit/activity?type=0&rideid=" + activity.getRideId();
        log.info("尝试下载" + downloadUrl);

        CloseableHttpResponse downloadResp = (CloseableHttpResponse) httpClient.execute(ClassicRequestBuilder.get(downloadUrl).build());
        InputStream downloadInputStream = downloadResp.getEntity().getContent();
        long copied = IoUtil.copy(downloadInputStream, new FileOutputStream(filePath));
        log.info("{} bytes copied", copied);
    }

    private void uploadToXingZhe(IGPSActivity activity, String filePath) throws IOException {
        String uploadUrl = "https://www.imxingzhe.com/api/v4/upload_fits";
        HttpEntity uploadForm = MultipartEntityBuilder.create()
                .addPart("upload_file_name", new FileBody(new File(filePath)))
                .addPart("title", new StringBody(activity.getFileName(), ContentType.create("text/plain", Charset.defaultCharset())))
                .addPart("device", new StringBody("3", ContentType.create("text/plain", Charset.defaultCharset())))
                .addPart("sport", new StringBody("3", ContentType.create("text/plain", Charset.defaultCharset())))
                .build();

        CloseableHttpResponse uploadResponse = (CloseableHttpResponse) httpClient.execute(ClassicRequestBuilder.post(uploadUrl).setEntity(uploadForm).build());
        log.info("upload to xingzhe result: {}", new String(IoUtil.readBytes(uploadResponse.getEntity().getContent())));
    }

    private void uploadToGiant(String filePath) throws IOException {
        if (giant.getEnable()) {
            String giantUploadUrl = "https://ridelife.giant.com.cn/index.php/api/upload_fit";

            cn.hutool.http.HttpRequest formed = HttpUtil.createPost(giantUploadUrl)
                    .form("files[]", new File[]{new File(filePath)})
                    .form("brand", "giant")
                    .form("device", "bike_computer")
                    .form("token", contextCache.getOrDefault(GIANT_TOKEN_KEY, "").toString());
            cn.hutool.http.HttpResponse execute = formed.execute();
            log.info("upload to giant result: {}", execute.body());
        }
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
