package org.jmouse.core.bind.introspection.internal;

import org.jmouse.core.bind.introspection.ClassTypeDescriptor;

import java.lang.reflect.Method;

/**
 * Holds introspected metadata about a specific Java {@link Method}.
 *
 * <p>Stores information about the method's return type along with its executable details.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class MethodData extends ExecutableData<Method> {

    private ClassTypeDescriptor returnType;

    public MethodData(Method target) {
        super(target);
    }

    /**
     * Gets the introspected return type descriptor for this method.
     *
     * @return the return type descriptor
     */
    public ClassTypeDescriptor getReturnType() {
        return returnType;
    }

    /**
     * Sets the introspected return type descriptor for this method.
     *
     * @param returnType the return type descriptor
     */
    public void setReturnType(ClassTypeDescriptor returnType) {
        this.returnType = returnType;
    }
}
