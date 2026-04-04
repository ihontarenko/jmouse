package org.jmouse.context.feature;

public interface FeatureSelector {

    Class<?>[] select(FeatureSelectorContext context);

}