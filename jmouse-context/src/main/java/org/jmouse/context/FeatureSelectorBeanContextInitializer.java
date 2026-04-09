package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextInitializer;
import org.jmouse.context.feature.AnnotationBasedFeatureMetadataResolver;
import org.jmouse.context.feature.AnnotationFeatureSelectionResolver;
import org.jmouse.context.feature.FeatureSelectionResolver;
import org.jmouse.context.feature.FeatureSelectorContext;
import org.jmouse.context.feature.ReflectionFeatureSelectorFactory;
import org.jmouse.context.feature.scanner.ExplicitFeatureClassBeanScanner;
import org.jmouse.core.Priority;

/**
 * 🧩 {@link BeanContextInitializer} that drives feature selection and import process.
 *
 * <p>
 * This initializer is responsible for activating feature-based configuration
 * during {@link BeanContext} bootstrap.
 * </p>
 *
 * <p>
 * Processing pipeline:
 * </p>
 * <ol>
 *     <li>read base classes from the {@link BeanContext}</li>
 *     <li>resolve feature classes using {@link FeatureSelectionResolver}</li>
 *     <li>import resolved classes into the context via {@link FeatureSelectionImporter}</li>
 * </ol>
 *
 * <p>
 * Default strategy:
 * </p>
 * <ul>
 *     <li>{@link AnnotationFeatureSelectionResolver} — resolves features from annotations</li>
 *     <li>{@link AnnotationBasedFeatureMetadataResolver} — extracts metadata from annotations</li>
 *     <li>{@link ReflectionFeatureSelectorFactory} — instantiates selectors</li>
 *     <li>{@link ExplicitFeatureClassBeanScanner} — scans selected classes</li>
 * </ul>
 *
 * <p>
 * Runs with very high priority (negative order) to ensure feature infrastructure
 * is registered early in the initialization phase.
 * </p>
 */
@Priority(-100001)
public class FeatureSelectorBeanContextInitializer implements BeanContextInitializer {

    /**
     * Resolver used to determine which feature classes should be activated.
     */
    private final FeatureSelectionResolver resolver;

    /**
     * Importer used to register resolved feature classes into the context.
     */
    private final FeatureSelectionImporter importer;

    /**
     * Creates initializer with default annotation-based feature selection strategy.
     */
    public FeatureSelectorBeanContextInitializer() {
        this(
                new AnnotationFeatureSelectionResolver(
                        new AnnotationBasedFeatureMetadataResolver(),
                        new ReflectionFeatureSelectorFactory()
                ),
                new FeatureSelectionImporter.Default(
                        new ExplicitFeatureClassBeanScanner()
                )
        );
    }

    /**
     * Creates initializer with custom resolver and importer.
     *
     * @param resolver feature selection resolver
     * @param importer feature importer
     */
    public FeatureSelectorBeanContextInitializer(
            FeatureSelectionResolver resolver,
            FeatureSelectionImporter importer
    ) {
        this.resolver = resolver;
        this.importer = importer;
    }

    /**
     * Initializes the {@link BeanContext} by resolving and importing feature classes.
     *
     * <p>
     * If no base classes are defined or no features are resolved, the method exits early.
     * </p>
     *
     * @param context target bean context
     */
    @Override
    public void initialize(BeanContext context) {
        Class<?>[] baseClasses = context.getBaseClasses();

        if (baseClasses == null || baseClasses.length == 0) {
            return;
        }

        FeatureSelectorContext selectorContext = new FeatureSelectorContext.Default(context, baseClasses);
        Class<?>[]             selectedClasses = resolver.resolve(selectorContext);

        if (selectedClasses == null || selectedClasses.length == 0) {
            return;
        }

        importer.importClasses(context, selectedClasses);
    }
}