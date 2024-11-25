package com.dc.tools.spring.http;

import com.dc.tools.spring.mvc.ResultStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 请求好未来异常状态码
 *
 * @author zy
 */
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum RequestStatus implements ResultStatus {

    /**
     * 请求超时
     */
    TIMEOUT(90010, "Request time out for tal connection"),


    /**
     * 请求失败
     */
    REQUEST_FAIL(90011, "Request fail for tal, cause is unknown"),

    /**
     * 程序内部错误
     */
    PROGRESS_FAIL(90012, "Request fail for tal, cause is program failure"),
    ;
    ;

    int code;


    String message;


    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
