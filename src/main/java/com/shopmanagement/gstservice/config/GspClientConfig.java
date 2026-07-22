package com.shopmanagement.gstservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.shopmanagement.gstservice.compliance.GspClientProperties;

@Configuration
@EnableConfigurationProperties(GspClientProperties.class)
public class GspClientConfig {
}
