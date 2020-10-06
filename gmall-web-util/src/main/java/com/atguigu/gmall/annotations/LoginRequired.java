package com.atguigu.gmall.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解
 * Target  作用范围
 * Retention  生效范围
 */
@Target(ElementType.METHOD)    //作用在方法上生效
@Retention(RetentionPolicy.RUNTIME)  //生效范围
public @interface LoginRequired {
    boolean loginSuccess() default true;
}
