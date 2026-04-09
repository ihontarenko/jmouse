package org.jmouse.context.feature;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 🔎 Annotation-based implementation of {@link FeatureMetadataResolver}.
 *
 * <p>
 * Extracts {@link FeatureMetadata} directly from annotation types using
 * declarative annotations:
 * </p>
 * <ul>
 *     <li>{@link FeatureImport} — defines statically imported feature classes</li>
 *     <li>{@link FeatureSelectorBinding} — defines dynamic {@link FeatureSelector selectors}</li>
 * </ul>
 *
 * <p>
 * This resolver does not traverse annotation hierarchies — it operates strictly
 * on the provided annotation type. Recursive processing is handled by
 * higher-level components (e.g. {@code AnnotationFeatureSelectionResolver}).
 * </p>
 *
 * <p>
 * Uses {@link LinkedHashSet} to preserve declaration order and avoid duplicates.
 * </p>
 */
public class AnnotationBasedFeatureMetadataResolver implements FeatureMetadataResolver {

    /**
     * Resolves {@link FeatureMetadata} from the given annotation type.
     *
     * <p>
     * Processing rules:
     * </p>
     * <ul>
     *     <li>if {@link FeatureImport} is present — extract imported classes</li>
     *     <li>if {@link FeatureSelectorBinding} is present — extract selector classes</li>
     *     <li>otherwise — return empty metadata</li>
     * </ul>
     *
     * @param annotationType annotation type to inspect
     * @return resolved {@link FeatureMetadata} (never {@code null})
     */
    @Override
    public FeatureMetadata resolve(Class<? extends Annotation> annotationType) {
        Set<Class<?>>                         imports   = new LinkedHashSet<>();
        Set<Class<? extends FeatureSelector>> selectors = new LinkedHashSet<>();

        FeatureImport feature = annotationType.getAnnotation(FeatureImport.class);

        if (feature != null) {
            imports.addAll(Arrays.asList(feature.value()));
        }

        FeatureSelectorBinding binding = annotationType.getAnnotation(FeatureSelectorBinding.class);

        if (binding != null) {
            selectors.addAll(Arrays.asList(binding.value()));
        }

        return new FeatureMetadata.Default(imports, selectors);
    }

}