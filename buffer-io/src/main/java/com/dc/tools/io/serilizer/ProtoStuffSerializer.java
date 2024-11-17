package com.dc.tools.io.serilizer;

/**
 * 基于 {@code ProtoStuff} 形式的序列化方式
 *
 * @author zhangyang
 */
public class ProtoStuffSerializer<T> implements Serializer<T> {

    @Override
    public byte[] serialize(T obj) {
        return ProtoStuffUtils.serialize(obj);
    }


    @Override
    public T deserialize(byte[] data) {
      return ProtoStuffUtils.deserialize(data);
    }


}
