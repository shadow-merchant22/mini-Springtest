package org.example.aop;

import org.example.annotation.After;
import org.example.annotation.Before;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/*
* AOP（面向切面编程）
├── 切面（Aspect）
│   ├── 通知（Advice）
│   │   ├── 前置通知（@Before）
│   │   ├── 后置通知（@After）
│   │   ├── 返回通知（@AfterReturning）
│   │   ├── 异常通知（@AfterThrowing）
│   │   └── 环绕通知（@Around）
│   └── 切点（Pointcut）
├── 连接点（JoinPoint）
└── 织入（Weaving）
* */

/*
* Spring AOP与IOC的关系，手动AOP的话可以不用依赖IOC
* Spring 框架
├── IOC 容器（Bean工厂）
│   ├── 创建Bean
│   ├── 管理Bean生命周期
│   ├── 处理依赖注入
│   └── 存储Bean定义
│
└── AOP（面向切面）
    ├── 使用IOC容器中的Bean
    ├── 创建代理对象
    ├── 替换IOC容器中的原始对象
    └── 增强Bean的功能
* */
/**
 * AOP 代理工厂
 * 负责为目标对象创建代理，在方法调用前后织入通知
 */
public class ProxyFactory {
    /**
     * 为目标对象创建代理
     * @param target 目标对象
     * @param aspect 切面对象（包含 @Before / @After 方法）
     * @return 代理对象（如果目标类实现了接口），否则返回原对象
     */
    public static Object createProxy(Object target,Object aspect){
        Class<?> targetClass = target.getClass();
        Class<?>[] interfaces = targetClass.getInterfaces();

        // 没有实现接口 → 简化版直接返回原对象
        // （完整版应该用 CGLIB，这里暂时不做）
        if(interfaces.length==0){
            System.out.println("[AOP] " + targetClass.getSimpleName()
                    + " 没有实现接口，跳过代理");
            return target;
        }
        //若是一个类中没有任何的通知，则无需代理
        if(!hasAdvice(targetClass,aspect)){
            return target;
        }
        return createJdkProxy(target, aspect, interfaces);
    }
    //判断是否需要代理
    private static boolean hasAdvice(Class<?> targetClass, Object aspect) {
        Map<String, Method> beforeAdviceMap =collectAdvice(aspect, Before.class);
        Map<String,Method> afterAdviceMap =collectAdvice(aspect,After.class);
        if (beforeAdviceMap.isEmpty()&&afterAdviceMap.isEmpty()){
            return false;
        }
        for (Method method:targetClass.getDeclaredMethods()){
            if (beforeAdviceMap.containsKey(method.getName())||afterAdviceMap.containsKey(method.getName())){
                return true;
            }
        }
        return false;
    }

    //使用JDK动态代理
    private static Object createJdkProxy(Object target, Object aspect, Class<?>[] interfaces) {
        // 收集切面中的通知方法
        // key: 目标方法名, value: 对应的通知方法
        Map<String, Method> beforeAdviceMap =collectAdvice(aspect, Before.class);
        Map<String,Method> afterAdviceMap =collectAdvice(aspect,After.class);

        //InvocationHandler 是 JDK 动态代理的方法调用拦截器：
        // InvocationHandler 是 JDK 动态代理的核心接口，用于定义代理对象的方法调用处理逻辑。
        // 当调用代理对象的任何方法时，都会转发到这个接口的 invoke 方法中。
        InvocationHandler handler=new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // method：被调用的方法对象（接口中声明的方法）
                System.out.println("方法名: " + method.getName());
                System.out.println("返回类型: " + method.getReturnType());
                System.out.println("声明类: " + method.getDeclaringClass());
                System.out.println("参数类型: " + Arrays.toString(method.getParameterTypes()));
                String methodName = method.getName();
                // === 1. 执行前置通知 ===
                Method beforeMethod = beforeAdviceMap.get(methodName);
                if(beforeMethod!=null){
                    beforeMethod.setAccessible(true);
                    beforeMethod.invoke(aspect);
                }
                //2.执行目标方法
                Object result = method.invoke(target, args);
                //3.执行后置流程
                Method afterMethod = afterAdviceMap.get(methodName);
                if(afterMethod!=null){
                    afterMethod.setAccessible(true);
                    afterMethod.invoke(aspect);
                }
                return result;
            }
        };
        return Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                interfaces,
                handler
        );
    }

    /**
     * 收集切面中带有指定注解的方法
     *
     * @param aspect 切面对象
     * @param annotationClass 注解类型（Before.class 或 After.class）
     * @return Map<目标方法名, 通知方法>
     */
    private static Map<String, Method> collectAdvice(Object aspect, Class<? extends Annotation> annotationClass) {
        Map<String,Method> result=new HashMap<>();
        for (Method method:aspect.getClass().getDeclaredMethods()){
            if(annotationClass== Before.class){
                Before before = method.getAnnotation(Before.class);
                if(before!=null){
                    result.put(before.value(), method);
                }
            }else if (annotationClass== After.class){
                After after = method.getAnnotation(After.class);
                if (after!=null){
                    result.put(after.value(), method);
                }
            }
        }
        return result;
    }


}
