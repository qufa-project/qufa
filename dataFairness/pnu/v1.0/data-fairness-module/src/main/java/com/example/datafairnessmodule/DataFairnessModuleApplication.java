package com.example.datafairnessmodule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({com.example.datafairnessmodule.property.FileStorageProperties.class})
public class DataFairnessModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataFairnessModuleApplication.class, args);
    }

}
