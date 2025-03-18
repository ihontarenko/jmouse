package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.ClassTypeData;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.MethodFinder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Introspects Java class types, including constructors, fields, and methods,
 * collecting detailed metadata through reflection.
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * ClassTypeDescriptor descriptor = new ClassTypeIntrospector(UserService.class)
 *     .introspect()
 *     .toDescriptor();
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ClassTypeIntrospector extends AnnotatedElementIntrospector<ClassTypeData, ClassTypeIntrospector, Class<?>, ClassTypeDescriptor> {

    protected ClassTypeIntrospector(Class<?> type) {
        this(JavaType.forClass(type));
    }

    public ClassTypeIntrospector(JavaType type) {
        super(type.getRawType());
        type(type);
    }

    /**
     * Sets the introspector's name based on the target class.
     */
    @Override
    public ClassTypeIntrospector name() {
        return name(Describer.className(container.getTarget()));
    }

    /**
     * Initializes type information based on the target class.
     */
    public ClassTypeIntrospector type() {
        return type(JavaType.forClass(container.getTarget()));
    }

    /**
     * Sets explicit JavaType information.
     */
    public ClassTypeIntrospector type(JavaType type) {
        container.setType(type);
        return self();
    }

    /**
     * Adds a specific constructor descriptor.
     */
    public ClassTypeIntrospector constructor(ConstructorDescriptor constructor) {
        container.addConstructor(constructor);
        return self();
    }

    /**
     * Introspects and registers all public constructors.
     */
    public ClassTypeIntrospector constructors() {
        for (Constructor<?> constructor : container.getTarget().getConstructors()) {
            constructor(new ConstructorIntrospector(constructor).introspect().toDescriptor());
        }
        return self();
    }

    /**
     * Adds a specific field descriptor.
     */
    public ClassTypeIntrospector field(FieldDescriptor field) {
        container.addField(field);
        return self();
    }

    /**
     * Introspects and registers all public fields.
     */
    public ClassTypeIntrospector fields() {
        for (Field field : container.getTarget().getFields()) {
            field(new FieldIntrospector(field).introspect().toDescriptor());
        }
        return self();
    }

    /**
     * Adds a specific method descriptor.
     */
    public ClassTypeIntrospector method(MethodDescriptor method) {
        container.addMethod(method);
        return self();
    }

    /**
     * Introspects and registers all declared methods.
     */
    public ClassTypeIntrospector methods() {
        Collection<Method> methods = new MethodFinder().getMembers(container.getType().getRawType(), true);

        for (Method method : methods) {
            method(new MethodIntrospector(method).introspect().toDescriptor());
        }

        return self();
    }

    /**
     * Performs a complete introspection of class details (name, annotations,
     * constructors, methods, fields).
     */
    @Override
    public ClassTypeIntrospector introspect() {
        return name().annotations().constructors().methods().fields();
    }

    /**
     * Returns the fully introspected class descriptor.
     */
    @Override
    public ClassTypeDescriptor toDescriptor() {
        return getCachedDescriptor(() -> new ClassTypeDescriptor(this, container));
    }

    /**
     * Creates a new container for introspection data based on the provided target class.
     */
    @Override
    public ClassTypeData getContainerFor(Class<?> target) {
        return new ClassTypeData(target);
    }
}
