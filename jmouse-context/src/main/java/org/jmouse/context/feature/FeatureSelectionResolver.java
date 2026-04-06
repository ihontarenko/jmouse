package org.jmouse.context.feature;

/**
 * 🧩 Strategy interface for resolving feature classes from a given context.
 *
 * <p>
 * Implementations define how feature sources (e.g. annotations, configuration classes,
 * external metadata) are interpreted and translated into a set of classes that should
 * be imported into the {@code BeanContext}.
 * </p>
 *
 * <p>
 * This abstraction allows different resolution mechanisms, such as:
 * </p>
 * <ul>
 *     <li>annotation-driven resolution (e.g. {@code @Feature}, {@code @Import})</li>
 *     <li>programmatic or conditional selection</li>
 *     <li>environment-based feature activation</li>
 * </ul>
 *
 * @see FeatureSelectorContext
 */
public interface FeatureSelectionResolver {

    /**
     * 🔍 Resolves feature classes based on the provided context.
     *
     * <p>
     * Implementations may inspect source classes, annotations, or other contextual
     * data to determine which feature classes should be selected.
     * </p>
     *
     * @param context feature selection context
     * @return array of selected feature classes (never {@code null}, may be empty)
     */
    Class<?>[] resolve(FeatureSelectorContext context);

}