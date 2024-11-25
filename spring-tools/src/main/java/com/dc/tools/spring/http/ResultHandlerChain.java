package com.dc.tools.spring.http;

import com.alibaba.fastjson.JSONObject;

/**
 * 用于处理json返回结果
 *
 * @author zhangyang
 */
public interface ResultHandlerChain{

    /**
     * 返回处理之后新的 json数据
     * @param jsonObject 返回的json数据
     * @return json result
     */
    JSONObject handleResult(JSONObject jsonObject);



}
