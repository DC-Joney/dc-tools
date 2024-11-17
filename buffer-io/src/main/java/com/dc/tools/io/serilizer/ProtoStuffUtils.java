package com.dc.tools.io.serilizer;

import com.dc.tools.common.annotaion.JustForTest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import lombok.experimental.UtilityClass;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提供 ProtoStuff Serializer 实现
 *
 * @author zhangyang
 */
@UtilityClass
public class ProtoStuffUtils {
    /**
     * 避免每次序列化都重新申请Buffer空间
     */
    private static final LinkedBuffer buffer = LinkedBuffer.allocate(8192);
    /**
     * 缓存Schema
     */
    private static final Map<Class<?>, Schema<?>> schemaCache = new ConcurrentHashMap<>();


    /**
     * 序列化方法，把指定对象序列化成字节数组
     */
    public <T> byte[] serialize(T instance) {
        ByteBuf classBuffer = serializeBuf(instance);
        byte[] resultBytes = new byte[classBuffer.writerIndex()];
        classBuffer.readBytes(resultBytes);
        return resultBytes;
    }

    /**
     * 序列化方法，把指定对象序列化成字节数组
     */
    @SuppressWarnings("unchecked")
    public <T> ByteBuf serializeBuf(T instance) {
        Class<T> clazz = (Class<T>) instance.getClass();
        String className = clazz.getName();
        Schema<T> schema = getSchema(clazz);
        byte[] data;
        try {
            data = ProtostuffIOUtil.toByteArray(instance, schema, LinkedBuffer.allocate(1024));
            ByteBuf outputBuf = Unpooled.buffer(data.length + className.length());
            outputBuf.writeInt(className.length());
            outputBuf.writeCharSequence(clazz.getName(), Charset.defaultCharset());
            outputBuf.writeBytes(data);
            return outputBuf;
        } finally {
            buffer.clear();
        }
    }

    /**
     * 反序列化方法，将字节数组反序列化成指定Class类型
     */
    public <T> T deserialize(byte[] data) {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(data);
        return deserialize(byteBuf);
    }


    /**
     * 反序列化方法，将字节数组反序列化成指定Class类型
     */
    @SuppressWarnings("unchecked")
    public <T> T deserialize(ByteBuf byteBuf) {
        try {
            byteBuf.markReaderIndex();
            int classLength = byteBuf.readInt();
            ByteBuf classBuf = byteBuf.readBytes(classLength);
            byte[] classBytes = new byte[classLength];
            classBuf.readBytes(classBytes);
            Class<?> deserializeClass = Class.forName(new String(classBytes, Charset.defaultCharset()));
            byte[] deserializeBytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(deserializeBytes);
            Schema<T> schema = (Schema<T>) getSchema(deserializeClass);
            T instance = schema.newMessage();
            byteBuf.resetReaderIndex();
            ProtostuffIOUtil.mergeFrom(deserializeBytes, instance, schema);
            return instance;
        } catch (ClassNotFoundException e) {
            throw new SerializerException("Cannot deserialize current bytes, please check it");
        }
    }


    @SuppressWarnings("unchecked")
    private static <T> Schema<T> getSchema(Class<T> clazz) {
        Schema<T> schema = (Schema<T>) schemaCache.get(clazz);
        if (schema == null) {
            //这个schema通过RuntimeSchema进行懒创建并缓存
            //所以可以一直调用RuntimeSchema.getSchema(),这个方法是线程安全的
            schema = RuntimeSchema.getSchema(clazz);
            if (schema == null) {
                schemaCache.put(clazz, schema);
            }
        }

        return schema;
    }


    @JustForTest
    public void serializeTest(){
        Test parentTest = new Test();

        parentTest.testInstance = new Test1();
        parentTest.test = "456";

        byte[] serialize = ProtoStuffUtils.serialize(parentTest);
        Test deserialize = ProtoStuffUtils.deserialize(serialize);
        System.out.println(deserialize);
    }


    static class Test1 {
        int name1 = 1;
        int age1 = 2;
        String test1 = "123";
        Integer number1;

        @Override
        public String toString() {
            return "Test1{" +
                    "name1=" + name1 +
                    ", age1=" + age1 +
                    ", test1='" + test1 + '\'' +
                    ", number1=" + number1 +
                    '}';
        }
    }

    static class Test extends Test1{
        int name = 4;
        int age = 5;
        String test = "456";
        Integer number;
        Test1 testInstance;

        @Override
        public String toString() {
            return "Test{" +
                    "name1=" + name1 +
                    ", age1=" + age1 +
                    ", test1='" + test1 + '\'' +
                    ", number1=" + number1 +
                    ", name=" + name +
                    ", age=" + age +
                    ", test='" + test + '\'' +
                    ", number=" + number +
                    '}';
        }
    }

}
