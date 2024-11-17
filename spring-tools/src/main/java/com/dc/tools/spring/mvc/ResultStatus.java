package com.dc.tools.spring.mvc;

/**
 * 返回的结果状态码接口
 *
 * @author zy
 */
public interface ResultStatus {

    /**
     * 返回的状态码
     *
     * @return code
     */
    int getCode();

    /**
     * 返回的信息
     *
     * @return message
     */
    String getMessage();


}
