package org.jmouse.core.access.descriptor.structured;

import org.jmouse.core.access.descriptor.structured.bean.JavaBeanDescriptor;
import org.jmouse.core.access.descriptor.structured.bean.JavaBeanIntrospector;
import org.jmouse.core.access.descriptor.structured.map.MapDescriptor;
import org.jmouse.core.access.descriptor.structured.map.MapIntrospector;
import org.jmouse.core.access.descriptor.structured.record.ValueObjectDescriptor;
import org.jmouse.core.access.descriptor.structured.record.ValueObjectIntrospector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
public final class DescriptorResolver {

    private final static ConcurrentHashMap<Class<?>, ObjectDescriptor<?>> CACHE     = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Integer, MapDescriptor<?, ?>>  MAP_CACHE = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> ObjectDescriptor<T> describe(Class<T> type) {
        return type.isRecord() ? (ObjectDescriptor<T>) ofRecordType((Class<? extends Record>) type) : ofBeanType(type);
    }

    public static <T> JavaBeanDescriptor<T> ofBeanType(Class<T> type) {
        return (JavaBeanDescriptor<T>) CACHE.computeIfAbsent(
                type, t -> new JavaBeanIntrospector<>(type).introspect().toDescriptor()
        );
    }

    public static <R extends Record> ValueObjectDescriptor<R> ofRecordType(Class<R> type) {
        return (ValueObjectDescriptor<R>) CACHE.computeIfAbsent(
                type, t -> new ValueObjectIntrospector<>(type).introspect().toDescriptor()
        );
    }

    public static <K, V> MapDescriptor<K, V> ofMap(Map<K, V> map) {
        return (MapDescriptor<K, V>) MAP_CACHE.computeIfAbsent(
                map.hashCode(), ignore -> new MapIntrospector<>(map).introspect().toDescriptor()
        );
    }

}
