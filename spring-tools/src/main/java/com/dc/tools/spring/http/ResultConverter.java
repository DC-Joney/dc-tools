package com.dc.tools.spring.http;

import reactor.util.context.ContextView;

/**
 * 数据结果转换
 *
 * @author zy
 * @param <T>
 * @param <E>
 */
public interface ResultConverter<R,E> {

    /**
     * convert result to other instance
     *
     * @param instance 结果实例
     * @return 返回对应的数据
     */
    E convert(R response, ContextView contextView);


}
