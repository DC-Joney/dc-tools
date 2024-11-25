package com.dc.tools.spring.http;

import com.alibaba.fastjson.JSONObject;
import com.dc.tools.spring.mvc.Result;
import org.springframework.core.annotation.Order;
import reactor.util.context.ContextView;

import java.util.function.Function;

/**
 * 全局异常捕捉
 *
 * @author zy
 */
@Order(Integer.MAX_VALUE - 1)
public class GlobalExceptionHandler implements ExceptionHandler<JSONObject> {


    private static final Function<Result<String>, HttpResponse<JSONObject>> REQUEST_FAILED_CONVERT;

    static {
        REQUEST_FAILED_CONVERT = result -> HttpResponse.errorResponse((JSONObject) JSONObject.toJSON(result));
    }

    @Override
    public HttpResponse<JSONObject> handleException(Throwable throwable, ContextView contextView) {
        Result<String> failure = Result.failure(RequestStatus.PROGRESS_FAIL, throwable.getMessage());
        return REQUEST_FAILED_CONVERT.apply(failure);
    }

    @Override
    public boolean support(Throwable error) {
        return true;
    }
}
