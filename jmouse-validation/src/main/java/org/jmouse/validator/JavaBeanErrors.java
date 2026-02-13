package org.jmouse.validator;

import org.jmouse.core.access.descriptor.structured.DescriptorResolver;
import org.jmouse.core.access.descriptor.structured.bean.JavaBeanDescriptor;

/**
 * {@link Errors} implementation for JavaBean targets using a {@link JavaBeanDescriptor}. 🫘
 *
 * <p>{@code JavaBeanErrors} enriches {@link FieldError} metadata by resolving:</p>
 * <ul>
 *   <li>current field value via bean property access</li>
 *   <li>field type via the resolved property descriptor</li>
 * </ul>
 *
 * <p>If the target is {@code null}, or the requested field is unknown, value/type resolution
 * returns {@code null}.</p>
 *
 * <p>This class relies on {@link DescriptorResolver#ofBeanType(Class)} to obtain a descriptor
 * for the target bean type.</p>
 */
@SuppressWarnings("unchecked")
public final class JavaBeanErrors extends AbstractErrors {

    private final JavaBeanDescriptor<Object> descriptor;

    /**
     * Create errors collector for a JavaBean target.
     *
     * @param target validated bean instance (may be {@code null})
     * @param objectName logical object name used in error codes (may be {@code null})
     */
    public JavaBeanErrors(Object target, String objectName) {
        super(target, objectName);
        this.descriptor = target == null
                ? null : (JavaBeanDescriptor<Object>) DescriptorResolver.ofBeanType(target.getClass());
    }

    /**
     * Resolve current field value from the target bean using {@link JavaBeanDescriptor}.
     *
     * @param field field name/path (must match a bean property)
     * @return current property value or {@code null} if unknown/unresolvable
     */
    @Override
    protected Object tryGetValue(String field) {
        if (descriptor == null || !descriptor.hasProperty(field)) {
            return null;
        }
        return descriptor.obtainValue(field, getTarget());
    }

    /**
     * Resolve field type from the bean property descriptor.
     *
     * @param field field name/path
     * @return property type or {@code null} if unknown/unresolvable
     */
    @Override
    protected Class<?> tryGetType(String field) {
        if (descriptor == null || !descriptor.hasProperty(field)) {
            return null;
        }
        return descriptor.getProperty(field).getType().getClassType();
    }
}
