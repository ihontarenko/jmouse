package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.GenericConverter;

import java.util.*;

import static org.jmouse.core.convert.GenericConverter.of;

public class CollectionConverters {

    public static Set<GenericConverter<?, ?>> getConverters() {
        return Set.of(
                of(Object[].class, List.class, List::of),
                of(List.class, Object[].class, List::toArray),
                of(Object[].class, Set.class, Set::of),
                of(Set.class, List.class, ArrayList::new),
                of(List.class, Set.class, HashSet::new)
        );
    }

}
