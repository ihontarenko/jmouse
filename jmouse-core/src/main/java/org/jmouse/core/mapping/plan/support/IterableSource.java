package org.jmouse.core.mapping.plan.support;

import java.util.Iterator;
import java.util.OptionalInt;

public interface IterableSource {

    Iterator<?> iterator();

    /**
     * Known size if cheap/available.
     */
    OptionalInt knownSize();

    /**
     * Optional indexed access when size is known and random access is cheap.
     */
    default Object get(int index) {
        throw new UnsupportedOperationException();
    }

    default boolean isIndexed() {
        return false;
    }
}
