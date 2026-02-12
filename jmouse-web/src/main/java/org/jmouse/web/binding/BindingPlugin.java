package org.jmouse.web.binding;

import org.jmouse.core.mapping.MappingDestination;
import org.jmouse.core.mapping.plugin.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

public final class BindingPlugin implements MappingPlugin {

    @Override
    public Object onValue(MappingValue value) {

        MappingDestination destination = value.destination();

        // 1️⃣ Root-level (scalar / deep conversion)
        if (destination instanceof MappingDestination.RootTarget) {
            return normalize(value.current());
        }

        // 2️⃣ Bean property binding
        if (destination instanceof MappingDestination.BeanProperty beanProperty) {

            Object target = beanProperty.target();
            Object descriptor = beanProperty.propertyDescriptor();

            Object raw = value.current();
            Object normalized = normalize(raw);

            validateProperty(descriptor, normalized);

            return normalized;
        }

        // 3️⃣ Map entry binding
        if (destination instanceof MappingDestination.MapEntry mapEntry) {
            return normalize(value.current());
        }

        // 4️⃣ Collection element binding
        if (destination instanceof MappingDestination.CollectionElement element) {
            return normalize(value.current());
        }

        return value.current();
    }

    @Override
    public void onFinish(MappingResult result) {
        Object target = result.target();
        validateObject(target);
    }

    @Override
    public void onError(MappingFailure failure) {
        // optional: logging / aggregation
    }

    // ----------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------

    private Object normalize(Object value) {
        if (value instanceof String s) {
            return s.trim();
        }
        return value;
    }

    private void validateProperty(Object descriptor, Object value) {
        if (descriptor == null) return;

        if (descriptor instanceof AnnotatedElement element) {
            for (Annotation annotation : element.getAnnotations()) {
                // custom constraint handling here
            }
        }
    }

    private void validateObject(Object target) {
        if (target == null) return;

        // cross-field validation here
    }
}
