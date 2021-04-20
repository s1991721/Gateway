package com.jef.gateway.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WeightMeta<T> extends ArrayList<Map.Entry<T, Float>> {

    public WeightMeta(List<Map.Entry<T, Float>> list) {
        super(list);
    }

    public T random() {
        return get(0).getKey();
    }
}
