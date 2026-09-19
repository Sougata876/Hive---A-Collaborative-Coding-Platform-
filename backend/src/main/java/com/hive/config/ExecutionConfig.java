package com.hive.config;

import com.hive.execution.ExecutionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ExecutionProperties.class)
public class ExecutionConfig {
}
