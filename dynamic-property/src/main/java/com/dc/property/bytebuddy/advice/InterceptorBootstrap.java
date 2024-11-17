package com.dc.property.bytebuddy.advice;


import com.dc.tools.common.spi.CommonServiceLoader;

import java.util.List;

public class InterceptorBootstrap {

    private static final List<BootInitializer> bootInitializers;
    static {
        bootInitializers = loadAllBootInitializer();
    }

    public static List<BootInitializer> loadAllBootInitializer() {
        return CommonServiceLoader.load(BootInitializer.class).sort();
    }

    public static void initBootInitializers(BootConfiguration configuration) {
        bootInitializers.forEach(bootInitializer -> bootInitializer.init(configuration));
    }



    /**
     * 根据系统的环境变量进行初始化
     */
    public static void initBootInitializers() {
        BootConfiguration bootConfiguration = new BootConfiguration();
//        bootConfiguration.setIgnorePackages(Lists.newArrayList("com.turing.common.property"));
        initBootInitializers(bootConfiguration);
    }

}
