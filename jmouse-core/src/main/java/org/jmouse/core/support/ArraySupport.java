package org.jmouse.core.support;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

final public class ArraySupport {

    private ArraySupport() {}

    public static Collection<?> toCollection(Object object) {
        if (object == null) {
            return List.of();
        }

        if (object instanceof Collection<?> collection) {
            return collection;
        }

        if (object instanceof Iterator<?> iterator) {
            @SuppressWarnings("unchecked")
            Iterable<?> iterable = () -> (Iterator<Object>) iterator;
            object = iterable;
        }

        if (object instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false).toList();
        }

        if (isArray(object)) {
            return toList(object);
        }

        if (object instanceof CharSequence charSequence) {
            return toCodePoints(charSequence.toString());
        }

        return List.of(object);
    }

    public static boolean isArray(Object object) {
        return object != null && object.getClass().isArray();
    }

    public static List<?> toList(Object array) {
        int          length = Array.getLength(array);
        List<Object> result = new ArrayList<>(length);

        for (int i = 0; i < length; i++) {
            result.add(Array.get(array, i));
        }

        return result;
    }

    public static List<String> toCodePoints(String string) {
        return string.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .collect(Collectors.toList());
    }

}
