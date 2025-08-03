package org.jmouse.core.reflection.annotation;

import org.jmouse.core.bind.descriptor.AnnotationDescriptor;
import org.jmouse.core.bind.descriptor.AnnotationIntrospector;
import org.jmouse.util.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 🛠️ Default implementation for resolving and mapping annotation attributes via {@link AttributeFor}.
 * <p>
 * Supports attribute aliasing between annotations and recursive flattening of meta-annotations.
 * </p>
 *
 * <pre>{@code
 * @Meta
 * @interface A {
 *   String value() default "test";
 * }
 *
 * @AttributeFor(attribute = "value", annotation = A.class)
 * @interface B {
 *   String alias() default "test";
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AnnotationMapping implements AnnotationAttributeMapping {

    private final AnnotationDescriptor descriptor;
    private final Annotation           annotation;
    private final AnnotationAttributes attributes;
    private final MergedAnnotation     root;

    /**
     * Creates mapping from an annotation with its merged hierarchy.
     *
     * @param annotation annotation instance
     * @param root       root merged annotation context
     */
    public AnnotationMapping(Annotation annotation, MergedAnnotation root) {
        this.annotation = annotation;
        this.descriptor = new AnnotationIntrospector(annotation).introspect().toDescriptor();
        this.attributes = AnnotationAttributes.forAnnotationType(annotation.annotationType());
        this.root = root;
    }

    /**
     * 🔍 Finds the index of the attribute with the given name.
     *
     * @param name attribute name
     * @return index or -1 if not found
     */
    @Override
    public int getAttributeIndex(String name) {
        return attributes.indexOf(name);
    }

    /**
     * 🔁 Resolves attribute value by name, using alias fallback if defined via {@link AttributeFor}.
     *
     * @param name attribute name
     * @param type expected type
     * @return resolved value or {@code null} if not present
     */
    @Override
    public <T> T getAttributeValue(String name, Class<T> type) {
        int index = getAttributeIndex(name);

        if (index == -1) {
            return getAttributeValue(index, type);
        }

        return resolveAliasByName(name, type);
    }

    /**
     * 🔢 Gets value by attribute index, including fallback to aliases.
     *
     * @param index attribute index
     * @param type  expected type
     * @return attribute value or {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttributeValue(int index, Class<T> type) {
        Method attribute = attributes.getAttribute(index);

        if (attribute == null)
            return null;

        Object raw = Getter.ofMethod(attribute).get(annotation);

        if (raw != null && !isDefaultValue(attribute, raw)) {
            return (T) raw;
        }

        AttributeFor alias = attribute.getAnnotation(AttributeFor.class);

        return (alias != null) ? resolveAliasValue(alias, type) : null;
    }

    /**
     * 🪞 Fallback resolution if no index found — scans meta-annotations and aliases.
     */
    private <T> T resolveAliasByName(String name, Class<T> type) {
        for (MergedAnnotation annotation : root.getFlattened()) {
            AnnotationAttributes attributes = AnnotationAttributes.forAnnotationType(annotation.getAnnotationType());
            for (Method method : attributes.getAttributes()) {
                AttributeFor alias = method.getAnnotation(AttributeFor.class);
                if (alias != null && alias.attribute().equals(name)) {
                    Object value = Getter.ofMethod(method).get(annotation.getNativeAnnotation());
                    if (value != null && !isDefaultValue(method, value)) {
                        return type.cast(value);
                    }
                }
            }
        }
        return null;
    }

    /**
     * ✅ Checks if the value is the default declared in annotation.
     *
     * @param attribute method reference
     * @param value     current value
     * @return {@code true} if value is default
     */
    @Override
    public boolean isDefaultValue(Method attribute, Object value) {
        Object defaultValue = attribute.getDefaultValue();

        if (defaultValue == null) {
            return false;
        }

        return defaultValue.equals(value);
    }

    /**
     * 🔄 Resolves value of an aliased attribute from the declared target.
     *
     * @param alias alias declaration
     * @param type  expected result type
     * @param <T>   generic type
     * @return aliased value or {@code null}
     */
    private <T> T resolveAliasValue(AttributeFor alias, Class<T> type) {
        Class<? extends Annotation> target          = alias.annotation();
        String                      targetAttribute = alias.attribute();

        return root.getAnnotation(target)
                .map(m -> m.getMapping().getAttributeValue(targetAttribute, type))
                .orElse(null);
    }
}
