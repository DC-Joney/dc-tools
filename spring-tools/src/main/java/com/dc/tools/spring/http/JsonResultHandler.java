package com.dc.tools.spring.http;

import com.alibaba.fastjson.JSONObject;

/**
 * 用于处理json返回结果
 *
 * @author zhangyang
 */
public interface JsonResultHandler {

    /**
     * 返回处理之后新的 json数据
     * @param jsonObject 返回的json数据
     * @param chain handler result chain
     * @return json result
     */
    JSONObject handleResult(JSONObject jsonObject, ResultHandlerChain chain);

}
