package org.jmouse.jdbc.template;

import org.jmouse.core.reflection.Reflections;
import org.jmouse.jdbc.operation.annotation.InlineSql;

public interface SelfAnnotatedQueryOperation<T> extends QueryOperation<T> {

    @Override
    default String sql() {
        return Reflections.getAnnotationValue(getClass(), InlineSql.class, InlineSql::value);
    }

}
