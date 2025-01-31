package org.jmouse.context.binding;

public interface BindContext {

    DataSource getDataSource();

    ObjectBinder getRootBinder();

    boolean isDeepBinding();

    boolean isShallowBinding();

}
