package org.jmouse.core;

import java.lang.reflect.Constructor;
import java.util.*;

public final class MapFactory {

    public static final int DEFAULT_CAPACITY = 16;

    private MapFactory() {}

    @SuppressWarnings({"unchecked"})
    public static <K, V> Map<K, V> createMap(Class<?> mapType, int initialCapacity) {
        Verify.nonNull(mapType, "mapType");
        Verify.state(initialCapacity > 0, "initialCapacity must be >= 0");

        if (mapType == Map.class) {
            return new LinkedHashMap<>(capacity(initialCapacity));
        }

        if (mapType == SortedMap.class || mapType == NavigableMap.class) {
            return new TreeMap<>();
        }

        Verify.state(Map.class.isAssignableFrom(mapType), "mapType must be Map type");
        Verify.state(!EnumMap.class.isAssignableFrom(mapType), "use createEnumMap(enumKeyType)");

        if (HashMap.class.isAssignableFrom(mapType)) {
            return new HashMap<>(capacity(initialCapacity));
        }
        if (LinkedHashMap.class.isAssignableFrom(mapType)) {
            return new LinkedHashMap<>(capacity(initialCapacity));
        }
        if (TreeMap.class.isAssignableFrom(mapType)) {
            return new TreeMap<>();
        }
        if (IdentityHashMap.class.isAssignableFrom(mapType)) {
            return new IdentityHashMap<>(capacity(initialCapacity));
        }
        if (WeakHashMap.class.isAssignableFrom(mapType)) {
            return new WeakHashMap<>(capacity(initialCapacity));
        }
        if (Hashtable.class.isAssignableFrom(mapType)) {
            return new Hashtable<>(capacity(initialCapacity));
        }

        return instantiateMap((Class<? extends Map<K, V>>) mapType);
    }

    public static <K, V> Map<K, V> createMap(Class<?> mapType) {
        return createMap(mapType, 0);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <K, V> Map<K, V> createLike(Map<?, ?> source, int initialCapacity) {
        Verify.nonNull(source, "source");
        return switch (source) {
            case EnumMap<?, ?> enumMap -> {
                Class<? extends Enum> enumType = null;

                if (!enumMap.isEmpty()) {
                    Object key = enumMap.keySet().iterator().next();
                    if (key instanceof Enum<?> e) {
                        enumType = e.getDeclaringClass();
                    }
                }

                if (enumType != null) {
                    yield ((Map<K, V>) new EnumMap(enumType));
                }

                yield new LinkedHashMap<>(capacity(initialCapacity));
            }
            case NavigableMap<?, ?> ignored -> new TreeMap<>();
            case SortedMap<?, ?> ignored -> new TreeMap<>();
            case IdentityHashMap<?, ?> ignored -> new IdentityHashMap<>(capacity(initialCapacity));
            case WeakHashMap<?, ?> ignored -> new WeakHashMap<>(capacity(initialCapacity));
            case Hashtable<?, ?> ignored -> new Hashtable<>(capacity(initialCapacity));
            default -> new LinkedHashMap<>(capacity(initialCapacity));
        };
    }

    public static <K, V> Map<K, V> createLike(Map<?, ?> source) {
        return createLike(source, 0);
    }

    public static <E extends Enum<E>, V> EnumMap<E, V> createEnumMap(Class<E> enumKeyType) {
        return new EnumMap<>(Verify.nonNull(enumKeyType, "enumKeyType"));
    }

    private static int capacity(int initialCapacity) {
        return Math.max(DEFAULT_CAPACITY, initialCapacity);
    }

    private static <K, V> Map<K, V> instantiateMap(Class<? extends Map<K, V>> mapType) {
        try {
            Constructor<? extends Map<K, V>> constructor = mapType.getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException(
                    "Cannot instantiate map type: %s (no accessible default constructor?)".formatted(
                            mapType.getName()), exception
            );
        }
    }
}
