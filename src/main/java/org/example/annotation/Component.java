package org.example.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)//限定只能用在类上
@Retention(RetentionPolicy.RUNTIME)//
public @interface Component {
        /**
         * Bean 的名字，默认空字符串表示用类名首字母小写
         * 这个value()并非方法，当你使用这个注解时，
         * 可以通过 @Component("xxx") 的方式给这个属性赋值。
         * 在 Java 注解中，如果只有一个属性，并且名字叫 value，那么使用时可以省略属性名，直接写值。
         */
        String value() default "";
}
