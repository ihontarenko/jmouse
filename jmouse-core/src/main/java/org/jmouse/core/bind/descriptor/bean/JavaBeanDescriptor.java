package org.jmouse.core.bind.descriptor.bean;

import org.jmouse.core.bind.descriptor.*;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.function.BiConsumer;

import static org.jmouse.core.bind.descriptor.MethodDescriptor.forMethod;
import static org.jmouse.core.reflection.MethodMatchers.*;
import static org.jmouse.core.reflection.MethodMatchers.getter;
import static org.jmouse.core.reflection.Reflections.getPropertyName;
import static org.jmouse.core.reflection.Reflections.getShortName;

public interface JavaBeanDescriptor<T> extends ObjectDescriptor<T> {

    /**
     * Returns the class descriptor representing the bean's type.
     *
     * @return the {@link TypeDescriptor} of the bean
     */
    TypeDescriptor getBeanClass();

    /**
     * Returns a collection of method descriptors associated with this bean.
     *
     * @return a collection of {@link MethodDescriptor} instances
     */
    default Collection<MethodDescriptor> getMethods() {
        return getBeanClass().getMethods();
    }

    /**
     * Retrieves a method descriptor by name.
     *
     * @param name the name of the method
     * @return the corresponding {@link MethodDescriptor}, or {@code null} if not found
     */
    default MethodDescriptor getMethod(String name) {
        return getBeanClass().getMethod(name);
    }

    /**
     * Checks if this class contains a method with the given name.
     *
     * @param name the name of the method
     * @return {@code true} if the method exists, otherwise {@code false}
     */
    default boolean hasMethod(String name) {
        return getBeanClass().hasMethod(name);
    }

    /**
     * Returns a collection of constructor descriptors associated with this bean.
     *
     * @return a collection of {@link ConstructorDescriptor} instances
     */
    default Collection<ConstructorDescriptor> getConstructors() {
        return getBeanClass().getConstructors();
    }

    /**
     * Returns a constructor descriptor associated with this class.
     *
     * @return a {@link ConstructorDescriptor} instance
     */
    default ConstructorDescriptor getConstructor(int index) {
        return getBeanClass().getConstructor(index);
    }

    /**
     * Returns a collection of field descriptors associated with this bean.
     *
     * @return a collection of {@link FieldDescriptor} instances
     */
    default Collection<FieldDescriptor> getFields() {
        return getBeanClass().getFields();
    }

    /**
     * Retrieves a field descriptor by name.
     *
     * @param name the name of the field
     * @return the corresponding {@link FieldDescriptor}, or {@code null} if not found
     */
    default FieldDescriptor getField(String name) {
        return getBeanClass().getField(name);
    }

    /**
     * Checks if this class contains a field with the given name.
     *
     * @param name the name of the field
     * @return {@code true} if the field exists, otherwise {@code false}
     */
    default boolean hasField(String name) {
        return getBeanClass().hasField(name);
    }

    /**
     * Default implementation of {@link JavaBeanDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing descriptor related to beans,
     * including their properties, methods, constructors, and annotations.
     * </p>
     * <p>
     * During construction, each {@link JavaBeanPropertyDescriptor} is assigned an owner reference
     * to this bean descriptor, ensuring proper linkage.
     * </p>
     *
     * @param <T> the type of the bean
     */
    class Implementation<T> extends ObjectDescriptor.Implementation<T> implements JavaBeanDescriptor<T> {

        /**
         * Constructs a new {@code BeanDescriptor.PropertyDescriptorAccessor} instance.
         *
         * @param name        the name of the bean
         * @param internal    the bean instance
         * @param annotations a set of annotation descriptors associated with this bean
         * @param type        the descriptor representing the class of this bean
         * @param properties  a map of property names to property descriptors
         */
        Implementation(
                String name,
                T internal,
                Set<AnnotationDescriptor> annotations,
                TypeDescriptor type,
                Map<String, PropertyDescriptor<T>> properties
        ) {
            super(name, internal, annotations, type, properties);
        }

        /**
         * Returns the class type being inspected.
         *
         * @return the {@link Class} bean representing the bean type
         */
        @Override
        public Class<?> getClassType() {
            return getBeanClass().getClassType();
        }

        /**
         * Returns the class descriptor representing the bean's type.
         *
         * @return the {@link TypeDescriptor} of the bean
         */
        @Override
        public TypeDescriptor getBeanClass() {
            return type;
        }

        /**
         * Returns a collection of method descriptors associated with this bean.
         *
         * @return a collection of {@link MethodDescriptor} instances
         */
        @Override
        public Collection<MethodDescriptor> getMethods() {
            return getBeanClass().getMethods();
        }

        /**
         * Returns a collection of constructor descriptors associated with this bean.
         *
         * @return a collection of {@link ConstructorDescriptor} instances
         */
        @Override
        public Collection<ConstructorDescriptor> getConstructors() {
            return getBeanClass().getConstructors();
        }

        /**
         * Returns a collection of field descriptors associated with this bean.
         *
         * @return a collection of {@link FieldDescriptor} instances
         */
        @Override
        public Collection<FieldDescriptor> getFields() {
            return getBeanClass().getFields();
        }

        /**
         * Returns a string representation of this bean descriptor.
         *
         * @return a string representation of this bean descriptor
         */
        @Override
        public String toString() {
            return "[%s] : %s".formatted(type.isRecord() ? "VO" : "JB", super.toString());
        }
    }

    /**
     * A builder class for constructing {@link JavaBeanDescriptor} instances.
     * <p>
     * This builder extends {@link ObjectDescriptor.Builder} and provides methods
     * for configuring and constructing a {@link JavaBeanDescriptor} that represents
     * a JavaBean with its associated properties and descriptor.
     * </p>
     *
     * @param <T> the type of the JavaBean being described
     */
    class Builder<T> extends ObjectDescriptor.Builder<T> {

        /**
         * Constructs a new {@code JavaBeanDescriptor.Mutable}.
         *
         * @param name the name of the JavaBean being built
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Builds and returns a {@link JavaBeanDescriptor} instance.
         * <p>
         * The resulting descriptor is immutable, with its annotations and properties
         * wrapped in unmodifiable collections.
         * </p>
         *
         * @return a new instance of {@link JavaBeanDescriptor}
         */
        @Override
        public JavaBeanDescriptor<T> toImmutable() {
            return new Implementation<>(
                    name,
                    target,
                    Collections.unmodifiableSet(annotations),
                    descriptor,
                    Collections.unmodifiableMap(properties)
            );
        }
    }

    /**
     * Creates a descriptor for a JavaBean.
     * <p>
     * This method analyzes the given class and generates a {@link JavaBeanDescriptor},
     * detecting properties based on JavaBean getter/setter conventions.
     * </p>
     *
     * @param type the class to describe
     * @param <T>  the type of the bean
     * @return a {@link JavaBeanDescriptor} instance representing the bean
     */
    static <T> JavaBeanDescriptor<T> forBean(Class<T> type) {
        return forBean(type, null);
    }

    /**
     * Creates a descriptor for a JavaBean with an optional instance.
     * <p>
     * If an instance is provided, it is stored inside the descriptor for further inspection.
     * The descriptor is constructed based on JavaBean conventions for property detection.
     * </p>
     *
     * @param type the class to describe
     * @param bean an optional instance of the bean
     * @param <T>  the type of the bean
     * @return a {@link JavaBeanDescriptor} instance representing the bean
     */
    static <T> JavaBeanDescriptor<T> forBean(Class<T> type, T bean) {
        return forBean(type, bean, TypeDescriptor.DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for a JavaBean with an optional instance and specified depth.
     * <p>
     * The method scans for getter and setter methods to construct property descriptors.
     * The recursion depth determines how deeply nested types should be resolved.
     * </p>
     *
     * @param type  the class to describe
     * @param bean  an optional instance of the bean
     * @param depth the recursion depth limit for nested descriptor resolution
     * @param <T>   the type of the bean
     * @return a {@link JavaBeanDescriptor} instance representing the bean
     */
    static <T> JavaBeanDescriptor<T> forBean(Class<T> type, T bean, int depth) {
        // Map to store property builders
        Map<String, JavaBeanPropertyDescriptor.Builder<T>> builders = new HashMap<>();

        // Initialize the BeanDescriptor builder with the short name of the class
        JavaBeanDescriptor.Builder<T> builder = new JavaBeanDescriptor.Builder<>(getShortName(type));

        // Generate the class descriptor for the given type
        TypeDescriptor typeDescriptor = TypeDescriptor.forType(JavaType.forType(type), depth);

        // Define matchers for getter and setter methods
        Matcher<Executable> anyMatcher = getter().or(setter());
        BiConsumer<JavaBeanPropertyDescriptor.Builder<T>, MethodDescriptor> getterMethod
                = JavaBeanPropertyDescriptor.Builder::getterMethod;
        BiConsumer<JavaBeanPropertyDescriptor.Builder<T>, MethodDescriptor> setterMethod
                = JavaBeanPropertyDescriptor.Builder::setterMethod;

        // Iterate over all methods in the class descriptor
        for (MethodDescriptor method : typeDescriptor.getMethods()) {
            Method rawMethod = method.unwrap();

            // Check if the method matches getter or setter patterns
            if (anyMatcher.matches(rawMethod)) {
                BiConsumer<JavaBeanPropertyDescriptor.Builder<T>, MethodDescriptor> adder = (a, b) -> {};
                String                                                              name  = null;

                // Determine if the method is a getter ("is" or "get" prefix)
                if (getter().matches(rawMethod) && prefixIs().matches(rawMethod)) {
                    name = getPropertyName(rawMethod, GETTER_IS_PREFIX);
                    adder = getterMethod;
                } else if (getter().matches(rawMethod)) {
                    name = getPropertyName(rawMethod, GETTER_GET_PREFIX);
                    adder = getterMethod;
                }
                // Determine if the method is a setter ("set" prefix)
                else if (setter().matches(rawMethod)) {
                    name = getPropertyName(rawMethod, SETTER_PREFIX);
                    adder = setterMethod;
                }

                // Associate the method with a property descriptor
                adder.accept(builders.computeIfAbsent(name, JavaBeanPropertyDescriptor.Builder::new), forMethod(rawMethod, depth - 1));

                // Current builder that should be completed
                JavaBeanPropertyDescriptor.Builder<T> propertyBuilder = builders.get(name);

                // Copy annotations from method to property descriptor
                if (propertyBuilder != null) {
                    method.getAnnotations().forEach(propertyBuilder::annotation);

                    if (setter().matches(rawMethod)) {
                        propertyBuilder.setter(Setter.ofMethod(rawMethod));
                    } else if (getter().matches(rawMethod)) {
                        propertyBuilder.getter(Getter.ofMethod(rawMethod));
                    }

                    // Copy annotations from corresponding field (if exists)
                    if (typeDescriptor.hasField(name)) {
                        typeDescriptor.getField(name).getAnnotations().forEach(propertyBuilder::annotation);
                    }
                }
            }
        }

        // Assign the class descriptor to the bean descriptor
        builder.descriptor(typeDescriptor);

        // If a bean instance is provided, attach it to the descriptor
        if (bean != null) {
            builder.bean(bean).target(bean);
        }

        // Build the final BeanDescriptor instance
        JavaBeanDescriptor<T> beanDescriptor = builder.toImmutable();

        // Assign properties to the final descriptor
        for (JavaBeanPropertyDescriptor.Builder<T> property : builders.values()) {
            builder.property(property.owner(beanDescriptor).toImmutable());
        }

        return beanDescriptor;
    }

    /**
     * Creates a descriptor for a record (value bean).
     * <p>
     * This method detects record components and generates a {@link JavaBeanDescriptor},
     * treating them as immutable properties.
     * </p>
     *
     * @param type the record class to describe
     * @param <T>  the type of the record
     * @return a {@link JavaBeanDescriptor} instance representing the record
     */
    static <T extends Record> JavaBeanDescriptor<T> forRecord(Class<? super T> type) {
        return forRecord(type, null);
    }

    /**
     * Creates a descriptor for a record (value bean) with an optional instance.
     * <p>
     * This method extracts descriptor from record components and generates a descriptor
     * treating them as immutable properties.
     * </p>
     *
     * @param type the record class to describe
     * @param bean an optional instance of the record
     * @param <T>  the type of the record
     * @return a {@link JavaBeanDescriptor} instance representing the record
     */
    static <T extends Record> JavaBeanDescriptor<T> forRecord(Class<? super T> type, T bean) {
        return forRecord(type, bean, TypeDescriptor.DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for a record (value bean) with an optional instance and specified depth.
     * <p>
     * Unlike standard JavaBeans, records do not have setters; their components are immutable.
     * This method detects record components and maps them to property descriptors.
     * </p>
     *
     * @param type  the record class to describe
     * @param bean  an optional instance of the record
     * @param depth the recursion depth limit for nested descriptor resolution
     * @param <T>   the type of the record
     * @return a {@link JavaBeanDescriptor} instance representing the record
     */
    static <T extends Record> JavaBeanDescriptor<T> forRecord(Class<? super T> type, T bean, int depth) {
        // Initialize the builder for the BeanDescriptor using the class short name
        JavaBeanDescriptor.Builder<T> builder = new JavaBeanDescriptor.Builder<>(getShortName(type));

        // Generate a ClassDescriptor for the given record type
        TypeDescriptor descriptor = TypeDescriptor.forType(JavaType.forClass(type), depth);

        // Assign the class descriptor to the builder
        builder.descriptor(descriptor);

        // If an instance of the record is provided, attach it to the descriptor
        if (bean != null) {
            builder.bean(bean).target(bean);
        }

        // Build the BeanDescriptor for the record
        JavaBeanDescriptor<T> beanDescriptor = builder.toImmutable();

        // Iterate over all record components (fields implicitly declared in the record)
        for (RecordComponent component : type.getRecordComponents()) {
            // Initialize a property descriptor builder for each record component
            JavaBeanPropertyDescriptor.Builder<T> propertyBuilder = new JavaBeanPropertyDescriptor.Builder<>(component.getName());

            // Link the getter method of the record component
            propertyBuilder.owner(beanDescriptor).getterMethod(forMethod(component.getAccessor(), depth - 1));
            propertyBuilder.getter(Getter.ofMethod(component.getAccessor()));

            // Add the property to the bean descriptor
            builder.property(propertyBuilder.toImmutable());

            // If the field exists in the descriptor, copy its annotations to the property
            if (descriptor.hasField(component.getName())) {
                FieldDescriptor field = descriptor.getField(component.getName());
                field.getAnnotations().forEach(propertyBuilder::annotation);
            }

            // If a method with the same name exists, copy its annotations to the property
            if (descriptor.hasMethod(component.getName())) {
                MethodDescriptor method = descriptor.getMethod(component.getName());
                method.getAnnotations().forEach(propertyBuilder::annotation);
            }
        }

        // Return the BeanDescriptor for the record
        return beanDescriptor;
    }
}
