package org.jmouse.core.bind.descriptor.structured.jb;

import org.jmouse.core.bind.descriptor.AbstractDescriptor;
import org.jmouse.core.bind.descriptor.ClassTypeDescriptor;
import org.jmouse.core.bind.descriptor.ClassTypeIntrospector;
import org.jmouse.core.bind.descriptor.MethodDescriptor;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyData;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.Getter;
import org.jmouse.core.Setter;

/**
 * Represents a descriptor for a JavaBean property, providing metadata about its getter, setter,
 * type, and owner object.
 * <p>
 * This class extends {@link AbstractDescriptor} and implements {@link PropertyDescriptor},
 * allowing detailed descriptor of JavaBean properties.
 * </p>
 *
 * @param <T> the type of the JavaBean that owns this property
 *
 * @author JMouse - Team
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public class JavaBeanPropertyDescriptor<T>
        extends AbstractDescriptor<T, PropertyData<T>, JavaBeanPropertyIntrospector<T>> implements PropertyDescriptor<T> {

    /**
     * Constructs a {@code JavaBeanPropertyDescriptor} with the given introspector and data container.
     *
     * @param introspector the introspector responsible for analyzing the property structure
     * @param container    the container holding metadata about the property
     */
    protected JavaBeanPropertyDescriptor(JavaBeanPropertyIntrospector<T> introspector, PropertyData<T> container) {
        super(introspector, container);
    }

    /**
     * Returns the getter function for accessing the property value.
     *
     * @return the {@link Getter} for the property
     */
    @Override
    public Getter<T, Object> getGetter() {
        return container.getGetter();
    }

    /**
     * Sets the getter function for accessing the property value.
     *
     * @param getter the {@link Getter} to set
     */
    @Override
    public void setGetter(Getter<T, ?> getter) {
        container.setGetter(getter);
    }

    /**
     * Returns the setter function for modifying the property value.
     *
     * @return the {@link Setter} for the property
     */
    @Override
    public Setter<T, Object> getSetter() {
        return container.getSetter();
    }

    /**
     * Sets the setter function for modifying the property value.
     *
     * @param setter the {@link Setter} to set
     */
    @Override
    public void setSetter(Setter<T, ?> setter) {
        container.setSetter(setter);
    }

    /**
     * Returns the descriptor for the getter method of this property.
     *
     * @return the {@link MethodDescriptor} representing the getter method
     */
    @Override
    public MethodDescriptor getGetterMethod() {
        return container.getGetterMethod();
    }

    /**
     * Sets the descriptor for the getter method of this property.
     *
     * @param getterMethod the {@link MethodDescriptor} to set
     */
    @Override
    public void setGetterMethod(MethodDescriptor getterMethod) {
        container.setGetterMethod(getterMethod);
    }

    /**
     * Returns the descriptor for the setter method of this property.
     *
     * @return the {@link MethodDescriptor} representing the setter method
     */
    @Override
    public MethodDescriptor getSetterMethod() {
        return container.getSetterMethod();
    }

    /**
     * Sets the descriptor for the setter method of this property.
     *
     * @param setterMethod the {@link MethodDescriptor} to set
     */
    @Override
    public void setSetterMethod(MethodDescriptor setterMethod) {
        container.setSetterMethod(setterMethod);
    }

    /**
     * Returns the type descriptor of the property.
     * <p>
     * If the type is not yet set and the property is writable, it attempts to infer
     * the type from the setter method parameter.
     * </p>
     *
     * @return the {@link ClassTypeDescriptor} representing the property type
     */
    @Override
    public ClassTypeDescriptor getType() {
        ClassTypeDescriptor type = container.getType();

        if (type == null) {
            JavaType javaType = null;

            if (isWritable()) {
                javaType = JavaType.forParameter(getSetterMethod().unwrap(), 0);
            } else if (isReadable()) {
                javaType = JavaType.forMethodReturnType(getGetterMethod().unwrap());
            }

            if (javaType != null) {
                ClassTypeIntrospector introspector = new ClassTypeIntrospector(javaType);
                toIntrospector().type(introspector.name().toDescriptor());
                type = container.getType();
            }
        }

        return type;
    }

    /**
     * Sets the type descriptor for this property.
     *
     * @param type the {@link ClassTypeDescriptor} to set
     */
    @Override
    public void setType(ClassTypeDescriptor type) {
        container.setType(type);
    }

    /**
     * Returns the owner descriptor that contains this property.
     *
     * @return the {@link ObjectDescriptor} representing the owner of this property
     */
    @Override
    public ObjectDescriptor<T> getOwner() {
        return container.getOwner();
    }

    /**
     * Returns the introspector associated with this property descriptor.
     *
     * @return the {@link JavaBeanPropertyIntrospector} used for descriptor
     */
    @Override
    public JavaBeanPropertyIntrospector<T> toIntrospector() {
        return introspector;
    }

    /**
     * Returns a string representation of this JavaBean property descriptor.
     *
     * @return a formatted string representing the property name and type
     */
    @Override
    public String toString() {
        return "[%s]: %s".formatted(getName(), getType());
    }
}
