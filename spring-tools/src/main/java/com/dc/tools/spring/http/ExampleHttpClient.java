package com.dc.tools.spring.http;

import com.alibaba.fastjson.JSONObject;
import com.dc.tools.common.date.TemporalUtils;
import com.dc.tools.spring.http.url.TalUrlBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.util.context.ContextView;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.dc.tools.spring.http.url.TalUrlBuilder.*;


/**
 * http 请求样例实现
 *
 * @author zy
 */
@Slf4j
public class ExampleHttpClient extends AbstractHttpClient<JSONObject, JSONObject, JSONObject> {


    @Override
    public String clientName() {
        return "common request";
    }

    @Override
    protected String getRequestUrl(JSONObject request, ContextView contextView) {
        String uid = UUID.randomUUID().toString();
        String timestamp = TemporalUtils.TAL_LOCAL_DATE_TIME.format(LocalDateTime.now());

        String accessKey = RequestAttributeUtils.getStringAttribute(contextView, "");
        String accessSecret = RequestAttributeUtils.getStringAttribute(contextView, "ACCESS_SECRET");
        String requestUrl = RequestAttributeUtils.getStringAttribute(contextView, "REQUEST_URL");


        //创建请求url，可以做通用抽象
        return TalUrlBuilder.create(accessSecret)
                .builder()
                .baseUrl(requestUrl)
                .addParam(ACCESS_KEY_PARAM, accessKey)
                .addParam(SIGNATURE_NONCE_PARAM, uid)
                .addParam(TIMESTAMP_PARAM, timestamp)
                .body(request).build();
    }

    @Override
    protected JSONObject convertToResponse(JSONObject jsonObject) {
        return jsonObject;
    }
}
