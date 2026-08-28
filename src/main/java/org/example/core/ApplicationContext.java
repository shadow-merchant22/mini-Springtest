package org.example.core;

import org.example.annotation.*;
import org.example.aop.ProxyFactory;
import org.example.util.ClassScanner;
import org.example.util.PropertiesLoader;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext {
    //注册表
    private final Map<String,BeanDefinition> beanDefinitionMap=new ConcurrentHashMap<>();
    /** 单例池 */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    /** 要扫描的包名 */
    private final String basePackage;
    /** 配置文件中的属性 */
    private final Properties properties;
    //存储所有的切面对象
    private final List<Object> aspects=new ArrayList<>();

    public ApplicationContext(String basePackage,String configFile) {
        this.basePackage = basePackage;
        this.properties = PropertiesLoader.load(configFile);
        // 启动时刷新容器
        refresh();
    }
    public ApplicationContext(String basePackage){
        this(basePackage,"application.properties");
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
        //先创建切面Aspect类
        for (String beanName: beanDefinitionMap.keySet()){
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if(beanDefinition.isSingleton()&&beanDefinition.getBeanClass().isAnnotationPresent(Aspect.class)){
                Object bean = getBean(beanName);
                aspects.add(bean);
                System.out.println("发现切面: " + beanDefinition.getBeanClass().getSimpleName());
            }
        }
        //再创建普通bean
        for (String beanName:beanDefinitionMap.keySet()){
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if(beanDefinition.isSingleton()&&!beanDefinition.getBeanClass().isAnnotationPresent(Aspect.class)){
                getBean(beanName);

            }
        }
        System.out.println("发现切面"+aspects.size()+"个");
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
     * 创建 Bean 实例（暂时只做实例化，依赖注入下一步再加）+依赖注入（@Autowired 和 @Value）
     */
    private Object createBean(BeanDefinition beanDefinition){
        try {
            Object bean = beanDefinition.getBeanClass().getDeclaredConstructor().newInstance();
            //依赖注入
            injectFields(bean);
            //AOP增强（带有@Aspect的切面类本身无需代理）
            if(!beanDefinition.getBeanClass().isAnnotationPresent(Aspect.class)){
                for (Object aspect:aspects){
                    Object proxy = ProxyFactory.createProxy(bean, aspect);
                    //当代理成功时替换
                    if(bean!=proxy){
                        bean=proxy;
                        System.out.println("AOP为"+beanDefinition.getBeanName()+"创建了代理");
                    }
                }
            }

            return bean;
        }catch (Exception e){
            throw new RuntimeException("创建 Bean 失败: " + beanDefinition.getBeanName(), e);
        }
    }

    /**
     * 处理字段注入：
     * - @Autowired：从容器中查找对应类型的 Bean 注入
     * - @Value("${key}")：从配置文件中读取值注入
     */
    private void injectFields(Object bean) throws IllegalAccessException {
        Class<?> clazz = bean.getClass();
        for (Field field:clazz.getDeclaredFields()){
            //处理@Autowired
            if(field.isAnnotationPresent(Autowired.class)){
                Object dependency = getBean(field.getType());
                field.setAccessible(true);  // 突破 private 限制
                //例如：
                //这个 field 只是描述了 UserController 有一个叫 userService 的字段
                //但它不知道是哪个 UserController 实例的,所以在set的时候需要传一个实例
                field.set(bean,dependency);
                System.out.println("注入 @Autowired: " + clazz.getSimpleName()
                        + "." + field.getName() + " <- " + dependency.getClass().getSimpleName());
            }
            //处理@Value
            if(field.isAnnotationPresent(Value.class)){
                Value valueAnnotation = field.getAnnotation(Value.class);
                String placeholder = valueAnnotation.value();
                String key = placeholder.substring(placeholder.indexOf("{") + 1, placeholder.indexOf("}"));
                String value = properties.getProperty(key);
                if(value==null){
                    continue;
                }
                field.setAccessible(true);
                field.set(bean,convertValue(value,field.getType()));
            }
        }

    }

    /**
     * 字符串转目标类型（简化版，支持基本类型和 String）
     */
    private Object convertValue(String value, Class<?> targetType) {
        if (targetType == String.class) {
            return value;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        }
        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        }
        // 其他类型直接返回字符串
        return value;
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
