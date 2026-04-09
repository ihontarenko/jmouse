package org.jmouse.context.feature;

/**
 * 🏭 Factory for creating {@link FeatureSelector} instances.
 *
 * <p>
 * Abstracts the instantiation strategy for {@link FeatureSelector} implementations.
 * </p>
 *
 * <p>
 * Used by {@link FeatureSelectionResolver} to obtain selector instances
 * during feature resolution.
 * </p>
 */
public interface FeatureSelectorFactory {

    /**
     * Creates a {@link FeatureSelector} instance for the given type.
     *
     * @param type selector implementation class
     * @return instantiated {@link FeatureSelector}
     */
    FeatureSelector create(Class<? extends FeatureSelector> type);

}