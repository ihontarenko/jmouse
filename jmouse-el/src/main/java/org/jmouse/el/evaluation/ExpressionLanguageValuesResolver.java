package org.jmouse.el.evaluation;

import org.jmouse.core.access.DefaultPropertyValueResolver;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.VirtualPropertyResolver;

/**
 * A custom property value resolver for expression language evaluation.
 * <p>
 * This class extends {@link DefaultPropertyValueResolver} to enable property resolution
 * within an expression language context. It uses an {@link ObjectAccessor} for reading and
 * writing property values and a {@link VirtualPropertyResolver} for resolving virtual properties.
 * </p>
 * <p>
 * Two constructors are provided:
 * <ul>
 *   <li>
 *     One that accepts both an {@link ObjectAccessor} and a {@link VirtualPropertyResolver},
 *     allowing full customization.
 *   </li>
 *   <li>
 *     A convenience constructor that uses a default virtual property resolver
 *     ({@link VirtualPropertyResolver.Default}).
 *   </li>
 * </ul>
 * </p>
 *
 * @see DefaultPropertyValueResolver
 */
public class ExpressionLanguageValuesResolver extends DefaultPropertyValueResolver {

    /**
     * Constructs a new ExpressionLanguageValuesResolver with the specified object accessor
     * and virtual property resolver.
     *
     * @param accessor the object accessor used to perform property access operations
     * @param resolver the virtual property resolver for handling virtual properties
     */
    public ExpressionLanguageValuesResolver(ObjectAccessor accessor, VirtualPropertyResolver resolver) {
        super(accessor, resolver);
    }

    /**
     * Constructs a new ExpressionLanguageValuesResolver with the specified object accessor.
     * <p>
     * This constructor uses the {@link VirtualPropertyResolver.Default} as the default virtual
     * property resolver.
     * </p>
     *
     * @param accessor the object accessor used to perform property access operations
     */
    public ExpressionLanguageValuesResolver(ObjectAccessor accessor) {
        super(accessor, new VirtualPropertyResolver.Default());
    }
}
