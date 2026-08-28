package org.example.test;

import org.example.annotation.After;
import org.example.annotation.Aspect;
import org.example.annotation.Before;
import org.example.annotation.Component;
import org.example.core.ApplicationContext;
import org.junit.Test;

public class ProxyTest {
    @Component
    public static class Student {
        private Integer id;
        private String name;
        public void sayHello(){
            System.out.println("hello");
        }
    }
    public interface Teacher{
        void teach();
        void sayHello();
    }
    @Component
    public static class TeacherImpl implements Teacher{
        @Override
        public void teach() {
            System.out.println("教书");
        }
        public void sayHello(){
            System.out.println("同学们好");
        }
    }
    @Aspect
    @Component
    public static class TestAspect{
        @Before("sayHello")
        public void beforeSayHello(){
            System.out.println("[Before] 即将执行 sayHello");
        }
        @Before("teach")
        public void beforeTeach(){
            System.out.println(" [Before] 即将执行 Teach");
        }
        @After("teach")
        public void afterTeach(){
            System.out.println(" [After] 执行 Teach结束");
        }
    }
    @Test
    public void TestProxy(){
        //预期结果：
        //[Before] 即将执行 Teach
        // 教书
        // [After] 执行 sayHello结束
        //[Before] 即将执行 sayHello
        //同学们好
        //hello
        ApplicationContext applicationContext=new ApplicationContext("org.example.test");
        Teacher teacher = applicationContext.getBean(Teacher.class);
        teacher.teach();
        teacher.sayHello();
        Student student = applicationContext.getBean(Student.class);
        student.sayHello();
    }
}
