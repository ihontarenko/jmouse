package org.jmouse.core.access;

import org.jmouse.core.Sorter;
import org.jmouse.core.access.accessor.DummyObjectAccessor;
import org.jmouse.core.access.accessor.NullObjectAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A flexible factory for creating ObjectAccessor instances.
 * <p>
 * The factory is configured with a list of providers (strategies) that decide
 * whether they support a given source and create an accessor accordingly.
 * </p>
 *
 * <h3>Why this class caches</h3>
 * <p>
 * ⚠️ {@link #wrap(Object)} sits on the mapping engine's hot path - at least twice per mapped object,
 * plus once per nested value. Two things made that expensive, and both are gone:
 * </p>
 * <ul>
 *   <li>the provider list was re-sorted on every call; it is now ordered when it <em>changes</em></li>
 *   <li>every provider was asked {@code supports(source)}, and the shipped ones answer by building a
 *       {@code TypeInformation} and running up to eight classification predicates; the winner is now
 *       remembered per source class</li>
 * </ul>
 *
 * <p>
 * ⚠️ <b>The contract this places on providers:</b> {@link ObjectAccessorProvider#supports(Object)}
 * must decide from the source's <em>class</em>, never from the state of one instance. Every shipped
 * provider does. A provider that answers differently for two instances of the same class will be
 * asked once and its first answer reused for the rest.
 * </p>
 */
abstract public class AbstractAccessorWrapper implements AccessorWrapper {

    protected final List<ObjectAccessorProvider> providers;

    private final Map<Class<?>, ObjectAccessorProvider> selected = new ConcurrentHashMap<>();

    /**
     * Constructs a factory with the given list of providers.
     *
     * @param providers a list of ObjectAccessorProvider instances
     */
    public AbstractAccessorWrapper(List<ObjectAccessorProvider> providers) {
        this.providers = new ArrayList<>(providers);
        Sorter.sort(this.providers);
    }

    /**
     * Wraps the given source into a ObjectAccessor by delegating to the first provider that supports it.
     *
     * @param source the source object to wrap
     * @return a ObjectAccessor for the given source
     */
    @Override
    public ObjectAccessor wrap(Object source) {
        if (source == null) {
            return new NullObjectAccessor();
        }

        ObjectAccessorProvider provider = selected.get(source.getClass());

        if (provider == null) {
            provider = select(source);
            selected.put(source.getClass(), provider);
        }

        ObjectAccessor instance = provider.create(source);

        if (instance instanceof AccessorWrapper.Aware aware) {
            aware.setWrapper(this);
        }

        return instance;
    }

    /**
     * Ask each provider in order which one handles this source.
     *
     * <p>The real instance is passed, because that is what the contract says providers receive; only
     * the <em>answer</em> is then kept per class.</p>
     *
     * @param source the object being wrapped
     * @return the first provider that supports it, or one producing a plain accessor
     */
    private ObjectAccessorProvider select(Object source) {
        for (ObjectAccessorProvider provider : providers) {
            if (provider.supports(source)) {
                return provider;
            }
        }

        return DUMMY;
    }

    /**
     * Register an {@link ObjectAccessorProvider}, restore the ordering, and forget the memoized choices.
     *
     * <p>A new provider may outrank one that already answered for some class, so previous choices
     * cannot be trusted afterwards.</p>
     *
     * @param provider the valueProvider to register
     */
    @Override
    public void registerProvider(ObjectAccessorProvider provider) {
        providers.add(provider);
        Sorter.sort(providers);
        selected.clear();
    }

    /**
     * Stand-in for "no provider claimed this class", so the memo can record that too.
     */
    private static final ObjectAccessorProvider DUMMY = new ObjectAccessorProvider() {

        @Override
        public boolean supports(Object source) {
            return true;
        }

        @Override
        public ObjectAccessor create(Object source) {
            return new DummyObjectAccessor(source);
        }
    };

}
