package org.example.test;

import org.example.annotation.Autowired;
import org.example.annotation.Component;
import org.example.annotation.Value;
import org.example.core.ApplicationContext;
import org.junit.Test;

public class InjectTest {
    @Component
    public static class DatabaseConfig {
        @Value("${db.url}")
        private String url;

        @Value("${db.username}")
        private String username;

        @Value("${app.name}")
        private String appName;

        public void print() {
            System.out.println("url=" + url + ", username=" + username + ", appName=" + appName);
        }
    }
    @Component
    public static class UserService{
        @Autowired
        private DatabaseConfig databaseConfig;
        public void print() {
            System.out.println("UserService 持有: " + databaseConfig);
            databaseConfig.print();
        }
    }
    @Component
    public static class OrderService{
        @Autowired
        private DatabaseConfig databaseConfig;
        public void print() {
            System.out.println("OrderService 持有: " + databaseConfig);
            databaseConfig.print();
        }
    }
    @Test
    public void testAutowired(){
        ApplicationContext applicationContext=new ApplicationContext("org.example.test");
        UserService userService = applicationContext.getBean(UserService.class);
        userService.print();
        //可以看到这条测试用例的执行结果是
        // java.lang.RuntimeException: 找不到类型为 org.example.test.InjectTest$UserService 的 Bean
        //这是因为在MiniSpringTest包下也有一个UserService类，在注册Bean的时候会有两个key为userService
        //但是BeanDefinition不同的类，因为是Map，所以在put时后面一个会将前一个覆盖，导致
        //  if (clazz.isAssignableFrom(bd.getBeanClass())) {
        //                return (T) doGetBean(bd);
        //            }
        //在判断时，InjectTest$UserService既不是MiniSpringTest$UserService的父类也非接口更不是本体，
        // 所以找不到对应的bean
    }
    @Test
    public void testAutowired2(){
        ApplicationContext applicationContext=new ApplicationContext("org.example.test");
        OrderService bean = applicationContext.getBean(OrderService.class);
        bean.print();
        //在MiniSpringTest包下也有一个OrderService类
        // 那为什么这里的OrderService没有出现像上面UserService的情况呢？
        //其实是因为MiniSpringTest在OrderService类上添加的注解：@Component("customName")
        //为OrderService在Map中的key起了个名字叫customName，所以不会覆盖掉。
        //这就是人们常说的SpringIOC容器中bean的名字，所以SpringIOC容器底层核心存储结构就是个map？
    }

}
