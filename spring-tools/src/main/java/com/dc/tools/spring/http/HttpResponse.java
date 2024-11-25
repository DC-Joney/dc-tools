package com.dc.tools.spring.http;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 数据相应
 *
 * @author zy
 */
@Getter
@Setter
@ToString
public class HttpResponse<T> {

    public static final int UNKNOW_CODE = -3;

    public static final int FAIL_CODE = -1;
    public static final int SUCCESS_CODE = 0;

    /**
     * 返回的结果code
     */
    private  int code;

    private T response;

    private String errorMessage;


    public boolean isError(){
        return code != FAIL_CODE;
    }

    public static <T> HttpResponse<T> errorResponse() {
        HttpResponse<T> response = new HttpResponse<>();
        response.setCode(FAIL_CODE);
        return response;
    }

    public static <T> HttpResponse<T> errorResponse(T result) {
        HttpResponse<T> response = new HttpResponse<>();
        response.setCode(FAIL_CODE);
        response.setResponse(result);
        return response;
    }

    public static <T> HttpResponse<T> errorResponse(String errorMessage) {
        HttpResponse<T> response = new HttpResponse<>();
        response.setCode(FAIL_CODE);
        response.setErrorMessage(errorMessage);
        return response;
    }

    public static <T> HttpResponse<T> errorResponse(Integer resultCode, String errorMessage) {
        HttpResponse<T> response = new HttpResponse<>();
        response.setCode(FAIL_CODE);
        response.setErrorMessage(errorMessage);
        response.setCode(resultCode);
        return response;
    }


    public static <T> HttpResponse<T> successResponse(T data) {
        HttpResponse<T> response = new HttpResponse<>();
        response.setResponse(data);
        response.setCode(SUCCESS_CODE);
        return response;
    }

}
