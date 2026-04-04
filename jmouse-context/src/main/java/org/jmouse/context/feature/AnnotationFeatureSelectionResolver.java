package org.jmouse.context.feature;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

public class AnnotationFeatureSelectionResolver implements FeatureSelectionResolver {

    private final FeatureMetadataResolver metadataResolver;
    private final FeatureSelectorFactory  selectorFactory;

    public AnnotationFeatureSelectionResolver(FeatureMetadataResolver metadataResolver, FeatureSelectorFactory selectorFactory) {
        this.metadataResolver = metadataResolver;
        this.selectorFactory = selectorFactory;
    }

    @Override
    public Class<?>[] resolve(FeatureSelectorContext context) {
        Set<Class<?>>                    selected = new LinkedHashSet<>();
        Set<Class<? extends Annotation>> visited  = new LinkedHashSet<>();

        for (Class<?> sourceClass : context.sourceClasses()) {
            if (sourceClass != null) {
                resolveElement(sourceClass, context, selected, visited);
            }
        }

        return selected.toArray(Class<?>[]::new);
    }

    protected void resolveElement(
            Class<?> element,
            FeatureSelectorContext rootContext,
            Set<Class<?>> selected,
            Set<Class<? extends Annotation>> visited
    ) {
        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();

            if (!visited.add(annotationType)) {
                continue;
            }

            FeatureMetadata metadata = metadataResolver.resolve(annotationType);

            selected.addAll(metadata.imports());

            if (!metadata.selectors().isEmpty()) {
                FeatureSelectorContext selectorContext = new FeatureSelectorContext.Default(
                        rootContext.beanContext(), rootContext.sourceClasses()
                );

                for (Class<? extends FeatureSelector> selectorClass : metadata.selectors()) {
                    FeatureSelector selector  = selectorFactory.create(selectorClass);
                    Class<?>[]      selection = selector.select(selectorContext);

                    if (selection != null) {
                        for (Class<?> selectedClass : selection) {
                            if (selectedClass != null) {
                                selected.add(selectedClass);
                            }
                        }
                    }
                }
            }

            resolveElement(annotationType, rootContext, selected, visited);
        }
    }

}