package org.jmouse.core.bind;

import org.jmouse.core.reflection.TypeInformation;

/**
 * An {@link ObjectAccessorProvider} implementation that supports Java Bean objects.
 * <p>
 * This provider checks whether a given source object is a Java Bean using its type information.
 * If the source is identified as a bean, it creates and returns a corresponding {@link ObjectAccessor}
 * instance (specifically, a {@link JavaBeanAccessor}).
 * </p>
 */
public class JavaBeanAccessorProvider implements ObjectAccessorProvider {

    /**
     * Checks if the provided source object is a Java Bean.
     *
     * @param source the object to check
     * @return {@code true} if the source is recognized as a Java Bean; {@code false} otherwise
     */
    @Override
    public boolean supports(Object source) {
        return TypeInformation.forInstance(source).isBean();
    }

    /**
     * Creates an {@link ObjectAccessor} for the given Java Bean source.
     *
     * @param source the Java Bean object to wrap
     * @return a new {@link JavaBeanAccessor} instance wrapping the specified source
     */
    @Override
    public ObjectAccessor create(Object source) {
        return new JavaBeanAccessor(source);
    }
}
