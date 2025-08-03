package org.jmouse.core.reflection.annotation;

import org.jmouse.core.bind.descriptor.AnnotationDescriptor;
import org.jmouse.core.bind.descriptor.AnnotationIntrospector;
import org.jmouse.util.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 🛠️ Default implementation for mapping attributes using {@link AttributeFor}.
 */
public class AnnotationMapping implements AnnotationAttributeMapping {

    private final AnnotationDescriptor descriptor;
    private final Annotation           annotation;
    private final AnnotationAttributes attributes;
    private final MergedAnnotation     root;

    public AnnotationMapping(Annotation annotation, MergedAnnotation root) {
        this.annotation = annotation;
        this.descriptor = new AnnotationIntrospector(annotation).introspect().toDescriptor();
        this.attributes = AnnotationAttributes.forAnnotationType(annotation.annotationType());
        this.root = root;
    }

    @Override
    public int getAttributeIndex(String name) {
        return attributes.indexOf(name);
    }

    @Override
    public <T> T getAttributeValue(String name, Class<T> type) {
        int index = getAttributeIndex(name);

        if (index != -1) {
            T value = getAttributeValue(index, type);
            if (value != null) {
                return value;
            }
        }

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

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttributeValue(int index, Class<T> type) {
        Method attribute = attributes.getAttribute(index);
        if (attribute == null) {
            return null;
        }

        T value = (T) Getter.ofMethod(attribute).get(annotation);

        if (value != null && !isDefaultValue(attribute, value)) {
            return value;
        }

        AttributeFor alias = attribute.getAnnotation(AttributeFor.class);

        if (alias != null) {
            return resolveAliasValue(alias, type);
        }

        return null;
    }

    @Override
    public Map<String, Object> resolveMappings() {
        Map<String, Object> result = new HashMap<>();

        for (Method method : annotation.getClass().getDeclaredMethods()) {
            AttributeFor map = method.getAnnotation(AttributeFor.class);

            if (map != null) {
                Object value = Getter.ofMethod(method).get(annotation);
                result.put(map.annotation().getName() + "#" + map.attribute(), value);
            }
        }

        return result;
    }

    public boolean isDefaultValue(Method attribute, Object value) {
        Object defaultValue = attribute.getDefaultValue();

        if (defaultValue == null) {
            return false;
        }

        return defaultValue.equals(value);
    }

    private <T> T resolveAliasValue(AttributeFor alias, Class<T> type) {
        Class<? extends Annotation> target          = alias.annotation();
        String                      targetAttribute = alias.attribute();

        return root.getAnnotation(target).map(m
                -> m.getMapping().getAttributeValue(targetAttribute, type)).orElse(null);
    }

}