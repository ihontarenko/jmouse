package org.jmouse.core.reflection.annotation;

import org.jmouse.core.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 🔗 Default implementation of {@link AnnotationAttributeMapping}.
 *
 * <p>Resolves attribute values from a single annotation and its meta-annotations,
 * supporting aliasing via {@link MapTo}.
 *
 * <p>Example usage:
 * <pre>{@code
 * AnnotationMapping mapping = new AnnotationMapping(annotation, mergedRoot);
 * String name = mapping.getAttributeValue("name", String.class);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AnnotationMapping implements AnnotationAttributeMapping {

    private final Annotation           annotation;
    private final AnnotationAttributes attributes;
    private final MergedAnnotation     root;

    /**
     * 🏗️ Constructs a new mapping for the given annotation and its root metadata.
     *
     * @param annotation the annotation to wrap
     * @param root the root merged annotation (with full hierarchy)
     */
    public AnnotationMapping(Annotation annotation, MergedAnnotation root) {
        this.annotation = annotation;
        this.attributes = AnnotationAttributes.forAnnotationType(annotation.annotationType());
        this.root = root;
    }

    /** {@inheritDoc} */
    @Override
    public int getAttributeIndex(String name) {
        return attributes.indexOf(name);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T getAttributeValue(String name, Class<T> type) {
        Method method = attributes.getAttribute(name);

        if (method != null) {
            return getAttributeValue(method, type);
        }

        return resolve(name, type);
    }

    /** {@inheritDoc} */
    @Override
    public Object getAttributeValue(Method method) {
        return getAttributeValue(method, method.getReturnType());
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttributeValue(Method method, Class<T> type) {
        Object value = Getter.ofMethod(method).get(annotation);

        if (value != null && !isDefaultValue(method, value)) {
            return (T) value;
        }

        return resolve(method.getName(), type);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDefaultValue(Method method, Object value) {
        Object defaultValue = method.getDefaultValue();

        if (defaultValue != null) {
            if (defaultValue instanceof Object[] arrayA && value instanceof Object[] arrayB) {
                return Arrays.equals(arrayA, arrayB);
            }
            return defaultValue.equals(value);
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public AnnotationAttributes getAttributes() {
        return attributes;
    }

    /**
     * 🔄 Attempts to resolve an attribute value from meta-annotations by alias or name.
     *
     * @param name attribute name
     * @param type expected type
     * @return resolved value or {@code null} if not found
     */
    private <T> T resolve(String name, Class<T> type) {
        T result = null;

        for (MergedAnnotation meta : root.getFlattened()) {
            if ((result = resolve(meta, name, type)) != null) {
                break;
            }
        }

        return result;
    }

    /**
     * 🎯 Attempts to resolve an attribute from a specific annotation level.
     *
     * @param annotation the merged annotation to check
     * @param name attribute name
     * @param type expected type
     * @return resolved value or {@code null} if not found or default
     */
    private <T> T resolve(MergedAnnotation annotation, String name, Class<T> type) {
        AnnotationAttributes        attributes = annotation.getAnnotationMapping().getAttributes();
        Class<? extends Annotation> targetType = this.annotation.annotationType();
        Class<? extends Annotation> sourceType = annotation.getAnnotationType();

        for (Method method : attributes.getAttributes()) {
            MapTo   mapTo         = method.getAnnotation(MapTo.class);
            boolean isLinkMatch   = mapTo != null && (mapTo.attribute() == null || mapTo.attribute().equals(name));
            boolean isNameMatch   = method.getName().equals(name);

            if (isLinkMatch) {
                isLinkMatch = mapTo.annotation().equals(targetType);
                if (mapTo.annotation() == Annotation.class) {
                    isLinkMatch = sourceType == targetType;
                }
            }

            if (isLinkMatch || isNameMatch) {
                Object value = Getter.ofMethod(method).get(annotation.getNativeAnnotation());
                if (value != null && !isDefaultValue(method, value)) {
                    return type.cast(value);
                }
            }
        }

        return null;
    }

}
