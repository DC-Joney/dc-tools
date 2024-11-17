package com.dc.tools.spring.log2;

public interface MethodArgumentChanger {


    MethodInvocationArguments doChange(MethodInvocationArguments invocationArguments, MethodArgumentsChain chain);

}
