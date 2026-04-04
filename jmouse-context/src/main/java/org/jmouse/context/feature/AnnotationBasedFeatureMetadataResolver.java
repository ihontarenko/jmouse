package org.jmouse.context.feature;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class AnnotationBasedFeatureMetadataResolver implements FeatureMetadataResolver {

    @Override
    public FeatureMetadata resolve(Class<? extends Annotation> annotationType) {
        Set<Class<?>>                         imports       = new LinkedHashSet<>();
        Set<Class<? extends FeatureSelector>> selectors     = new LinkedHashSet<>();
        FeatureImport                         featureImport = annotationType.getAnnotation(FeatureImport.class);

        if (featureImport != null) {
            imports.addAll(Arrays.asList(featureImport.value()));
        }

        FeatureSelectorBinding binding = annotationType.getAnnotation(FeatureSelectorBinding.class);

        if (binding != null) {
            selectors.addAll(Arrays.asList(binding.value()));
        }

        return new FeatureMetadata.Default(imports, selectors);
    }

}