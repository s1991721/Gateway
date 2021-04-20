package com.jef.gateway.loadbalancer.gray.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: Jef
 * @Date: 2021/4/20 16:36
 */
public class WeightMeta<T> extends ArrayList<Map.Entry<T, Float>> {

    public WeightMeta(List<Map.Entry<T, Float>> list) {
        super(list);
    }

    public T random() {
        return get(0).getKey();
    }
}
