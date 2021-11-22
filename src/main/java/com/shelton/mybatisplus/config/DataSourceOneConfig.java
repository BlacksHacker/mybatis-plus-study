package com.shelton.mybatisplus.config;

import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.annotation.Resource;

/**
 * @ClassName DataSourceConfig
 * @Description
 * @Author xiaosheng1.li
 **/
@Configuration
@MapperScan(basePackages = "com.shelton.mybatisplus.mapper", sqlSessionFactoryRef = "mybatisPlusOneSqlSessionFactory")
public class DataSourceOneConfig {

    @Resource
    DataSourceProperties dataSourceProperties;

    @Bean(name = "mybatisPlusOneSqlSessionFactory")
    public SqlSessionFactory mybatisPlusSqlSessionFactory() throws Exception {
        HikariDataSource hikariDataSource = new HikariDataSource(dataSourceProperties.getMybatisPlusOne());
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(hikariDataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:/mapper/mybatisplusone/*.xml"));
        factoryBean.setTypeAliasesPackage("com.shelton.mybatisplus.entity");
        factoryBean.setPlugins(new MybatisPlusInterceptor());
        factoryBean.setGlobalConfig(GlobalConfigUtils.defaults());
        return factoryBean.getObject();
    }
}
