package org.jmouse.core.bind.introspection.structured.jb;

import org.jmouse.core.bind.introspection.AbstractIntrospector;
import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.ClassTypeIntrospector;
import org.jmouse.core.bind.introspection.MethodDescriptor;
import org.jmouse.core.bind.introspection.structured.ObjectData;
import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;
import org.jmouse.core.reflection.JavaType;

import java.util.Map;

/**
 * An introspector for Java Beans, allowing reflection-based analysis of their structure.
 * <p>
 * This class extracts metadata about a Java Bean, including its properties and methods,
 * and provides a structured representation of its components.
 * </p>
 *
 * @param <T> the type of the Java Bean being introspected
 *
 * @author JMouse - Team
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public class JavaBeanIntrospector<T>
        extends AbstractIntrospector<ObjectData<T>, JavaBeanIntrospector<T>, T, JavaBeanDescriptor<T>> {

    /**
     * Constructs an introspector for the specified Java Bean type.
     *
     * @param target the class of the Java Bean to introspect
     */
    public JavaBeanIntrospector(Class<T> target) {
        super(null);
        type(target);
    }

    /**
     * Sets the name of the Java Bean introspector based on the class name.
     *
     * @return this introspector instance for method chaining
     */
    @Override
    public JavaBeanIntrospector<T> name() {
        return name(container.getType().getName());
    }

    /**
     * Defines the target class type for introspection.
     *
     * @param type the class type to analyze
     * @return this introspector instance for method chaining
     */
    public JavaBeanIntrospector<T> type(Class<T> type) {
        ClassTypeIntrospector introspector = new ClassTypeIntrospector(JavaType.forClass(type));
        container.setType(introspector.introspect().toDescriptor());
        return self();
    }

    /**
     * Performs introspection on the Java Bean, analyzing its structure and properties.
     *
     * @return this introspector instance for method chaining
     */
    @Override
    public JavaBeanIntrospector<T> introspect() {
        return name().properties();
    }

    /**
     * Extracts properties from the Java Bean based on its methods.
     *
     * @return this introspector instance for method chaining
     */
    public JavaBeanIntrospector<T> properties() {
        ClassTypeDescriptor descriptor = container.getType();

        for (Map.Entry<String, MethodDescriptor> entry : descriptor.getMethods().entrySet()) {
            MethodDescriptor method = entry.getValue();
            property(method);
        }

        return self();
    }

    /**
     * Processes a method and determines whether it represents a getter or setter.
     * If so, the method is associated with a corresponding property descriptor.
     *
     * @param method the method descriptor to analyze
     * @return this introspector instance for method chaining
     */
    public JavaBeanIntrospector<T> property(MethodDescriptor method) {
        String                          name         = method.getPropertyName();
        JavaBeanPropertyIntrospector<T> introspector = new JavaBeanPropertyIntrospector<>(null);
        PropertyDescriptor<T>           previous     = container.getProperty(name);
        JavaBeanDescriptor<T>           parent       = toDescriptor();

        if (previous instanceof JavaBeanPropertyDescriptor<?> propertyDescriptor) {
            introspector = (JavaBeanPropertyIntrospector<T>) propertyDescriptor.toIntrospector();
        }

        introspector.owner(parent).name(name);

        if (method.isGetter() || method.isSetter()) {
            if (method.isGetter()) {
                introspector.getterMethod(method);
            } else if (method.isSetter()) {
                introspector.setterMethod(method);
            }

            container.addProperty(introspector.toDescriptor());
        }

        return self();
    }

    /**
     * Converts this introspector into a Java Bean descriptor.
     *
     * @return a {@link JavaBeanDescriptor} representing the introspected Java Bean
     */
    @Override
    public JavaBeanDescriptor<T> toDescriptor() {
        return getCachedDescriptor(() -> new JavaBeanDescriptor<>(this, container));
    }

    /**
     * Creates an {@link ObjectData} container for the given target object.
     *
     * @param target the target object
     * @return an instance of {@link ObjectData} containing the target object
     */
    @Override
    public ObjectData<T> getContainerFor(T target) {
        return new ObjectData<>(target);
    }

    /**
     * Returns a string representation of this introspector.
     *
     * @return a formatted string representation of the Java Bean introspector
     */
    @Override
    public String toString() {
        return "JB-Introspector: " + container;
    }
}
