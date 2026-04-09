package org.jmouse.context.feature;

import org.jmouse.core.Visitor;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 🔎 {@link FeatureSelectionResolver} that derives feature classes from annotations.
 *
 * <p>
 * This resolver inspects source classes from {@link FeatureSelectorContext},
 * reads their annotations, resolves {@link FeatureMetadata} for each annotation,
 * collects declared imports, executes registered {@link FeatureSelector selectors},
 * and recursively traverses meta-annotations.
 * </p>
 *
 * <p>
 * Resolution is protected against cycles by tracking already visited annotation types.
 * A {@link LinkedHashSet} is used to preserve discovery order while preventing duplicates.
 * </p>
 */
public class AnnotationFeatureSelectionResolver implements FeatureSelectionResolver {

    /**
     * Metadata resolver used to extract imports and selectors from annotation types.
     */
    private final FeatureMetadataResolver metadataResolver;

    /**
     * Factory used to instantiate {@link FeatureSelector} implementations.
     */
    private final FeatureSelectorFactory selectorFactory;

    /**
     * Creates a new annotation-based feature selection resolver.
     *
     * @param metadataResolver resolver for feature metadata declared on annotations
     * @param selectorFactory  factory for creating selector instances
     */
    public AnnotationFeatureSelectionResolver(
            FeatureMetadataResolver metadataResolver,
            FeatureSelectorFactory selectorFactory
    ) {
        this.metadataResolver = metadataResolver;
        this.selectorFactory = selectorFactory;
    }

    /**
     * Resolves feature classes from all source classes in the given context.
     *
     * <p>
     * Each non-null source class is inspected recursively through its annotations
     * and meta-annotations. Imported classes and dynamically selected classes are
     * accumulated into an ordered, deduplicated result.
     * </p>
     *
     * @param context feature selection context with bean context and source classes
     * @return resolved feature classes, never {@code null}
     */
    @Override
    public Class<?>[] resolve(FeatureSelectorContext context) {
        Set<Class<?>>                        selected = new LinkedHashSet<>();
        Visitor<Class<? extends Annotation>> visitor  = new Visitor.Default<>();

        for (Class<?> sourceClass : context.sourceClasses()) {
            if (sourceClass != null) {
                resolveElement(sourceClass, context, selected, visitor);
            }
        }

        return selected.toArray(Class<?>[]::new);
    }

    /**
     * Resolves feature declarations from annotations present on the given element.
     *
     * <p>
     * For each annotation on the element this method:
     * </p>
     * <ol>
     *     <li>skips the annotation if it has already been processed</li>
     *     <li>resolves {@link FeatureMetadata} for the annotation type</li>
     *     <li>adds imported classes declared by the metadata</li>
     *     <li>instantiates and executes declared {@link FeatureSelector selectors}</li>
     *     <li>recursively processes annotations declared on the annotation type itself</li>
     * </ol>
     *
     * <p>
     * The {@code visited} set prevents repeated processing and protects against
     * cyclic meta-annotation graphs.
     * </p>
     *
     * @param element      class whose annotations should be processed
     * @param rootContext  root feature selector context
     * @param selected     accumulator for resolved feature classes
     * @param visitor      already processed annotation types
     */
    protected void resolveElement(
            Class<?> element,
            FeatureSelectorContext rootContext,
            Set<Class<?>> selected,
            Visitor<Class<? extends Annotation>> visitor
    ) {
        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();

            if (visitor.familiar(annotationType)) {
                continue;
            }

            visitor.visit(annotationType);

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

            resolveElement(annotationType, rootContext, selected, visitor);
        }
    }

}