package com.freedraw.repository.persistence.disk.type;

import com.freedraw.repository.persistence.disk.type.impl.*;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class DataTypeRegistry {
    private static final Map<Class<?>, DataType<?>> registry = new HashMap<>();

    static {
        registry.put(Integer.class, new IntegerField());
        registry.put(Double.class, new DoubleField());
        registry.put(Float.class, new FloatField());
        registry.put(Long.class, new LongField());
        registry.put(String.class, new StringField());
        registry.put(Integer[].class, new CompositeField());
    }

    @SuppressWarnings("unchecked")
    public static <T> DataType<T> get(Class<T> clazz) {
        return (DataType<T>) registry.get(clazz);
    }
}