package org.jmouse.el.evaluation;

import org.jmouse.core.bind.*;
import org.jmouse.core.convert.Conversion;
import org.jmouse.el.extension.ExtensionContainer;

/**
 * Represents the evaluation context used for expression evaluation.
 * <p>
 * Provides the scope chain, extensions, and conversion services required during evaluation.
 * Also supplies helper methods for retrieving and setting values via a standard
 * {@link ObjectAccessor} wrapped by a default {@link ObjectAccessorWrapper}.
 * </p>
 */
public interface EvaluationContext extends VirtualPropertyResolver.Aware {

    /**
     * The default ObjectAccessorWrapper used to wrap object access operations.
     */
    ObjectAccessorWrapper WRAPPER = new StandardAccessorWrapper();

    /**
     * Returns the current scope chain for variable resolution.
     *
     * @return the current {@link ScopedChain} instance
     */
    ScopedChain getScopedChain();

    /**
     * Returns the container holding custom extensions for expression evaluation.
     *
     * @return the {@link ExtensionContainer} instance
     */
    ExtensionContainer getExtensions();

    /**
     * Returns the conversion service for converting values between types.
     *
     * @return the {@link Conversion} instance
     */
    Conversion getConversion();

    /**
     * Retrieves the value for the specified property name.
     * <p>
     * Delegates to the value resolver to obtain the property value.
     * </p>
     *
     * @param name the property name or path
     * @return the resolved value, or {@code null} if not found
     */
    default Object getValue(String name) {
        return getValueResolver().getProperty(name);
    }

    /**
     * Sets the value for the specified property name.
     * <p>
     * Delegates to the value resolver to inject the value.
     * </p>
     *
     * @param name  the property name or path
     * @param value the value to set
     */
    default void setValue(String name, Object value) {
        getValueResolver().setProperty(name, value);
    }

    /**
     * Retrieves an ObjectAccessor configured with the current scoped chain.
     * <p>
     * Creates a new {@link ScopedChainValuesAccessor} based on the current scope chain,
     * sets the default wrapper, and returns it.
     * </p>
     *
     * @return an ObjectAccessor for accessing scoped values
     */
    default ObjectAccessor getValueAccessor() {
        ObjectAccessor accessor = new ScopedChainValuesAccessor(getScopedChain());
        ((ObjectAccessorWrapper.Aware) accessor).setWrapper(WRAPPER);
        return accessor;
    }

    /**
     * Returns the PropertyValueResolver that resolves properties, including virtual ones.
     * <p>
     * It is constructed using the current ObjectAccessor and the VirtualPropertyResolver
     * obtained from the EvaluationContext.
     * </p>
     *
     * @return a PropertyValueResolver for resolving property values
     */
    default PropertyValueResolver getValueResolver() {
        return new ExpressionLanguageValuesResolver(getValueAccessor(), getVirtualProperties());
    }
}
