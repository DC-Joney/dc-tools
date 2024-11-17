package com.dc.tools.spring.validate;

import com.dc.tools.spring.validate.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Collections;
import java.util.List;

public class ValidateExample {

    public static void main1(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
                CheckParamConfig.class, ValidateMethod.class);

        ParamExample paramExample = new ParamExample();
        paramExample.deviceId = "123456";
        paramExample.apiKey = "000000";
        NestedObject nestedObject = null;

        paramExample.setNestedObject(nestedObject);


        NestedObjectList nestedObjectList = new NestedObjectList();
        nestedObjectList.name = "null";
        paramExample.setNestedObjectLists(Collections.emptyList());

        StopWatch stopWatch = new StopWatch("statics check time");

        stopWatch.start("first check time");

        CheckExpressions.check(paramExample, ValidateExample.class);

        stopWatch.stop();

        System.out.println(paramExample.uuid);

        stopWatch.start("second check time");
//        paramExample.setNestedObject(new NestedObject());
        CheckExpressions.check(paramExample, ValidateExample.class);
        stopWatch.stop();


        System.out.println(stopWatch.prettyPrint());

    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
                CheckParamConfig.class, ValidateMethod.class);

        ParamExample paramExample = new ParamExample();
        paramExample.deviceId = "123456";
        paramExample.apiKey = "000000";
        NestedObject nestedObject = new NestedObject();
        paramExample.setNestedObject(nestedObject);

        CheckExpressions.check(paramExample, ValidateExample.class);

    }

    @Setter
    @Getter
    // @el(root: com.turing.platform.common.validate.ValidateExample.ParamExample)
    // @el(check: com.turing.pt.common.validate.CheckUtil)
    // @el(apiKey: java.lang.String)
    // @el(deviceId: java.lang.String)
    @ToString
    public static class ParamExample {

        //校验apiKey是否合法
        @CheckMethod(method = "validateApiKey(#apiKey, #deviceId)", scope = ValidateExample.class)
        private String apiKey;

        //校验deviceId是否合法
        @CheckMethod(method = "@validateMethod.validateDeviceId(#deviceId, #apiKey)")
        String deviceId;

        /**
         * 校验apiKeyParams是否合法
         */
        @CheckParams({
                @CheckParam(condition = "#check.isEmpty(#apiKey)", message = "apiKey不能有值", scope = ParamExample.class)
        })
        String apiKeyParams;

        @NestedCheck
        NestedObject nestedObject;

        @NestedCheck
        List<NestedObjectList> nestedObjectLists;

        /**
         * 为uuid字段注入新的值，并且校验其是否合法
         */
        @ParamValue(value = "@validateMethod.generateUUID()", condition = "#root.nestedObject != null ", scope = ValidateExample.class)
        UUIDS uuid;


        public void validateApiKey(String apiKey, String deviceId) {
            System.out.println(apiKey);
        }

    }

    @Getter
    @Setter
    // @el(root: com.turing.platform.common.validate.ValidateExample.NestedObject)
    // @el(check: com.turing.pt.common.validate.CheckUtil)
    public static class NestedObject {

        @CheckParam(condition = "#check.hasText(#root.name)", message = "NestedObject name不能为空", scope = ValidateExample.class)
        @CheckMethod(method = "@validateMethod.checkParent(#parent,#root.name)")
        String name;

    }

    @Getter
    @Setter
    // @el(root: com.turing.platform.common.validate.ValidateExample.NestedObjectList)
    // @el(check: com.turing.pt.common.validate.CheckUtil)
    public static class NestedObjectList {

        @CheckParam(condition = "#check.hasText(#root.name)", message = "NestedObjectList name不能为空")
        @CheckMethod(method = "@validateMethod.checkParent(#parent,#root.name)")
        String name;


    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    private static class UUIDS {

        String uuid;
        private String name;

    }

    @Component(value = "validateMethod")
    public static class ValidateMethod {

        public void validateDeviceId(String deviceId, String apiKey) {
            System.out.println("执行到这里了");
            System.out.println(deviceId + "@" + apiKey);
        }

        public UUIDS generateUUID() {
            System.out.println("生成uuid");
            return UUIDS.of("3456", "1234");
        }

        public void checkParent(Object parent, String name) {
            System.out.println("parent :" + parent);
            System.out.println("name :" + name);
        }

    }

}
