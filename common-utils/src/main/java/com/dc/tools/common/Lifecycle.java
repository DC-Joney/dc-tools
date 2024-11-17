package com.dc.tools.common;

public interface Lifecycle<C> {

    void startUp(C config);

    void shutdown();

}
