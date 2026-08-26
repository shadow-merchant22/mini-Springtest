package org.example.core;

import org.example.annotation.Component;
import org.example.annotation.Scope;
import org.example.util.ClassScanner;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext {
    //注册表
    private final Map<String,BeanDefinition> beanDefinitionMap=new ConcurrentHashMap<>();
    /** 单例池 */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    /** 要扫描的包名 */
    private final String basePackage;

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
        // 启动时刷新容器
        refresh();
    }
    /**
     * 容器启动流程
     */
    public void refresh(){
        Set<Class<?>> classes = ClassScanner.scan(basePackage);
        System.out.println("扫描到 " + classes.size() + " 个类");
        // 2. 过滤出带 @Component 的类，注册为 BeanDefinition
        for (Class<?> clazz :classes){
            if(clazz.isAnnotationPresent(Component.class)){
                registerBeanDefinition(clazz);
            }
        }
        System.out.println("注册了 " + beanDefinitionMap.size() + " 个 BeanDefinition");

        // 3. 预创建所有单例 Bean
        for (String beanName:beanDefinitionMap.keySet()){
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if(beanDefinition.isSingleton()){
                getBean(beanName);
            }
        }
        System.out.println("单例 Bean 创建完成，共 " + singletonObjects.size() + " 个");
    }
    //根据名字获取bean
    public Object getBean(String beanName) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if(beanDefinition==null){
            throw new RuntimeException("找不到 Bean: " + beanName);
        }
        return doGetBean(beanDefinition);
    }
    /**
     * 根据类型获取 Bean（简化版：按类型匹配第一个）
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> clazz) {
        for (BeanDefinition bd : beanDefinitionMap.values()) {
            if (clazz.isAssignableFrom(bd.getBeanClass())) {
                return (T) doGetBean(bd);
            }
        }
        throw new RuntimeException("找不到类型为 " + clazz.getName() + " 的 Bean");
    }
    //实际获取bean
    private Object doGetBean(BeanDefinition beanDefinition) {
        // 单例：先从缓存取，没有就创建
        if(beanDefinition.isSingleton()){
            Object bean = singletonObjects.get(beanDefinition.getBeanName());
            if(bean!=null){
                return bean;
            }
            bean=createBean(beanDefinition);
            singletonObjects.put(beanDefinition.getBeanName(),bean);
            return bean;
        }
        return createBean(beanDefinition);
    }

    /**
     * 创建 Bean 实例（暂时只做实例化，依赖注入下一步再加）
     */
    private Object createBean(BeanDefinition beanDefinition){
        try {
           return beanDefinition.getBeanClass().getDeclaredConstructor().newInstance();
        }catch (Exception e){
            throw new RuntimeException("创建 Bean 失败: " + beanDefinition.getBeanName(), e);
        }
    }

    //注册一个BeanDefinition
    private void registerBeanDefinition(Class<?> clazz) {
        //确认类名
        Component component = clazz.getAnnotation(Component.class);
        String beanName=component.value();
        if(beanName==null||beanName.isEmpty()){
            beanName=toLowerFirstLetter(clazz.getSimpleName());
        }
        //确定作用域
        String scope="singleton";
        if(clazz.isAnnotationPresent(Scope.class)){
            scope = clazz.getAnnotation(Scope.class).value();
        }
        BeanDefinition beanDefinition=new BeanDefinition();
        beanDefinition.setBeanClass(clazz);
        beanDefinition.setBeanName(beanName);
        beanDefinition.setScope(scope);

        beanDefinitionMap.put(beanName,beanDefinition);
        System.out.println("注册 Bean: " + beanName + " -> " + clazz.getName() + " (scope: " + scope + ")");
    }

    private String toLowerFirstLetter(String simpleName) {
        if(simpleName==null||simpleName.isEmpty()){
            return simpleName;
        }
        return Character.toLowerCase(simpleName.charAt(0))+simpleName.substring(1);
    }
}
