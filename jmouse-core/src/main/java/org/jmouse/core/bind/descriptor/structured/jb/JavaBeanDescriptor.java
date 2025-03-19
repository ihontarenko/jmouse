package org.jmouse.core.bind.descriptor.structured.jb;

import org.jmouse.core.bind.descriptor.AbstractDescriptor;
import org.jmouse.core.bind.descriptor.ClassTypeDescriptor;
import org.jmouse.core.bind.descriptor.structured.ObjectData;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.reflection.ClassTypeInspector;

import java.util.Map;

/**
 * Represents a descriptor for a JavaBean, providing metadata about its structure and properties.
 * <p>
 * This class extends {@link AbstractDescriptor} and implements {@link ObjectDescriptor},
 * allowing descriptor of JavaBeans, including their properties and types.
 * </p>
 *
 * @param <T> the type of the JavaBean being described
 *
 * @author JMouse - Team
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public class JavaBeanDescriptor<T>
        extends AbstractDescriptor<T, ObjectData<T>, JavaBeanIntrospector<T>>
        implements ClassTypeInspector, ObjectDescriptor<T> {

    /**
     * Constructs a {@code JavaBeanDescriptor} with the given introspector and data container.
     *
     * @param introspector the introspector responsible for analyzing the JavaBean structure
     * @param container    the container holding metadata about the JavaBean
     */
    protected JavaBeanDescriptor(JavaBeanIntrospector<T> introspector, ObjectData<T> container) {
        super(introspector, container);
    }

    /**
     * Returns the type descriptor of the JavaBean.
     *
     * @return the type descriptor of the JavaBean
     */
    public ClassTypeDescriptor getType() {
        return container.getType();
    }

    /**
     * Sets the type descriptor for the JavaBean.
     *
     * @param type the type descriptor to set
     */
    @Override
    public void setType(ClassTypeDescriptor type) {
        container.setType(type);
    }

    /**
     * Returns a map of property descriptors representing the JavaBean properties.
     *
     * @return a map of property descriptors
     */
    @Override
    public Map<String, PropertyDescriptor<T>> getProperties() {
        return container.getProperties();
    }

    /**
     * Returns the introspector associated with this descriptor.
     *
     * @return the {@link JavaBeanIntrospector} instance used for descriptor
     */
    @Override
    public JavaBeanIntrospector<T> toIntrospector() {
        return introspector;
    }

    /**
     * Returns the raw class type of the JavaBean.
     *
     * @return the {@link Class} representing the JavaBean type
     */
    @Override
    public Class<?> getClassType() {
        return container.getType().getClassType();
    }

    /**
     * Returns a string representation of this JavaBean descriptor.
     *
     * @return a formatted string representing the JavaBean type
     */
    @Override
    public String toString() {
        return "[JB]: " + container.getType().getJavaType();
    }
}
