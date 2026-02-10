package org.jmouse.jdbc.mapping.bind;

import org.jmouse.core.Priority;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ObjectAccessorProvider;
import org.jmouse.core.reflection.TypeInformation;

import java.sql.ResultSet;

/**
 * {@link ObjectAccessorProvider} implementation for JDBC {@link ResultSet}.
 * <p>
 * {@code ResultSetAccessorProvider} enables the jMouse binding infrastructure
 * to treat a {@link ResultSet} as a readable object source, allowing column-based
 * access during binding and mapping operations.
 *
 * <p>
 * This valueProvider is typically used indirectly by higher-level components such as:
 * <ul>
 *     <li>{@code BeanRowMapper}</li>
 *     <li>generic binders operating on JDBC results</li>
 * </ul>
 *
 * <h3>Priority</h3>
 * <p>
 * The valueProvider is registered with a very low priority ({@code -5000}) to ensure
 * it is selected only when no more specific accessor providers apply.
 *
 * <h3>Example scenario</h3>
 * <pre>{@code
 * ResultSet rs = statement.executeQuery();
 * ObjectAccessor accessor = valueProvider.create(rs);
 * }</pre>
 *
 * @author jMouse
 */
@Priority(-5000)
public class ResultSetAccessorProvider implements ObjectAccessorProvider {

    /**
     * Determines whether this valueProvider supports the given source object.
     *
     * @param source source object
     * @return {@code true} if the source is a {@link ResultSet}
     */
    @Override
    public boolean supports(Object source) {
        return TypeInformation.forInstance(source).is(ResultSet.class);
    }

    /**
     * Creates an {@link ObjectAccessor} for the given {@link ResultSet}.
     *
     * @param source source object (expected to be {@link ResultSet})
     * @return result set accessor
     */
    @Override
    public ObjectAccessor create(Object source) {
        return new ResultSetAccessor(source);
    }

}
