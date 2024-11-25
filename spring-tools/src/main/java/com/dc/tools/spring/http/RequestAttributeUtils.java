package com.dc.tools.spring.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dc.tools.common.utils.WriteContext;
import com.dc.tools.spring.mvc.Result;
import com.dc.tools.spring.mvc.ResultStatus;
import lombok.experimental.UtilityClass;
import reactor.util.context.ContextView;

import java.util.function.Consumer;

/**
 * @author zy
 */
@UtilityClass
public class RequestAttributeUtils {

    public static final String ACCESS_KEY = "access_key";

    public static final String ACCESS_SECRET = "access_secret";

    public static final String CLIENT_NAME = "client_name";

    public static final String REQUEST_URL = "request_url";

    public static final String REQUEST_PATH = "request_path";

    /**
     * HTTP status code
     */
    public static final String HTTP_CODE = "HTTP_CODE";

    public static final String ORIGIN_RESULT = "origin_result";

    public static final String GLOBAL_ID = "globalId";

    public static final String CONFIG = "config";


    public static <T> void putAttribute(WriteContext writeContext, String attributeKey, T attributeValue) {
        writeContext.put(attributeKey, attributeValue);
    }

    public static <T> void putAttribute(ContextView contextView, String attributeKey, T attributeValue) {
        WriteContext writeContext = contextView.get(WriteContext.class);
        writeContext.put(attributeKey, attributeValue);
    }

    public static <T> T getAttribute(ContextView contextView, String attributeKey, Class<T> valueClass) {
        T value = contextView.getOrDefault(attributeKey, null);
        if (value == null) {
            WriteContext writeContext = contextView.get(WriteContext.class);
            value = writeContext.get(attributeKey, valueClass);
        }

        return value;
    }


    public static String getStringAttribute(ContextView contextView, String attributeKey) {
        return getAttribute(contextView, attributeKey, String.class);
    }


    public static boolean containsAttribute(ContextView contextView, String attributeKey) {
        return getAttribute(contextView, attributeKey, Object.class) != null;
    }



    public static JSONObject formatResult(Result<?> result) {
        return (JSONObject) JSON.toJSON(result);
    }

    public static JSONObject formatResult(ResultStatus resultStatus, Consumer<Result<Void>> consumer) {
        Result<Void> failure = Result.failure(resultStatus);
        return (JSONObject) JSON.toJSON(failure);
    }


    public static <T> JSONObject formatResult(ResultStatus resultStatus, String message) {
        Result<Void> failure = Result.failure(resultStatus, message);
        return (JSONObject) JSON.toJSON(failure);
    }

    @Deprecated
    public static JSONObject addMarkOrigin(JSONObject jsonObject, String requestPath) {
        jsonObject.put(TalJsonPropertyPreFilter.class.getName(), null);
        jsonObject.put(REQUEST_PATH, requestPath);
        return jsonObject;
    }

    @Deprecated
    public static JSONObject removeMarkOrigin(JSONObject jsonObject) {
        jsonObject.remove(TalJsonPropertyPreFilter.class.getName());
        jsonObject.remove(REQUEST_PATH);
        return jsonObject;
    }

    @Deprecated
    public static boolean containsMarkOrigin(JSONObject jsonObject) {
        return jsonObject.containsKey(TalJsonPropertyPreFilter.class.getName());
    }


}
