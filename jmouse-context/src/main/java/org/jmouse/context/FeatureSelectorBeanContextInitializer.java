package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextInitializer;
import org.jmouse.context.feature.*;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class FeatureSelectorBeanContextInitializer implements BeanContextInitializer {

    private final FeatureSelectionResolver resolver;

    public FeatureSelectorBeanContextInitializer() {
        this(new AnnotationFeatureSelectionResolver(
                new AnnotationBasedFeatureMetadataResolver(),
                new ReflectionFeatureSelectorFactory()
        ));
    }

    public FeatureSelectorBeanContextInitializer(FeatureSelectionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void initialize(BeanContext context) {
        Class<?>[] baseClasses = context.getBaseClasses();

        if (baseClasses == null || baseClasses.length == 0) {
            return;
        }

        FeatureSelectorContext selectorContext = new FeatureSelectorContext.Default(context, baseClasses);
        Class<?>[]             selectedClasses = resolver.resolve(selectorContext);

        if (selectedClasses.length == 0) {
            return;
        }

        Set<Class<?>> additions = new LinkedHashSet<>();
        Set<Class<?>> existing  = new LinkedHashSet<>(Arrays.asList(baseClasses));

        for (Class<?> selectedClass : selectedClasses) {
            if (selectedClass != null && !existing.contains(selectedClass)) {
                additions.add(selectedClass);
            }
        }

        if (!additions.isEmpty()) {
            context.addBaseClasses(additions.toArray(Class<?>[]::new));
        }
    }

}