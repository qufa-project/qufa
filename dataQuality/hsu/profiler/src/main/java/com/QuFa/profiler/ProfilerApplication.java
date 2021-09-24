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
//		DecimalFormat form = new DecimalFormat("#.#####");
//		double dNumber = 40377.24999999998;
//		double dNumber2 = 1.55510000000001;
//		System.out.println(form.format(dNumber));
//		System.out.println(form.format(dNumber2));

		SpringApplication.run(ProfilerApplication.class, args);
	}
}
