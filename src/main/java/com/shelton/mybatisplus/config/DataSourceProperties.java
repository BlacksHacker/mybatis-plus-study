package com.shelton.mybatisplus.config;

import com.zaxxer.hikari.HikariConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName DataSourceProperties
 * @Description
 * @Author xiaosheng1.li
 **/
@ConfigurationProperties(prefix = "spring.datasource")
@Configuration
@Data
public class DataSourceProperties {

    private HikariConfig mybatisPlusOne;

    private HikariConfig mybatisPlusTwo;
}
