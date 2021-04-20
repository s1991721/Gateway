package com.jef.gateway.loadbalancer.gray;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: Jef
 * @Date: 2021/4/20 16:36
 */
@Configuration
public class GrayConfiguration {

    @Bean
    @ConditionalOnMissingBean({GrayLoadBalancerFilter.class})
    public GrayLoadBalancerFilter grayReactiveLoadBalancerClientFilter(LoadBalancerClientFactory clientFactory, LoadBalancerProperties properties) {
        return new GrayLoadBalancerFilter(clientFactory, properties);
    }

}
