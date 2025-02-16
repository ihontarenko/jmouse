package org.jmouse.core.bind.descriptor;

import org.jmouse.core.reflection.JavaType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.jmouse.core.reflection.Reflections.getShortName;

public interface AnnotationDescriptor extends Descriptor<Annotation> {

    TypeDescriptor getAnnotationType();

    Map<String, Object> getAttributes();

    static AnnotationDescriptor.Builder of(Annotation annotation) {
        return new AnnotationDescriptor.Builder(annotation);
    }

    class Implementation extends AbstractDescriptor<Annotation> implements AnnotationDescriptor {

        private final Map<String, Object> attributes = new HashMap<>();
        private final TypeDescriptor      annotationType;

        protected Implementation(Mutable<?, Annotation, Descriptor<Annotation>> mutable, String name, Annotation internal) {
            super(mutable, name, internal);
        }

//        Implementation(String name, Annotation internal, Map<String, Object> attributes, TypeDescriptor annotationType) {
//            this.attributes.putAll(attributes);
//            this.annotationType = annotationType;
//        }



        @Override
        public TypeDescriptor getAnnotationType() {
            return annotationType;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public AnnotationDescriptor introspect() {
            return this;
        }
    }

    final class Builder extends AbstractMutableDescriptor<Builder, Annotation, AnnotationDescriptor> {

        private Map<String, Object> attributes = new HashMap<>();
        private TypeDescriptor      annotationType;

        public Builder(Annotation describable) {
            super(describable);
        }

        public Builder attribute(String name, Object value) {
            attributes.put(name, value);
            return self();
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return self();
        }

        public Builder annotationType(TypeDescriptor annotationType) {
            this.annotationType = annotationType;
            return self();
        }

        public Builder annotationType() {
            return annotationType(TypeDescriptor.forType(JavaType.forType(target.annotationType())));
        }

        @Override
        public AnnotationDescriptor toImmutable() {
            return new Implementation(name, target, attributes, annotationType);
        }

    }

    /**
     * Creates a descriptor for an annotation.
     *
     * @param annotation the annotation to describe
     * @return an {@link AnnotationDescriptor} instance
     */
    static AnnotationDescriptor forAnnotation(Annotation annotation) {
        return forAnnotation(annotation, TypeDescriptor.DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for an annotation with a specified depth for nested elements.
     * <p>
     * This method extracts descriptor about the annotation, including its type and attributes.
     * The depth parameter controls how deep nested annotations and class descriptors are resolved.
     * </p>
     *
     * @param annotation the annotation to describe
     * @param depth      the recursion depth limit for nested descriptor resolution
     * @return an {@link AnnotationDescriptor} instance
     */
    static AnnotationDescriptor forAnnotation(Annotation annotation, int depth) {
        Class<? extends Annotation>  type    = annotation.annotationType();
        AnnotationDescriptor.Builder builder = new AnnotationDescriptor.Builder(annotation);

        builder.target(annotation).annotationType(TypeDescriptor.forType(JavaType.forType(type), depth - 1));

        try {
            Map<String, Object> attributes = new HashMap<>();

            for (Method method : type.getDeclaredMethods()) {
                attributes.put(method.getName(), method.invoke(annotation));
            }

            builder.attributes(attributes);
        } catch (Exception ignored) {
        }

        return builder.toImmutable();
    }

}
