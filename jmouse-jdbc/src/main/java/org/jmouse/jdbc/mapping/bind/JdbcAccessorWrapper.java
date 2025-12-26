package org.jmouse.jdbc.mapping.bind;

import org.jmouse.core.bind.ObjectAccessorWrapper;
import org.jmouse.core.bind.StandardAccessorWrapper;

public class JdbcAccessorWrapper extends StandardAccessorWrapper {

    public static final ObjectAccessorWrapper WRAPPER = new JdbcAccessorWrapper();

    public JdbcAccessorWrapper() {
        super();
        registerProvider(new ResultSetAccessorProvider());
    }

}
