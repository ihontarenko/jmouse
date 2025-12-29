package org.jmouse.core.bind;

import org.jmouse.core.convert.Conversion;

/**
 * Provides the context for data binding operations.
 * This interface encapsulates the binding environment,
 * including data sources, type conversion, and binding depth settings.
 */
public interface BindContext {

    /**
     * Returns the {@link ObjectAccessor} associated with this binding context.
     * The data source provides the raw input for binding operations.
     *
     * @return the data source
     */
    ObjectAccessor getObjectAccessor();

    void setObjectAccessor(ObjectAccessor objectAccessor);

    /**
     * Returns the root {@link ObjectBinder} that manages the binding process.
     * This binder is responsible for handling various binding strategies.
     *
     * @return the root object binder
     */
    ObjectBinder getRootBinder();

    /**
     * Checks if deep binding is enabled.
     * Deep binding allows nested structures to be recursively processed.
     *
     * @return {@code true} if deep binding is enabled, otherwise {@code false}
     */
    boolean isDeepBinding();

    /**
     * Checks if shallow binding is enabled.
     * Shallow binding limits binding operations to the first level of properties.
     *
     * @return {@code true} if shallow binding is enabled, otherwise {@code false}
     */
    boolean isShallowBinding();

    void useShallowBinding();

    void useDeepBinding();

    /**
     * Returns the {@link Conversion} instance used for type conversion.
     * This allows the transformation of values from one type to another
     * during the binding process.
     *
     * @return the conversion instance
     */
    Conversion getConversion();
}
