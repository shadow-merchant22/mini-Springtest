package org.example.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
/**
 * 注入配置文件中的值
 * 用法：@Value("${db.url}")
 */
public @interface Value {
    String value();//没有default表示必填否则编译报错
}
