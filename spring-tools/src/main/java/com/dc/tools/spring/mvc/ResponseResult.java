package com.dc.tools.spring.mvc;


import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

import java.util.Optional;

/**
 * 返回类型封装
 * @author zhangyang
 */
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.MODULE)
public class ResponseResult<T> {

    private static final String DEFAULT_SUCCESS_MESSAGE = "调用成功！";

    private static final String DEFAULT_FAIL_MESSAGE = "调用失败,请联系管理员进行处理！";

    private int code;

    /**
     * 返回的数据
     */
    private T result;

    private boolean success;

    /**
     * 调用接口失败消息
     */
    private String message;

    public static <E> ResponseResultBuilder<E> builder() {
        return new ResponseResultBuilder<E>(HttpStatus.OK);
    }

    public static <E> ResponseResultBuilder<E> result(E result) {
        return new ResponseResultBuilder<E>(HttpStatus.OK, result);
    }


    public static <E> ResponseResultBuilder<E> ok() {
        return new ResponseResultBuilder<E>(HttpStatus.OK);
    }

    public static <E> ResponseResultBuilder<E> status(HttpStatus httpStatus) {
        return new ResponseResultBuilder<E>(httpStatus);
    }

    @Setter
    @Getter
    @NoArgsConstructor(access = AccessLevel.MODULE)
    @Accessors(fluent = true, chain = true)
    public static class ResponseResultBuilder<T> {

        private HttpStatus httpStatus;

        private T result;

        private boolean success = true;

        private String message = DEFAULT_SUCCESS_MESSAGE;

        ResponseResultBuilder(HttpStatus httpStatus) {
            this.httpStatus = httpStatus;
        }

        ResponseResultBuilder(HttpStatus httpStatus, T result) {
            this.httpStatus = httpStatus;
            this.result = result;
        }


        public ResponseResultBuilder<T> result(T result) {
            this.result = result;
            return this;
        }


        public ResponseResult<T> build() {
            if (!httpStatus.is2xxSuccessful()) {
                success = false;
                this.message = Optional.ofNullable(this.message)
                        .orElse(DEFAULT_FAIL_MESSAGE);
            }
            return new ResponseResult<>(httpStatus.value(), result, success, message);
        }

    }


}
