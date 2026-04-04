package org.jmouse.context.feature;

public interface FeatureSelectorFactory {

    FeatureSelector create(Class<? extends FeatureSelector> type);

}