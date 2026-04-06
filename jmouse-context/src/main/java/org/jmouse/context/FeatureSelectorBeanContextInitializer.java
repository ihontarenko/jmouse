package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextInitializer;
import org.jmouse.context.feature.AnnotationBasedFeatureMetadataResolver;
import org.jmouse.context.feature.AnnotationFeatureSelectionResolver;
import org.jmouse.context.feature.FeatureSelectionResolver;
import org.jmouse.context.feature.FeatureSelectorContext;
import org.jmouse.context.feature.ReflectionFeatureSelectorFactory;
import org.jmouse.context.feature.scanner.ExplicitFeatureClassBeanScanner;

public class FeatureSelectorBeanContextInitializer implements BeanContextInitializer {

    private final FeatureSelectionResolver resolver;
    private final FeatureSelectionImporter importer;

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

    public FeatureSelectorBeanContextInitializer(
            FeatureSelectionResolver resolver,
            FeatureSelectionImporter importer
    ) {
        this.resolver = resolver;
        this.importer = importer;
    }

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