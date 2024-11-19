package com.dc.tools.spring.log;

public interface MethodArgumentChanger {


    MethodInvocationArguments doChange(MethodInvocationArguments invocationArguments, MethodArgumentsChain chain);

}
