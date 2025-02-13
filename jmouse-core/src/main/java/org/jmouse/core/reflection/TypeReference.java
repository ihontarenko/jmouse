package org.jmouse.core.reflection;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A generic type reference that captures and retains generic type information
 * at runtime via the super type's type parameter. This is particularly useful
 * for scenarios where Java's type erasure would otherwise remove generic type
 * details, such as deserialization frameworks or reflection-based utilities.
 *
 * <p>Usage binder:
 * <pre>{@code
 * // Create an anonymous subclass of TypeReference with a specific generic type
 * TypeReference<List<String>> typeRef = new TypeReference<>() {};
 *
 * // Retrieve the captured type
 * Type type = typeRef.getType();  // Represents List<String>
 * }</pre>
 *
 * <p>By extending {@code TypeReference<T>} with an anonymous subclass, the generic type T
 * is preserved in the {@code type} field. If you do not provide any actual type information,
 * you will get an {@link IllegalArgumentException} indicating that no generic type was captured.</p>
 *
 * @param <T> the generic type to be captured
 */
public abstract class TypeReference<T> {

    /**
     * The captured generic type, resolved during instantiation of the
     * anonymous subclass.
     */
    protected final Type type;

    /**
     * Constructs a new {@code TypeReference}, capturing the generic type T
     * from the subclass's declaration. Throws an {@link IllegalArgumentException}
     * if no actual type information was provided.
     */
    protected TypeReference() {
        Type superClass = this.getClass().getGenericSuperclass();
        if (superClass instanceof Class) {
            throw new IllegalArgumentException("CONSTRUCTED WITHOUT ACTUAL TYPE INFORMATION");
        } else {
            this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        }
    }

    /**
     * Returns the captured {@link Type} for the generic parameter T.
     *
     * @return the captured type
     */
    public Type getType() {
        return this.type;
    }

}
