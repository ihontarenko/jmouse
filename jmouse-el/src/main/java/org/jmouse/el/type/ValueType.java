package org.jmouse.el.type;

import org.jmouse.core.convert.Conversion;

/**
 * 🏷️ A type a declaration may be annotated with — {@code let count : int = 1} —: a name a document writes, the Java
 * class a value of it is carried as, and what a value bound to it must be.
 *
 * <p>A type is registered by the host, through an {@link org.jmouse.el.extension.Extension}, never named by a
 * document: {@code int => java.lang.Integer} is the core's line, written in Java. A document only refers to a type by
 * its name, so a saved text can never make the engine load a class.</p>
 *
 * <p>A domain may carry a value as a primitive and still say what it is — {@code a : current} is a {@code double} that
 * is a current —: such a type is its own implementation, registered by the domain's extension.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ValueType {

    /** The name a document writes after the colon. */
    String name();

    /** The Java class a value of this type is carried as. */
    Class<?> carrier();

    /**
     * The value as this type carries it: as it is when the carrier already holds it, else converted by the
     * language's {@code conversion}. Nothing — a declaration without a value — is admitted as nothing.
     *
     * @throws ValueTypeException when the value is not of this type and does not convert to it
     */
    default Object admit(Object value, Conversion conversion) {
        if (value == null || carrier().isInstance(value)) {
            return value;
        }
        Object converted;
        try {
            converted = conversion.convert(value, carrier());
        } catch (RuntimeException unconvertible) {
            throw ValueTypeException.refused(this, value, unconvertible);
        }
        if (!carrier().isInstance(converted)) {
            throw ValueTypeException.refused(this, value, null);
        }
        return converted;
    }
}
