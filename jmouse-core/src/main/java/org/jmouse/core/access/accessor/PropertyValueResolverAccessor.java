package org.jmouse.core.access.accessor;

import org.jmouse.core.access.AbstractAccessor;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.PropertyValueResolver;
import org.jmouse.core.access.UnsupportedOperationException;

/**
 * An ObjectAccessor implementation that wraps a {@link PropertyValueResolver}.
 * <p>
 * This accessor delegates property retrieval and setting to the underlying
 * {@link PropertyValueResolver} instance. It allows accessing properties via names,
 * but does not support indexed access.
 * </p>
 */
public class PropertyValueResolverAccessor extends AbstractAccessor {

    /**
     * Constructs a new PropertyValueResolverAccessor for the specified source.
     * <p>
     * The source must be an instance of {@link PropertyValueResolver} and is wrapped
     * by this accessor.
     * </p>
     *
     * @param source the {@link PropertyValueResolver} to wrap
     */
    public PropertyValueResolverAccessor(PropertyValueResolver source) {
        super(source);
    }

    /**
     * Retrieves a nested ObjectAccessor by property name.
     * <p>
     * This method delegates to the underlying {@link PropertyValueResolver} to retrieve
     * the property value associated with the given name and then wraps the result into an
     * ObjectAccessor.
     * </p>
     *
     * @param name the name of the nested property to retrieve
     * @return an ObjectAccessor wrapping the retrieved property value
     */
    @Override
    public ObjectAccessor get(String name) {
        return wrap(asType(PropertyValueResolver.class).getProperty(name));
    }

    /**
     * Throws an exception because indexed access is not supported.
     *
     * @param index the index of the nested data source
     * @return never returns normally
     * @throws UnsupportedOperationException always, as indexed access is unsupported
     */
    @Override
    public ObjectAccessor get(int index) {
        throw new UnsupportedOperationException(
                "Accessor '%s' does not support indexed accessing"
                        .formatted(getClass().getName()));
    }

    /**
     * Sets a property value by property name.
     * <p>
     * Delegates the setting of the property to the underlying {@link PropertyValueResolver}.
     * </p>
     *
     * @param name  the name of the property to set
     * @param value the value to assign to the property
     */
    @Override
    public void set(String name, Object value) {
        asType(PropertyValueResolver.class).setProperty(name, value);
    }

    /**
     * Throws an exception because indexed access is not supported.
     *
     * @param index the index of the property to set
     * @param value the value to set
     * @throws UnsupportedOperationException always, as indexed access is unsupported
     */
    @Override
    public void set(int index, Object value) {
        throw new UnsupportedOperationException(
                "Accessor '%s' does not support indexed accessing"
                        .formatted(getClass().getName()));
    }
}
