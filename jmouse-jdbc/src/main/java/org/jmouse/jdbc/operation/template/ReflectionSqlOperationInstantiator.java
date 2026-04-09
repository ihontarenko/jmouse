package org.jmouse.jdbc.operation.template;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.operation.SqlOperation;

import java.lang.reflect.Constructor;

public class ReflectionSqlOperationInstantiator implements SqlOperationInstantiator {

    @Override
    public <O extends SqlOperation> O instantiate(Class<O> operationType) {
        Verify.nonNull(operationType, "operationType");

        try {
            Constructor<O> constructor = operationType.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(
                    "SQL operation must declare a no-argument constructor for class-based execution: "
                            + operationType.getName(),
                    exception
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to instantiate SQL operation: " + operationType.getName(),
                    exception
            );
        }
    }

}