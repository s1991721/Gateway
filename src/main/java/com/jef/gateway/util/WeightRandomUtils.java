package com.jef.gateway.util;

import org.springframework.cloud.client.ServiceInstance;

import java.util.*;

public class WeightRandomUtils {

    public static WeightMeta<ServiceInstance> buildWeightMeta(Map<ServiceInstance, Float> weightMap) {
        List<Map.Entry<ServiceInstance, Float>> list = new ArrayList<Map.Entry<ServiceInstance, Float>>(weightMap.entrySet());
        Collections.sort(list,new Comparator<Map.Entry<ServiceInstance, Float>>() {
            //升序排序
            @Override
            public int compare(Map.Entry<ServiceInstance, Float> o1,
                               Map.Entry<ServiceInstance, Float> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }

        });
        return new WeightMeta(list);
    }

}
