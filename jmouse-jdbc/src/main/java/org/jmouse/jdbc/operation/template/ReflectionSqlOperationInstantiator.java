package org.jmouse.jdbc.operation.template;

import org.jmouse.core.Verify;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.jdbc.operation.SqlOperation;

import java.lang.reflect.Constructor;

/**
 * Reflection-based {@link SqlOperationInstantiator} that uses a no-argument
 * constructor.
 *
 * @author Ivan Hontarenko
 */
public class ReflectionSqlOperationInstantiator implements SqlOperationInstantiator {

    @Override
    public <O extends SqlOperation> O instantiate(Class<O> operationType) {
        Verify.nonNull(operationType, "operationType");

        try {
            @SuppressWarnings("unchecked")
            Constructor<O> constructor = (Constructor<O>) Reflections.findFirstConstructor(operationType);
            constructor.setAccessible(true);
            return Reflections.instantiate(constructor);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to instantiate SQL operation: " + operationType.getName(), exception
            );
        }
    }

}