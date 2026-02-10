package org.jmouse.core.mapping.strategy.support;

import org.jmouse.core.Verify;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.mapping.config.MappingConfig;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Base strategy support for {@link Map}-target mappings. 🗺️
 *
 * <p>This class provides a utility method for resolving the target map instance to populate.
 * It supports in-place mapping by reusing an existing map instance carried by {@link TypedValue},
 * otherwise it creates a new instance using {@link MappingConfig#mapFactory()}.</p>
 *
 * @param <T> concrete map target type
 */
abstract public class AbstractMapStrategy<T extends Map<?, ?>> extends AbstractStrategy<T> {

    /**
     * Resolve the target map instance to be populated.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>use an existing instance from {@code typedValue.getValue().get()}</li>
     *   <li>otherwise create a new instance via {@code config.mapFactory()}</li>
     * </ol>
     *
     * @param config mapping configuration providing default factories
     * @param typedValue typed target descriptor that may carry an existing instance
     * @return non-null target map instance
     * @throws IllegalArgumentException if the resolved instance is {@code null}
     */
    @SuppressWarnings("unchecked")
    protected Map<Object, Object> getTargetMap(MappingConfig config, TypedValue<T> typedValue) {
        Map<Object, Object> instance = (Map<Object, Object>) typedValue.getValue().get();
        Supplier<T>         factory  = (Supplier<T>) config.mapFactory();

        if (instance == null) {
            instance = (Map<Object, Object>) factory.get();
        }

        return Verify.nonNull(instance, "instance");
    }

}
