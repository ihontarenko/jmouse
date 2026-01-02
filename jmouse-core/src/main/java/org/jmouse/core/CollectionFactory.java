package org.jmouse.core;

import java.lang.reflect.Constructor;
import java.util.*;

public final class CollectionFactory {

    public static final int DEFAULT_CAPACITY = 16;

    private CollectionFactory() {}

    public static <E, C extends Collection<E>> C createCollection(Class<C> collectionType, int initialCapacity) {
        Contract.nonNull(collectionType, "collectionType");
        Contract.state(initialCapacity >= 0, "argument: initialCapacity");

        if (collectionType == List.class || collectionType == Collection.class) {
            return cast(new ArrayList<>(initialCapacity));
        }
        if (collectionType == Set.class) {
            return cast(new LinkedHashSet<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
        }
        if (collectionType == SortedSet.class || collectionType == NavigableSet.class) {
            return cast(new TreeSet<>());
        }
        if (collectionType == Queue.class || collectionType == Deque.class) {
            return cast(new ArrayDeque<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
        }

        Contract.state(!EnumSet.class.isAssignableFrom(collectionType), "use createEnumSet(enumType)");

        if (ArrayList.class.isAssignableFrom(collectionType)) {
            return cast(new ArrayList<>(initialCapacity));
        }
        if (LinkedList.class.isAssignableFrom(collectionType)) {
            return cast(new LinkedList<>());
        }
        if (HashSet.class.isAssignableFrom(collectionType)) {
            return cast(new HashSet<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
        }
        if (LinkedHashSet.class.isAssignableFrom(collectionType)) {
            return cast(new LinkedHashSet<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
        }
        if (TreeSet.class.isAssignableFrom(collectionType)) {
            return cast(new TreeSet<>());
        }
        if (ArrayDeque.class.isAssignableFrom(collectionType)) {
            return cast(new ArrayDeque<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
        }

        if (!Collection.class.isAssignableFrom(collectionType)) {
            throw new IllegalArgumentException("Not a Collection type: " + collectionType.getName());
        }

        return instantiateCollection(collectionType);
    }

    public static <E, C extends Collection<E>> C createCollection(Class<C> collectionType) {
        return createCollection(collectionType, 0);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E, C extends Collection<E>> C createLike(
            Collection<?> source, int initialCapacity, Class<C> targetType) {
        Contract.nonNull(source, "source");
        Contract.nonNull(targetType, "targetType");

        if (targetType != Collection.class) {
            return createCollection(targetType, initialCapacity);
        }

        return switch (source) {
            case EnumSet<?> enumSet -> {
                Class<?> enumType = enumSet.isEmpty() ? null : enumSet.iterator().next().getDeclaringClass();
                if (enumType == null) {
                    yield cast(new LinkedHashSet<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
                }
                yield (C) cast(EnumSet.noneOf((Class) enumType));
            }
            case SortedSet<?> ignored1 -> cast(new TreeSet<>());
            case Set<?> ignored -> cast(new LinkedHashSet<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
            case Queue<?> ignored -> cast(new ArrayDeque<>(Math.max(DEFAULT_CAPACITY, initialCapacity)));
            default -> cast(new ArrayList<>(initialCapacity));
        };
    }

    public static <E extends Enum<E>> EnumSet<E> createEnumSet(Class<E> enumType) {
        return EnumSet.noneOf(Contract.nonNull(enumType, "enumType"));
    }

    private static <E, C extends Collection<E>> C instantiateCollection(Class<C> collectionType) {
        try {
            Constructor<C> constructor = collectionType.getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException(
                    "Cannot instantiate collection type: %s (no accessible default constructor?)"
                            .formatted(collectionType.getName()),
                    exception
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static <E, C extends Collection<E>> C cast(Collection<E> collection) {
        return (C) collection;
    }
}
