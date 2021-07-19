package com.QuFa.profiler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("spring")
@Getter
@Setter
public class ActiveProfileProperty {
    private String profiles;

    public String getActive() {
        return this.profiles;
    }
}