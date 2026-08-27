package org.example.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {
    public static Properties load(String fileName){
        Properties properties=new Properties();
        try (InputStream resourceAsStream = PropertiesLoader.class.getClassLoader().getResourceAsStream(fileName)){
            if(resourceAsStream==null){
                throw new RuntimeException("找不到配置文件");
            }
            properties.load(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException("加载配置文件失败："+fileName,e);
        }
        return properties;
    }
}
