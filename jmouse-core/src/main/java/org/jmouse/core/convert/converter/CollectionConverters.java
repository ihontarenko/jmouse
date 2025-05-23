package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.GenericConverter;

import java.util.*;

import static org.jmouse.core.convert.GenericConverter.of;

public class CollectionConverters {

    public static Set<GenericConverter<?, ?>> getConverters() {
        return Set.of(
                of(Object[].class, List.class, List::of),
                of(List.class, Object[].class, List::toArray),
                of(Map.class, Object[].class, source -> source.values().toArray()),
                of(Map.class, List.class, source -> new ArrayList<>(source.values())),
                of(Object[].class, Set.class, Set::of),
                of(Set.class, List.class, ArrayList::new),
                of(List.class, Set.class, HashSet::new),
                of(String.class, String[].class, value -> new String[]{value}),
                of(String.class, List.class, List::of),
                of(String.class, Set.class, Set::of)
        );
    }

}
