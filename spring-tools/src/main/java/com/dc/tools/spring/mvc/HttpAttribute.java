package com.dc.tools.spring.mvc;

import io.netty.util.AbstractConstant;
import io.netty.util.ConstantPool;

public class HttpAttribute<T> extends AbstractConstant<HttpAttribute<T>> {


    /**
     * 常量池
     */
    private static final ConstantPool<HttpAttribute<Object>> constantPool = new ConstantPool<HttpAttribute<Object>>() {
        @Override
        protected HttpAttribute<Object> newConstant(int id, String name) {
            return new HttpAttribute<>(id, name);
        }
    };

    private HttpAttribute(int id, String name) {
        super(id, name);
    }


    @SuppressWarnings("unchecked")
    public static <T> HttpAttribute<T> valueOf(String attributeName) {
        return (HttpAttribute<T>) constantPool.newInstance(attributeName);
    }

    public static boolean exists(String attributeName) {
        return constantPool.exists(attributeName);
    }




}
