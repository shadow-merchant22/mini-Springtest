package org.example.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)//用在字段上
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {
}
