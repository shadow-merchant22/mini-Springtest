package org.example.core;

/**
 * Bean 的定义信息：描述一个 Bean 是什么、怎么创建
 */
public class BeanDefinition {
    /** Bean 对应的类 */
    private Class<?> beanClass;

    /** Bean 的名字 */
    private String beanName;

    /** 作用域：singleton 或 prototype */
    private String scope = "singleton";

    // ============ Getters and Setters ============

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isSingleton() {
        return "singleton".equals(scope);
    }

    @Override
    public String toString() {
        return "BeanDefinition{" +
                "beanClass=" + beanClass.getName() +
                ", beanName='" + beanName + '\'' +
                ", scope='" + scope + '\'' +
                '}';
    }
}
