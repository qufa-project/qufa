package com.example.datafairnessmodule.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MainDataSourceConfig {

	@Bean
	@ConfigurationProperties(prefix = "spring.main.datasource")
	public DataSource mainDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean
	public SqlSessionFactory mainSqlSessionFactory(DataSource mainDataSource, ApplicationContext applicationContext)
			throws Exception {
		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setDataSource(mainDataSource);
		sqlSessionFactoryBean.setTypeAliasesPackage("com.example.datafairnessmodule.domain");
		sqlSessionFactoryBean.setMapperLocations(applicationContext.getResources("classpath:mybatis/*.xml"));
		return sqlSessionFactoryBean.getObject();
	}

	@Bean
	public SqlSessionTemplate mainSqlSessionTemplate(SqlSessionFactory mainSqlSessionFactory) throws Exception {
		return new SqlSessionTemplate(mainSqlSessionFactory);
	}
}
