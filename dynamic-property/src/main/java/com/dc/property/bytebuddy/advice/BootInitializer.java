package com.dc.property.bytebuddy.advice;

public interface BootInitializer{

    void init(BootConfiguration configuration);


    void destroy();
}
