package org.jmouse.meterializer;

/**
 * Strategy interface for exposing an {@link ElementType}
 * from a custom model object. 🧩
 *
 * <p>
 * Implement this interface on field/view models that participate
 * in template selection. The returned {@link ElementType} is typically
 * used by the rendering engine to choose an appropriate blueprint
 * or rendering strategy.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * public final class TextFieldModel implements ElementTypeProvider {
 *
 *     @Override
 *     public ElementType elementType() {
 *         return ElementType.TEXT;
 *     }
 * }
 * }</pre>
 *
 * <p>
 * This interface is intentionally minimal and functional,
 * allowing lambda-based or method-reference implementations.
 * </p>
 */
@FunctionalInterface
public interface ElementTypeProvider {

    /**
     * Returns the logical element type of this model.
     *
     * <p>
     * The value is typically used for template lookup or
     * conditional rendering decisions.
     * </p>
     *
     * @return non-null element type
     */
    ElementType elementType();
}
