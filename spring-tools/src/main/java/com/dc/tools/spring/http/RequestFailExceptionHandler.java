package com.dc.tools.spring.http;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.dc.tools.spring.mvc.Result;
import org.springframework.core.annotation.Order;
import reactor.util.context.ContextView;

import java.util.function.Function;

/**
 * 结果解析失败处理
 *
 * @author zy
 */
@Order(0)
public class RequestFailExceptionHandler implements ExceptionHandler<JSONObject> {


    private static final Function<Result<String>, HttpResponse<JSONObject>> REQUEST_FAILED_CONVERT;

    static {
        REQUEST_FAILED_CONVERT = result -> HttpResponse.errorResponse((JSONObject) JSONObject.toJSON(result));
    }

    @Override
    public HttpResponse<JSONObject> handleException(Throwable throwable, ContextView contextView) {
        String result = RequestAttributeUtils.getStringAttribute(contextView, RequestAttributeUtils.ORIGIN_RESULT);
        //如果是解析json错误
        if (JSONException.class.isAssignableFrom(throwable.getClass())) {
            Result<String> failure = Result.failure(RequestStatus.REQUEST_FAIL, throwable.getMessage(), result);
            return REQUEST_FAILED_CONVERT.apply(failure);
        }


        Result<String> failure = Result.failure(RequestStatus.REQUEST_FAIL, throwable.getMessage());
        return REQUEST_FAILED_CONVERT.apply(failure);
    }

    @Override
    public boolean support(Throwable error) {
        return Exception.class.isAssignableFrom(error.getClass());
    }
}
