package com.QuFa.profiler;

import org.datacleaner.beans.StringAnalyzerResult;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.text.DecimalFormat;
import java.util.Locale;

@SpringBootApplication
public class ProfilerApplication{

	public static void main(String[] args) {
		SpringApplication.run(ProfilerApplication.class, args);
	}
}
