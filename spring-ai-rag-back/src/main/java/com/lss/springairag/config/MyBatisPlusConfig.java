package com.lss.springairag.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.lss.springairag.mapper")
public class MyBatisPlusConfig {
}