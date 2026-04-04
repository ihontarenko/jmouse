package org.jmouse.context.feature;

public class ReflectionFeatureSelectorFactory implements FeatureSelectorFactory {

    @Override
    public FeatureSelector create(Class<? extends FeatureSelector> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to instantiate feature selector: " + type.getName(), exception);
        }
    }

}