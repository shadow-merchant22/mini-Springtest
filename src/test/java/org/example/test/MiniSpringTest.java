package org.example.test;

import org.example.annotation.Component;
import org.example.annotation.Scope;
import org.example.core.ApplicationContext;
import org.junit.Test;

public class MiniSpringTest {
    @Component
    public static class UserService {
        public void sayHello() {
            System.out.println("UserService.sayHello() 被调用了");
        }
    }

    @Component("customName")
    public static class OrderService {
        public void createOrder() {
            System.out.println("OrderService.createOrder() 被调用了");
        }
    }

    @Component
    @Scope("prototype")
    public static class PrototypeService {
    }
    @Test
    public void testScanAndRegister(){
        ApplicationContext applicationContext=new ApplicationContext("org.example.test");
        OrderService customName =(OrderService) applicationContext.getBean("customName");
        customName.createOrder();
    }
    @Test
    public void testRegisterByType(){
        ApplicationContext applicationContext=new ApplicationContext("org.example.test");
        OrderService orderService =applicationContext.getBean(OrderService.class);
        orderService.createOrder();
    }
    @Test
    public void testSingleAndPrototype(){
        ApplicationContext applicationContext=new ApplicationContext("org.example.test");
        UserService userService = applicationContext.getBean(UserService.class);
        Object userService1 = applicationContext.getBean("userService");
        System.out.println(""+(userService1==userService));
        PrototypeService bean = applicationContext.getBean(PrototypeService.class);
        Object prototypeService = applicationContext.getBean("prototypeService");
        System.out.println(""+(bean==prototypeService));

    }

}
