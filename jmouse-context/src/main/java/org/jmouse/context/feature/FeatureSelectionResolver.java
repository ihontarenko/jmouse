package org.jmouse.context.feature;

public interface FeatureSelectionResolver {

    Class<?>[] resolve(FeatureSelectorContext context);

}