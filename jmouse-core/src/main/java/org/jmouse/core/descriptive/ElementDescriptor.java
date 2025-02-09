package org.jmouse.core.descriptive;

import java.util.*;

public interface ElementDescriptor<T> extends Descriptor<T> {

    Collection<AnnotationDescriptor> getAnnotations();

    abstract class Implementation<T> extends Descriptor.Implementation<T> implements ElementDescriptor<T> {

        protected final Set<AnnotationDescriptor> annotations;

        public Implementation(String name, T internal, Set<AnnotationDescriptor> annotations) {
            super(name, internal);
            this.annotations = annotations;
        }

        @Override
        public Collection<AnnotationDescriptor> getAnnotations() {
            return annotations;
        }
    }

    abstract class Builder<B extends Descriptor.Builder<B, I, D>, I, D extends ElementDescriptor<I>>
            extends DescriptorBuilder<B, I, D> {

        protected Set<AnnotationDescriptor> annotations = new HashSet<>();

        protected Builder(String name) {
            super(name);
        }

        public B annotation(AnnotationDescriptor annotation) {
            this.annotations.add(annotation);
            return self();
        }

    }

}
