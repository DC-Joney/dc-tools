package com.dc.tools.spring.http.url;

import com.alibaba.fastjson.JSONObject;

/**
 * 用于创建第三方接口 url
 *
 * @see TalUrlBuilder
 */
public interface UrlBuilder {

    /**
     * 返回用于创建第三方接口url接口的Builder
     */
    Builder builder();

    /**
     * 用于针对不同的第三方接口来创建相应的 url
     */
    interface Builder {

        /**
         * @param baseUrl remote interface base url
         */
        Builder baseUrl(String baseUrl);

        /**
         * Add url path
         * @param path path
         */
        Builder path(String path);

        /**
         * Add url param
         * @param key param key
         * @param value param value
         */
        Builder addParam(String key, Object value);

        /**
         * Return build success url
         */
        String build();
    }
}
