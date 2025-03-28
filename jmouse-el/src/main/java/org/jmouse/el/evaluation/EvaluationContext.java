package org.jmouse.el.evaluation;

import org.jmouse.core.bind.*;
import org.jmouse.core.convert.Conversion;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.rendering.TemplateStack;

/**
 * ⚙️ Represents the evaluation context for expression evaluation.
 * <p>
 * Provides the necessary services for expression evaluation such as variable scopes,
 * extensions, conversion, and property resolution via a standard {@link ObjectAccessor}
 * wrapped by a default {@link ObjectAccessorWrapper}.
 * </p>
 */
public interface EvaluationContext extends VirtualPropertyResolver.Aware {

    TemplateStack getInheritance();

    /**
     * The default ObjectAccessorWrapper used to wrap object access operations.
     */
    ObjectAccessorWrapper WRAPPER = new StandardAccessorWrapper();

    /**
     * Retrieves an object from the context using the specified key.
     *
     * @param key the key identifying the object
     * @return the corresponding object, or {@code null} if not found
     */
    Object getObject(Object key);

    /**
     * Sets an object in the context for the specified key.
     *
     * @param key    the key identifying where the object is stored
     * @param object the object to store
     */
    void setObject(Object key, Object object);

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
     * Retrieves the value associated with the specified property name.
     * <p>
     * Delegates to the PropertyValueResolver to obtain the property value.
     * </p>
     *
     * @param name the property name or path
     * @return the resolved value, or {@code null} if not found
     */
    default Object getValue(String name) {
        return getValueResolver().getProperty(name);
    }

    /**
     * Sets the value associated with the specified property name.
     * <p>
     * Delegates to the PropertyValueResolver to inject the value.
     * </p>
     *
     * @param name  the property name or path
     * @param value the value to set
     */
    default void setValue(String name, Object value) {
        getValueResolver().setProperty(name, value);
    }

    /**
     * Retrieves an ObjectAccessor configured with the current scope chain.
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
     * from the evaluation context.
     * </p>
     *
     * @return a PropertyValueResolver for resolving property values
     */
    default PropertyValueResolver getValueResolver() {
        return new ExpressionLanguageValuesResolver(getValueAccessor(), getVirtualProperties());
    }
}
