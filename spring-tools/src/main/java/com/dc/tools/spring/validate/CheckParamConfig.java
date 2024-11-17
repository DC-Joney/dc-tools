package com.dc.tools.spring.validate;

import com.dc.tools.spring.validate.mvc.CheckAfterRequestAdvice;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * 用于参数校验的配置类
 *
 * @author zhangyang
 */
@Import(CheckAfterRequestAdvice.class)
@Configuration
public class CheckParamConfig {

    @Bean
    public CheckExpressions checkExpressions() {
        return CheckExpressions.getInstance();
    }

    @Bean
    public CheckExpressionEvaluator validateExpressionEvaluator() {
        SpelExpressionParser parser = new SpelExpressionParser();
        return new CheckExpressionEvaluator(parser);
    }

}
