package org.example.util;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * 类扫描工具：扫描指定包下所有类
 */
public class ClassScanner {
    public static Set<Class<?>> scan(String basePackage) {
        Set<Class<?>> classes = new HashSet<>();

        try {
            // 1. 包名转路径：com.example.demo → com/example/demo
            String path = basePackage.replace('.', '/');

            // 2. 获取类加载器，找到该路径下的所有资源
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(path);

            // 3. 遍历资源（可能是文件目录，也可能是 jar 包）
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                if ("file".equals(resource.getProtocol())) {
                    // 文件系统中的目录
                    File dir = new File(resource.toURI());
                    scanDirectory(dir, basePackage, classes);
                }
                // 简化版暂时不处理 jar 包中的类
            }
        } catch (Exception e) {
            throw new RuntimeException("包扫描失败: " + basePackage, e);
        }

        return classes;
    }

    /**
     * 递归扫描目录
     */
    private static void scanDirectory(File dir, String packageName, Set<Class<?>> classes) {
        if (dir == null || !dir.exists()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归扫描子目录，包名拼接子目录名
                String subPackage = packageName + "." + file.getName();
                scanDirectory(file, subPackage, classes);
            } else if (file.getName().endsWith(".class")) {
                // 处理 .class 文件
                String className = file.getName().replace(".class", "");
                String fullClassName = packageName + "." + className;

                try {
                    classes.add(Class.forName(fullClassName));
                } catch (ClassNotFoundException e) {
                    // 类加载失败，忽略
                    System.err.println("无法加载类: " + fullClassName);
                }
            }
        }
    }
}
