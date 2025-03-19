package org.jmouse.core.bind.descriptor.structured.jb;

import org.jmouse.core.bind.descriptor.AbstractIntrospector;
import org.jmouse.core.bind.descriptor.ClassTypeDescriptor;
import org.jmouse.core.bind.descriptor.MethodDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyData;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

/**
 * Introspector for JavaBean properties, allowing analysis and metadata extraction.
 *
 * @param <T> the type of the target JavaBean
 *
 * @author Ivan Hontarenko
 * @author Mr. Jerry Mouse
 */
public class JavaBeanPropertyIntrospector<T>
        extends AbstractIntrospector<PropertyData<T>, JavaBeanPropertyIntrospector<T>, T, JavaBeanPropertyDescriptor<T>> {

    /**
     * Constructs a JavaBean property introspector for the given target.
     *
     * @param target the target JavaBean instance
     */
    protected JavaBeanPropertyIntrospector(T target) {
        super(target);
    }

    /**
     * Sets the owner descriptor for the property.
     *
     * @param descriptor the JavaBean descriptor
     * @return this introspector instance
     */
    public JavaBeanPropertyIntrospector<T> owner(JavaBeanDescriptor<T> descriptor) {
        container.setOwner(descriptor);
        return self();
    }

    /**
     * Sets the type of the property.
     *
     * @param type the type descriptor
     * @return this introspector instance
     */
    public JavaBeanPropertyIntrospector<T> type(ClassTypeDescriptor type) {
        container.setType(type);
        return self();
    }

    /**
     * Sets the getter function for the property.
     *
     * @param getter the getter function
     * @return this introspector instance
     */
    public JavaBeanPropertyIntrospector<T> getter(Getter<T, Object> getter) {
        container.setGetter(getter);
        return self();
    }

    /**
     * Sets the setter function for the property.
     *
     * @param setter the setter function
     * @return this introspector instance
     */
    public JavaBeanPropertyIntrospector<T> setter(Setter<T, Object> setter) {
        container.setSetter(setter);
        return self();
    }

    /**
     * Sets the getter method descriptor for the property.
     *
     * @param descriptor the method descriptor
     * @return this introspector instance
     */
    public JavaBeanPropertyIntrospector<T> getterMethod(MethodDescriptor descriptor) {
        container.setGetterMethod(descriptor);
        return getter(Getter.ofMethod(descriptor.unwrap()));
    }

    /**
     * Sets the setter method descriptor for the property.
     *
     * @param descriptor the method descriptor
     * @return this introspector instance
     */
    public JavaBeanPropertyIntrospector<T> setterMethod(MethodDescriptor descriptor) {
        container.setSetterMethod(descriptor);
        return setter(Setter.ofMethod(descriptor.unwrap()));
    }

    /**
     * Sets the name of the property (implementation placeholder).
     *
     * @return this introspector instance
     */
    @Override
    public JavaBeanPropertyIntrospector<T> name() {
        return self();
    }

    /**
     * Performs descriptor on the property (implementation placeholder).
     *
     * @return this introspector instance
     */
    @Override
    public JavaBeanPropertyIntrospector<T> introspect() {
        return self();
    }

    /**
     * Converts this introspector into a property descriptor.
     *
     * @return a new {@link JavaBeanPropertyDescriptor} instance
     */
    @Override
    public JavaBeanPropertyDescriptor<T> toDescriptor() {
        return getCachedDescriptor(() -> new JavaBeanPropertyDescriptor<>(this, container));
    }

    /**
     * Creates a new property data container for the given target.
     *
     * @param target the target JavaBean instance
     * @return a new {@link PropertyData} instance
     */
    @Override
    public PropertyData<T> getContainerFor(T target) {
        return new PropertyData<>(target);
    }
}
