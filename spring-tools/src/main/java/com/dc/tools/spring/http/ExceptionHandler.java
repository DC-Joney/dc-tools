package com.dc.tools.spring.http;

import cn.hutool.core.util.StrUtil;
import org.springframework.core.annotation.Order;
import reactor.util.context.ContextView;

/**
 * @author zy
 */
public interface ExceptionHandler<C> {

    HttpResponse<C> handleException(Throwable throwable, ContextView contextView);


    /**
     * 是否支持当前异常信息
     *
     * @param error 异常
     */
    boolean support(Throwable error);


    @Order(Integer.MAX_VALUE)
    class DefaultExceptionHandler<C> implements ExceptionHandler<C> {

        @Override
        public HttpResponse<C> handleException(Throwable throwable, ContextView contextView) {
            return HttpResponse.errorResponse(StrUtil.format("Execute fail cause is :{}", throwable.getMessage()));
        }

        @Override
        public boolean support(Throwable error) {
            return true;
        }
    }

}
