package main;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import config.ConfigProperty;

/**
 *
 * 创建人  liangsong
 * 创建时间 2018/11/17 16:55
 */
public class Log2DBMain {


    /**
     * 平台ID 截止时间
     * @param args
     */
    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        properties.load(new FileInputStream(new File("configuration.property")));
        ConfigProperty configProperty = new ConfigProperty(properties);
        new LogParser(args, configProperty);
        

    }
}
