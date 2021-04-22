package com.jef.gateway.loadbalancer.iphash;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

/**
 * @Author: Jef
 * @Date: 2021/4/20 16:36
 */
@Slf4j
public class IPHashLoadBalancerFilter implements GlobalFilter, Ordered {

    private final LoadBalancerClientFactory clientFactory;

    private LoadBalancerProperties properties;

    public IPHashLoadBalancerFilter(LoadBalancerClientFactory clientFactory, LoadBalancerProperties properties) {
        this.clientFactory = clientFactory;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        //是否开启IPhash开关
        boolean open = true;

        if (!open) {
            return chain.filter(exchange);
        }

        URI url = (URI) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = (String) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR);
        return choose(exchange).doOnNext(response -> {

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
    }

    private Mono<Response<ServiceInstance>> choose(ServerWebExchange exchange) {
        URI uri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        IPHashLoadBalancer loadBalancer = new IPHashLoadBalancer(clientFactory.getLazyProvider(uri.getHost(), ServiceInstanceListSupplier.class), uri.getHost());
        if (loadBalancer == null) {
            throw new NotFoundException("No loadbalancer available for " + uri.getHost());
        }
        return loadBalancer.choose(createRequest(exchange));
    }

    private Request createRequest(ServerWebExchange exchange) {
        String ip = getIPAddress(exchange.getRequest());
        Request<String> request = new DefaultRequest<>(ip);
        return request;
    }

    public static String getIPAddress(ServerHttpRequest request) {
        String ip = null;

        HttpHeaders headers=request.getHeaders();

        //X-Forwarded-For：Squid 服务代理
        List<String> ipAddresses = headers.get("X-Forwarded-For");
        if (ipAddresses == null || ipAddresses.size() == 0) {
            //Proxy-Client-IP：apache 服务代理
            ipAddresses = headers.get("Proxy-Client-IP");
        }
        if (ipAddresses == null || ipAddresses.size() == 0 ) {
            //WL-Proxy-Client-IP：weblogic 服务代理
            ipAddresses = headers.get("WL-Proxy-Client-IP");
        }
        if (ipAddresses == null || ipAddresses.size() == 0 ) {
            //HTTP_CLIENT_IP：有些代理服务器
            ipAddresses = headers.get("HTTP_CLIENT_IP");
        }
        if (ipAddresses == null || ipAddresses.size() == 0 ) {
            //X-Real-IP：nginx服务代理
            ipAddresses = headers.get("X-Real-IP");
        }

        //有些网络通过多层代理，那么获取到的ip就会有多个，一般都是通过逗号（,）分割开来，并且第一个ip为客户端的真实IP
        if (ipAddresses != null && ipAddresses.size() != 0) {
            ip = ipAddresses.get(0);
        }

        //还是不能获取到，最后再通过request.getRemoteAddr();获取
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        }
        return ip.equals("0:0:0:0:0:0:0:1")?"127.0.0.1":ip;
    }

    protected URI reconstructURI(ServiceInstance serviceInstance, URI original) {
        return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
    }

    @Override
    public int getOrder() {
        return 10150;
    }
}
