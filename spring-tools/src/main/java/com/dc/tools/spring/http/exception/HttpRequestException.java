package com.dc.tools.spring.http.exception;

import cn.hutool.core.util.StrUtil;
import com.dc.tools.common.utils.JsonObjectBuilder;
import lombok.Getter;

/**
 * 请求远程接口时出现的异常
 *
 * @author zhangyang
 */
public class HttpRequestException extends RuntimeException {

    private static final int ERROR_CODE = -1;
    @Getter
    private String jsonResult;

    @Getter
    private Integer resultCode;

    /**
     * @param jsonResult 好未来返回的错误信息
     * @param resultCode 好未来返回的错误状态码
     * @param message    异常信息message
     * @param args       参数
     */
    public HttpRequestException(String jsonResult, Integer resultCode, String message, Object... args) {
        super(StrUtil.format(message, args));
        this.jsonResult = jsonResult;
        this.resultCode = resultCode;
    }

    public HttpRequestException(String message, Object... args) {
        super(StrUtil.format(message, args));
        this.resultCode = ERROR_CODE;
        this.jsonResult = JsonObjectBuilder.create()
                .put("code", ERROR_CODE)
                .put("turing_error", true)
                .put("msg", StrUtil.format(message, args))
                .build().toJSONString();
    }

    public HttpRequestException(Throwable cause, String message, Object... args) {
        super(message, cause);
        this.resultCode = ERROR_CODE;
        this.jsonResult = JsonObjectBuilder.create()
                .put("code", ERROR_CODE)
                .put("turing_error", true)
                .put("msg", StrUtil.format(message, args))
                .build().toJSONString();
    }


    public HttpRequestException(Throwable cause) {
        super(cause);
    }
}
