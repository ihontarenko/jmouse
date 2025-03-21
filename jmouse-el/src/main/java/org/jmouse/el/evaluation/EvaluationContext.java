package org.jmouse.el.evaluation;

import org.jmouse.core.bind.*;
import org.jmouse.core.convert.Conversion;
import org.jmouse.el.extension.ExtensionContainer;

/**
 * Represents the evaluation context used for expression evaluation.
 * <p>
 * The {@code EvaluationContext} provides the scope chain, extension container, and conversion services
 * required during expression evaluation. It also supplies helper methods for retrieving and setting
 * values using a standard {@link ObjectAccessor} wrapped by a default {@link ObjectAccessorWrapper}.
 * </p>
 */
public interface EvaluationContext extends VirtualPropertyResolver.Aware {

    /**
     * The default {@link ObjectAccessorWrapper} instance used to wrap object access operations.
     */
    ObjectAccessorWrapper WRAPPER = new StandardAccessorWrapper();

    /**
     * Returns the current scope chain used for variable resolution.
     *
     * @return the current {@link ScopedChain} instance
     */
    ScopedChain getScopedChain();

    /**
     * Returns the extension container holding custom extensions for expression evaluation.
     *
     * @return the {@link ExtensionContainer} instance
     */
    ExtensionContainer getExtensions();

    /**
     * Returns the conversion service used for converting values between types.
     *
     * @return the {@link Conversion} instance
     */
    Conversion getConversion();

    /**
     * Retrieves the value associated with the specified name.
     * <p>
     * If the name contains a dot ('.') or a bracket ('['), the value is obtained by navigating the property
     * path via the {@link ObjectAccessor}. Otherwise, if the direct lookup returns {@code null} and the scoped chain
     * contains the name, the value is retrieved from the scoped chain.
     * </p>
     *
     * @param name the property name or path
     * @return the resolved value, or {@code null} if no value is found
     */
    default Object getValue(String name) {
        return getValueResolver().getProperty(name);
    }

    /**
     * Sets the value associated with the specified name.
     * <p>
     * This method injects the given value into the underlying data source via the {@link ObjectAccessor}.
     * </p>
     *
     * @param name  the property name or path
     * @param value the value to set
     */
    default void setValue(String name, Object value) {
        getValueResolver().setProperty(name, value);
    }

    /**
     * Retrieves an {@link ObjectAccessor} configured with the current scoped chain.
     * <p>
     * This method creates a new {@link ScopedChainValuesAccessor} based on the current scoped chain,
     * sets the default wrapper, and returns it. The resulting accessor is used for accessing and manipulating
     * scoped values.
     * </p>
     *
     * @return an {@link ObjectAccessor} instance for accessing scoped values
     */
    default ObjectAccessor getValueAccessor() {
        ObjectAccessor accessor = new ScopedChainValuesAccessor(getScopedChain());
        ((ObjectAccessorWrapper.Aware) accessor).setWrapper(WRAPPER);
        return accessor;
    }

    default PropertyValueResolver getValueResolver() {
        return new ExpressionLanguageValuesResolver(getValueAccessor(), getVirtualProperties());
    }

}
