package org.jmouse.core.bind;

import org.jmouse.core.bind.accessor.DummyObjectAccessor;
import org.jmouse.core.bind.accessor.NullObjectAccessor;
import org.jmouse.core.Sorter;

import java.util.ArrayList;
import java.util.List;

/**
 * A flexible factory for creating ObjectAccessor instances.
 * <p>
 * The factory is configured with a list of providers (strategies) that decide
 * whether they support a given source and create an accessor accordingly.
 * </p>
 */
public class BasicAccessorWrapper implements ObjectAccessorWrapper {

    protected final List<ObjectAccessorProvider> providers;

    /**
     * Constructs a factory with the given list of providers.
     *
     * @param providers a list of ObjectAccessorProvider instances
     */
    public BasicAccessorWrapper(List<ObjectAccessorProvider> providers) {
        this.providers = new ArrayList<>(List.copyOf(providers));
    }

    /**
     * Wraps the given source into a ObjectAccessor by delegating to the appropriate provider.
     * <p>
     * If no provider supports the source, an UnsupportedOperationException is thrown.
     * </p>
     *
     * @param source the source object to wrap
     * @return a ObjectAccessor for the given source
     */
    @Override
    public ObjectAccessor wrap(Object source) {
        ObjectAccessor instance = new NullObjectAccessor();

        if (source != null) {
            instance = new DummyObjectAccessor(source);

            Sorter.sort(providers);

            for (ObjectAccessorProvider provider : providers) {
                if (provider.supports(source)) {
                    instance = provider.create(source);
                    break;
                }
            }

            if (instance instanceof ObjectAccessorWrapper.Aware aware) {
                aware.setWrapper(this);
            }
        }

        return instance;
    }
}
