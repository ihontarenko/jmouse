package org.jmouse.el.evaluation.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IteratorToCollection implements GenericConverter<Iterator<?>, Collection<?>> {

    @Override
    public Collection<?> convert(Iterator<?> source, Class<Iterator<?>> sourceType, Class<Collection<?>> targetType) {
        @SuppressWarnings("unchecked")
        Iterable<?> iterable = () -> (Iterator<Object>) source;
        Stream<?>   stream   = StreamSupport.stream(iterable.spliterator(), false);

        if (List.class.isAssignableFrom(targetType)) {
            return stream.toList();
        }

        return stream.collect(Collectors.toSet());
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(new ClassPair(Iterator.class, Set.class), new ClassPair(Iterator.class, List.class));
    }

}
