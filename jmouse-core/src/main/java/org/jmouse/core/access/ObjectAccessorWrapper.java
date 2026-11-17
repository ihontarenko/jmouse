package org.jmouse.core.access;

import org.jmouse.core.access.accessor.*;

import java.util.List;

/**
 * A standard wrapper for {@link ObjectAccessor} that aggregates multiple {@link ObjectAccessorProvider} implementations.
 * This class extends {@link AbstractAccessorWrapper} and provides a pre‐configured list of providers that support
 * various data source types including scalar values, collections, maps, property value resolvers, Java Beans,
 * records, and property resolvers. The default constructor initializes the wrapper with the following providers:
 * <ul>
 *   <li>{@link ScalarValueAccessorProvider}</li>
 *   <li>{@link CollectionAccessorProvider}</li>
 *   <li>{@link MapAccessorProvider}</li>
 *   <li>{@link PropertyValueResolverAccessorProvider}</li>
 *   <li>{@link JavaBeanAccessorProvider}</li>
 *   <li>{@link RecordAccessorProvider}</li>
 *   <li>{@link PropertyResolverAccessorProvider}</li>
 * </ul>
 * Alternatively, a custom list of {@link ObjectAccessorProvider} instances can be supplied.
 */
public class ObjectAccessorWrapper extends AbstractAccessorWrapper {


    /**
     * Constructs a {@code ObjectAccessorWrapper} with a default set of {@link ObjectAccessorProvider} instances.
     * <p>
     * The default valueProvider list includes support for scalar values, collections, maps,
     * property value resolvers, Java Beans, records, and property resolvers.
     * </p>
     */
    public ObjectAccessorWrapper() {
        this(List.of(
                new ScalarValueAccessorProvider(),
                new ArrayAccessorProvider(),
                new CollectionAccessorProvider(),
                new MapAccessorProvider(),
                new PropertyValueResolverAccessorProvider(),
                new JavaBeanAccessorProvider(),
                new RecordAccessorProvider(),
                new PropertyResolverAccessorProvider()
        ));
    }

    /**
     * Constructs a {@code ObjectAccessorWrapper} with the specified list of {@link ObjectAccessorProvider} instances.
     *
     * @param providers a list of {@link ObjectAccessorProvider} instances to be used by this wrapper
     */
    public ObjectAccessorWrapper(List<ObjectAccessorProvider> providers) {
        super(providers);
    }

    /** The wrapper of every default provider, built once — when first asked for, not when this class is first touched. */
    public static AccessorWrapper defaults() {
        return Defaults.WRAPPER;
    }

    /**
     * The default wrapper, built on its first use: the JVM loads a holder then. A wrapper of chosen providers — one with no bean
     * or record access — never builds the reflective ones.
     */
    private static final class Defaults {

        private static final AccessorWrapper WRAPPER = new ObjectAccessorWrapper();
    }
}
