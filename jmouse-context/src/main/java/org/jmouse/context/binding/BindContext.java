package org.jmouse.context.binding;

import org.jmouse.core.convert.Conversion;

public interface BindContext {

    DataSource getDataSource();

    ObjectBinder getRootBinder();

    boolean isDeepBinding();

    boolean isShallowBinding();

    Conversion getConversion();

}
