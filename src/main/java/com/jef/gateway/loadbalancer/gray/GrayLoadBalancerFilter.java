package com.jef.gateway.loadbalancer.gray;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * @Author: Jef
 * @Date: 2021/4/20 16:36
 */
@Slf4j
public class GrayLoadBalancerFilter implements GlobalFilter, Ordered {

    private final LoadBalancerClientFactory clientFactory;
    private LoadBalancerProperties properties;

    public GrayLoadBalancerFilter(LoadBalancerClientFactory clientFactory, LoadBalancerProperties properties) {
        this.clientFactory = clientFactory;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = (URI) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
//        设置scheme进行特性跳转
        String schemePrefix = (String) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR);
        if (url != null /**&& ("grayLb".equals(url.getScheme()) || "grayLb".equals(schemePrefix))*/) {
            ServerWebExchangeUtils.addOriginalRequestUrl(exchange, url);
            if (log.isTraceEnabled()) {
                log.trace(ReactiveLoadBalancerClientFilter.class.getSimpleName() + " url before: " + url);
            }

            return this.choose(exchange).doOnNext((response) -> {
                if (!response.hasServer()) {
                    throw NotFoundException.create(/**this.properties.isUse404()*/false, "Unable to find instance for " + url.getHost());
                } else {
                    URI uri = exchange.getRequest().getURI();
                    String overrideScheme = null;
                    if (schemePrefix != null) {
                        overrideScheme = url.getScheme();
                    }

                    DelegatingServiceInstance serviceInstance = new DelegatingServiceInstance((ServiceInstance) response.getServer(), overrideScheme);
                    URI requestUrl = this.reconstructURI(serviceInstance, uri);
                    if (log.isTraceEnabled()) {
                        log.trace("LoadBalancerClientFilter url chosen: " + requestUrl);
                    }

                    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, requestUrl);
                }
            }).then(chain.filter(exchange));
        } else {
            return chain.filter(exchange);
        }
    }

    private Mono<Response<ServiceInstance>> choose(ServerWebExchange exchange) {
        URI uri = (URI) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        GrayLoadBalancer loadBalancer = new GrayLoadBalancer(clientFactory.getLazyProvider(uri.getHost(), ServiceInstanceListSupplier.class), uri.getHost());
        if (loadBalancer == null) {
            throw new NotFoundException("No loadbalancer available for " + uri.getHost());
        } else {
            return loadBalancer.choose(this.createRequest(exchange));
        }
    }

    private Request createRequest(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        Request<HttpHeaders> request = new DefaultRequest<>(headers);
        return request;
    }

    protected URI reconstructURI(ServiceInstance serviceInstance, URI original) {
        return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
    }

    @Override
    public int getOrder() {
        return 10150;
    }
}
