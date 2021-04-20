package com.jef.gateway.loadbalancer.iphash;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @Author: Jef
 * @Date: 2021/4/20 16:36
 */
@Slf4j
public class IPHashLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    public ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    public String serviceId;

    public IPHashLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId = serviceId;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        if (serviceInstanceListSupplierProvider != null) {
            ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
                    .getIfAvailable(NoopServiceInstanceListSupplier::new);
            return supplier.get().next().map((instances) -> getInstanceResponse(request, instances));
        }
        return null;
    }

    private Response<ServiceInstance> getInstanceResponse(Request request, List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            log.warn("No servers available for service: " + this.serviceId);
            return new EmptyResponse();
        }
        String ip = (String) request.getContext();
        int hash = Math.abs(ip.hashCode());
        int pos = hash % instances.size();
        ServiceInstance instance = instances.get(pos);
        return new DefaultResponse(instance);
    }
}
