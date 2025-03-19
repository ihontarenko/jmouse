package org.jmouse.core.bind.descriptor.internal;

import org.jmouse.core.bind.descriptor.ClassTypeDescriptor;

import java.lang.reflect.Parameter;

/**
 * Container for metadata related to a method or constructor parameter.
 * Extends {@link AnnotatedElementData} to store annotations and type information.
 *
 * @author Ivan Hontarenko
 * @author Mr. Jerry Mouse
 */
public class ParameterData extends AnnotatedElementData<Parameter> {

    /**
     * The type descriptor representing the parameter's type.
     */
    private ClassTypeDescriptor type;

    /**
     * Constructs a new {@code ParameterData} instance associated with the given {@link Parameter}.
     *
     * @param target the reflective parameter instance
     */
    public ParameterData(Parameter target) {
        super(target);
    }

    /**
     * Retrieves the type descriptor of this parameter.
     *
     * @return the parameter's {@link ClassTypeDescriptor}
     */
    public ClassTypeDescriptor getType() {
        return type;
    }

    /**
     * Sets the type descriptor for this parameter.
     *
     * @param type the {@link ClassTypeDescriptor} to set
     */
    public void setType(ClassTypeDescriptor type) {
        this.type = type;
    }
}
