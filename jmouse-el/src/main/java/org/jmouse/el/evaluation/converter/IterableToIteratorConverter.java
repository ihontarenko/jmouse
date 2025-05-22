package org.jmouse.el.evaluation.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Iterator;
import java.util.Set;

public class IterableToIteratorConverter implements GenericConverter<Iterable<?>, Iterator<?>> {

    @Override
    public Iterator<?> convert(Iterable<?> source, Class<Iterable<?>> sourceType, Class<Iterator<?>> targetType) {
        return ((Iterable<Object>) source).iterator();
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(ClassPair.of(Iterable.class, Iterator.class));
    }

}
