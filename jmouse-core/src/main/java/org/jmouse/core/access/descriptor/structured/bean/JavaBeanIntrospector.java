package org.jmouse.core.access.descriptor.structured.bean;

import org.jmouse.core.access.descriptor.AbstractIntrospector;
import org.jmouse.core.access.descriptor.ClassTypeDescriptor;
import org.jmouse.core.access.descriptor.ClassTypeIntrospector;
import org.jmouse.core.access.descriptor.MethodDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.jmouse.core.access.descriptor.structured.ObjectData;
import org.jmouse.core.access.descriptor.structured.ObjectIntrospector;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.reflection.InferredType;

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
     * Defines the target class type for descriptor.
     *
     * @param type the class type to analyze
     * @return this introspector instance for method chaining
     */
    public JavaBeanIntrospector<T> type(Class<T> type) {
        ClassTypeIntrospector introspector = new ClassTypeIntrospector(InferredType.forClass(type));
        container.setType(introspector.introspect().toDescriptor());
        return self();
    }

    /**
     * Performs descriptor on the Java Bean, analyzing its structure and properties.
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
    @SuppressWarnings("unchecked")
    public JavaBeanIntrospector<T> property(MethodDescriptor method) {
        String methodName    = method.getPropertyName();
        String preferredName = ObjectIntrospector.getPreferredPropertyName(method);
        String propertyName  = preferredName == null ? methodName : preferredName;

        JavaBeanPropertyIntrospector<T> introspector  = new JavaBeanPropertyIntrospector<>(null);
        PropertyDescriptor<T>           previous      = container.getProperty(propertyName);
        JavaBeanDescriptor<T>           parent        = toDescriptor();

        if (previous instanceof JavaBeanPropertyDescriptor<?> propertyDescriptor) {
            introspector = (JavaBeanPropertyIntrospector<T>) propertyDescriptor.toIntrospector();
        }

        introspector.owner(parent).name(propertyName);

        if (!isReachable(method)) {
            return self();
        }

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
     * Whether this method can actually be called, so that it may stand as a property accessor.
     *
     * <h3>⚠️ The test is "can it be opened", not "is it public"</h3>
     *
     * <p>A getter is recognised by shape — no arguments, a {@code get}/{@code is} prefix — and that
     * matches things nobody would call properties: {@code java.math.BigDecimal#isPowerOfTen()} is
     * {@code private}, and it was introspected as a readable property named {@code powerOfTen}. Reading
     * it fails with an {@code IllegalAccessException} naming a method no application wrote and no file
     * mentions, which is a poor way to learn that a source type is a JDK class.</p>
     *
     * <p>⚠️ Filtering on {@code public} would be wrong, and would break something that works: the
     * framework <em>deliberately</em> supports non-public accessors — {@code MethodAccessorFactory}
     * opens them with {@code setAccessible} and falls back to reflection when a call site cannot be
     * spun. An application's own class with package-private getters maps today. What separates that
     * from {@code BigDecimal} is not visibility but the module: a private member of a closed JDK module
     * cannot be opened at all, and one of the application's own can.</p>
     *
     * <p>So the question asked here is the one that decides the outcome, and it is asked once per
     * method at introspection rather than per read.</p>
     *
     * @param method the candidate accessor
     * @return {@code true} when it can be called
     */
    private boolean isReachable(MethodDescriptor method) {
        Method executable = method.unwrap();

        if (Modifier.isPublic(executable.getModifiers())
                && Modifier.isPublic(executable.getDeclaringClass().getModifiers())) {
            return true;
        }

        try {
            executable.setAccessible(true);

            return true;
        } catch (RuntimeException refused) {
            // InaccessibleObjectException for a closed module, SecurityException for a manager that
            // says no. Either way this member is not callable, so it is not a property.
            return false;
        }
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
