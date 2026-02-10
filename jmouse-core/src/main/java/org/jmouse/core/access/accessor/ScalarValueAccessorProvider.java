package org.jmouse.core.access.accessor;

import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ObjectAccessorProvider;
import org.jmouse.core.reflection.TypeInformation;

public class ScalarValueAccessorProvider implements ObjectAccessorProvider {

    /**
     * Checks if this valueProvider supports the given source object.
     *
     * @param source the object to be wrapped
     * @return {@code true} if this valueProvider can create an ObjectAccessor for the source; {@code false} otherwise
     */
    @Override
    public boolean supports(Object source) {
        return TypeInformation.forInstance(source).isScalar();
    }

    /**
     * Creates an {@link ObjectAccessor} instance for the given source object.
     *
     * @param source the object to wrap
     * @return an ObjectAccessor instance wrapping the specified source
     */
    @Override
    public ObjectAccessor create(Object source) {
        return new ScalarValueAccessor(source);
    }

}
