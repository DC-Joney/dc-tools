package com.dc.tools.spring.http.url;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.alibaba.fastjson.JSONObject;
import com.dc.tools.spring.http.exception.HttpRequestException;
import com.dc.tools.spring.utils.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 用于创建需要加密接口的url
 *
 * @author zhangyang
 * @apiNote 好未来接口模拟调用，可改为其他实现
 */
@RequiredArgsConstructor(staticName = "create")
public class TalUrlBuilder implements UrlBuilder {

    /**
     * 好未来接口请求 access_key_id
     */
    public static final String ACCESS_KEY_PARAM = "access_key_id";

    /**
     * 好未来接口请求 签名参数名称
     */
    public static final String SIGNATURE_NONCE_PARAM = "signature_nonce";

    /**
     * 好未来接口请求 timestamp
     */
    public static final String TIMESTAMP_PARAM = "timestamp";

    /**
     * 好未来接口请求 request_body 参数名称
     */
    public static final String REQUEST_BODY_PARAM = "request_body";

    /**
     * 好未来接口请求 signature 参数名称
     */
    public static final String SIGNATURE_PARAM = "signature";


    private static final JsonMapper nonNullMapper = JsonMapper.nonNullMapper();

    private final String accessSecret;

    @Override
    public Builder builder() {
        return new Builder(accessSecret);
    }

    public static class Builder implements UrlBuilder.Builder {

        private final TreeMap<String, Object> treeMap = new TreeMap<>();
        private UriComponentsBuilder builder;
        private String body;
        private final String accessSecret;

        public Builder(String accessSecret) {
            this.accessSecret = accessSecret;
            this.builder = UriComponentsBuilder.newInstance();
        }

        @Override
        public Builder baseUrl(String baseUrl) {
            builder.uri(URI.create(baseUrl));
            return this;
        }

        @Override
        public Builder path(String path) {
            builder.path(path);
            return this;
        }

        @Override
        public Builder addParam(String key, Object value) {
            treeMap.put(key, value);
            builder.queryParam(key, value);
            return this;
        }


        public <T> Builder body(T bodyValue) {
            if (bodyValue != null) {
                if (bodyValue instanceof String)
                    this.body = (String) bodyValue;
                else
                    this.body = nonNullMapper.toJson(bodyValue);
            }

            return this;
        }


        /**
         * 对params进行format
         */
        private String generateQueryString() {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();

            treeMap.put(REQUEST_BODY_PARAM, body == null ? "" : body);

            treeMap.forEach((key, value) -> builder.queryParam(key,
                    value instanceof String ? value : nonNullMapper.toJson(value)
            ));

            treeMap.remove(REQUEST_BODY_PARAM);
            return builder.build(false).toString().substring(1);
        }

        /**
         * 计算签名
         */
        private byte[] hmacSHA1Signatures(String secret, String baseString) {
            HMac hmac = DigestUtil.hmac(HmacAlgorithm.HmacSHA1, secret.getBytes(StandardCharsets.UTF_8));
            return hmac.digest(baseString);
        }

        /**
         * 对params进行format
         */
        private String generateQueryString(Map<String, Object> params) {
            List<String> tempParams = new ArrayList<>(params.size());
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                Object val = entry.getValue();
                String sb = entry.getKey() + "=" +
                        (val instanceof String ? val : JSONObject.toJSONString(val));
                tempParams.add(sb);
            }
            return StringUtils.arrayToDelimitedString(tempParams.toArray(), "&");
        }


        @Override
        public String build() {
            try {
                String signString = generateQueryString();
                byte[] signBytes = hmacSHA1Signatures(this.accessSecret + "&", signString);
                String signStr = Base64.getEncoder().encodeToString(signBytes);
                System.out.println("encode before: " + signStr);
                //转译特殊字符
                signStr = URLEncoder.encode(signStr, "UTF-8");
                builder.queryParam(SIGNATURE_PARAM, signStr);
                treeMap.clear();
                return builder.build().toString();
            } catch (UnsupportedEncodingException e) {
                throw new HttpRequestException(e, "Url parse error, because sign param encode fail");
            }
        }
    }

    public static void main(String[] args) {
        Builder builder = TalUrlBuilder.create("").builder();

        Builder builder1 = builder.addParam("4", "4")
                .addParam("a", "b")
                .body("1234");

        System.out.println(builder1.generateQueryString());
    }

}
