package com.jef.gateway.loadbalancer.iphash;

import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @Author: Jef
 * @Date: 2021/4/20 16:36
 */
@Configuration
public class IPHashConfiguration {

    @Bean
    public IPHashLoadBalancerFilter ipHashLoadBalancerClientFilter(
            LoadBalancerClientFactory clientFactory, LoadBalancerProperties properties) {
        return new IPHashLoadBalancerFilter(clientFactory, properties);
    }

}
