package org.jmouse.jdbc.mapping.bind;

import org.jmouse.core.Priority;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.ObjectAccessorProvider;
import org.jmouse.core.reflection.TypeInformation;

import java.sql.ResultSet;

@Priority(-5000)
public class ResultSetAccessorProvider implements ObjectAccessorProvider {

    @Override
    public boolean supports(Object source) {
        return TypeInformation.forInstance(source).is(ResultSet.class);
    }

    @Override
    public ObjectAccessor create(Object source) {
        return new ResultSetAccessor(source);
    }

}
