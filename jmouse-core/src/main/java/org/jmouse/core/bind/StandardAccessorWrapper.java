package org.jmouse.core.bind;

import java.util.List;

/**
 * A standard wrapper for ObjectAccessor that aggregates multiple ObjectAccessorProvider implementations.
 * <p>
 * This class extends {@link BasicAccessorWrapper} and provides a pre‐configured list of providers that support
 * a variety of data source types, including standard types, Java Beans, records, and property resolvers.
 * The default constructor initializes the wrapper with the following providers:
 * <ul>
 *   <li>{@link StandardTypesAccessorProvider}</li>
 *   <li>{@link JavaBeanAccessorProvider}</li>
 *   <li>{@link RecordAccessorProvider}</li>
 *   <li>{@link PropertyResolverAccessorProvider}</li>
 * </ul>
 * Alternatively, a custom list of {@link ObjectAccessorProvider} instances can be supplied.
 * </p>
 */
public class StandardAccessorWrapper extends BasicAccessorWrapper {

    /**
     * Constructs a StandardAccessorWrapper with a default set of ObjectAccessorProvider instances.
     * <p>
     * The default provider list includes support for standard types, Java Beans, records,
     * and property resolvers.
     * </p>
     */
    public StandardAccessorWrapper() {
        this(List.of(
                new StandardTypesAccessorProvider(),
                new JavaBeanAccessorProvider(),
                new RecordAccessorProvider(),
                new PropertyResolverAccessorProvider()
        ));
    }

    /**
     * Constructs a StandardAccessorWrapper with the specified list of ObjectAccessorProvider instances.
     *
     * @param providers a list of ObjectAccessorProvider instances to be used by this wrapper
     */
    public StandardAccessorWrapper(List<ObjectAccessorProvider> providers) {
        super(providers);
    }
}
