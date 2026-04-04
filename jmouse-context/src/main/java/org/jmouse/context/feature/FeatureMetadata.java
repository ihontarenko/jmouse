package org.jmouse.context.feature;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public interface FeatureMetadata {

    Set<Class<?>> imports();

    Set<Class<? extends FeatureSelector>> selectors();

    boolean empty();

    class Default implements FeatureMetadata {

        private final Set<Class<?>>                         imports;
        private final Set<Class<? extends FeatureSelector>> selectors;

        public Default(Set<Class<?>> imports, Set<Class<? extends FeatureSelector>> selectors) {
            this.imports = Collections.unmodifiableSet(new LinkedHashSet<>(imports));
            this.selectors = Collections.unmodifiableSet(new LinkedHashSet<>(selectors));
        }

        @Override
        public Set<Class<?>> imports() {
            return imports;
        }

        @Override
        public Set<Class<? extends FeatureSelector>> selectors() {
            return selectors;
        }

        @Override
        public boolean empty() {
            return imports.isEmpty() && selectors.isEmpty();
        }

    }

}