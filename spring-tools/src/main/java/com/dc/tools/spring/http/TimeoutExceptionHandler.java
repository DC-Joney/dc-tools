package com.dc.tools.spring.http;

import com.alibaba.fastjson.JSONObject;
import com.dc.tools.spring.mvc.Result;
import org.springframework.core.annotation.Order;
import reactor.util.context.ContextView;

import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * 超时请求异常处理
 *
 * @author zy
 */
@Order(-100)
public class TimeoutExceptionHandler implements ExceptionHandler<JSONObject>{

    private static final Supplier<JSONObject> TIMEOUT;
    static {
        TIMEOUT = ()-> (JSONObject) JSONObject.toJSON(Result.failure(RequestStatus.TIMEOUT));
    }


    private static final JSONObject REQUEST_FAILED;
    static {
        REQUEST_FAILED = (JSONObject) JSONObject.toJSON(Result.failure(RequestStatus.REQUEST_FAIL));
    }

    @Override
    public HttpResponse<JSONObject> handleException(Throwable throwable, ContextView contextView) {
        return HttpResponse.errorResponse(TIMEOUT.get());
    }

    @Override
    public boolean support(Throwable error) {
        return TimeoutException.class.isAssignableFrom(error.getClass());
    }
}
