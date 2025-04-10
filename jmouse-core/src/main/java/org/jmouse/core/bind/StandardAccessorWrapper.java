package org.jmouse.core.bind;

import org.jmouse.core.bind.accessor.*;

import java.util.List;

/**
 * A standard wrapper for {@link ObjectAccessor} that aggregates multiple {@link ObjectAccessorProvider} implementations.
 * <p>
 * This class extends {@link BasicAccessorWrapper} and provides a pre‐configured list of providers that support
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
 * </p>
 */
public class StandardAccessorWrapper extends BasicAccessorWrapper {

    /**
     * Constructs a {@code StandardAccessorWrapper} with a default set of {@link ObjectAccessorProvider} instances.
     * <p>
     * The default provider list includes support for scalar values, collections, maps,
     * property value resolvers, Java Beans, records, and property resolvers.
     * </p>
     */
    public StandardAccessorWrapper() {
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
     * Constructs a {@code StandardAccessorWrapper} with the specified list of {@link ObjectAccessorProvider} instances.
     *
     * @param providers a list of {@link ObjectAccessorProvider} instances to be used by this wrapper
     */
    public StandardAccessorWrapper(List<ObjectAccessorProvider> providers) {
        super(providers);
    }
}
