package org.jmouse.core.bind;

import org.jmouse.core.bind.descriptor.bean.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.bean.ObjectDescriptor;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.Factory;
import org.jmouse.util.Priority;
import org.jmouse.util.helper.Strings;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static org.jmouse.core.bind.Bindable.of;
import static org.jmouse.core.reflection.Reflections.getAnnotationValue;

/**
 * A binder implementation that supports binding JavaBeans.
 * <p>
 * This binder inspects the properties of a JavaBean and attempts to populate them
 * from the provided {@link PropertyValuesAccessor}.
 * </p>
 */
@Priority(JavaBeanBinder.PRIORITY)
public class JavaBeanBinder extends AbstractBinder {

    public static final int PRIORITY = 1000;

    /**
     * Creates a {@link JavaBeanBinder} with the given binding context.
     *
     * @param context the binding context to use
     */
    public JavaBeanBinder(BindContext context) {
        super(context);
    }

    /**
     * Binds the given {@link Bindable} JavaBean to values from the {@link PropertyValuesAccessor}.
     * <p>
     * This method checks if the target type is an object or scalar, and delegates to
     * {@link #bindValue(PropertyPath, Bindable, PropertyValuesAccessor, BindCallback)} if necessary. Otherwise, it
     * iterates over the JavaBean's properties and attempts to bind each writable
     * property.
     * </p>
     *
     * @param name     the structured name path of the binding target
     * @param bindable the bindable instance representing the JavaBean
     * @param accessor   the data source from which values are retrieved
     * @param callback the binding callback for customization
     * @param <T>      the type of the JavaBean
     * @return a {@link BindResult} containing the bound JavaBean instance
     */
    @Override
    public <T> BindResult<T> bind(PropertyPath name, Bindable<T> bindable, PropertyValuesAccessor accessor, BindCallback callback) {
        JavaType    type = bindable.getType();
        JavaBean<T> bean = JavaBean.of(type);

        ObjectDescriptor<T> descriptor = JavaBeanDescriptor.forBean(type.getRawType());

        // Obtain a factory that can create new instances of the class
        Factory<T> factory = bean.getFactory(bindable);

        // If the type is either a simple object or a scalar value, bind it directly
        if (type.isObject() || type.isScalar()) {
            return bindValue(name, bindable, accessor, callback);
        }

        // Iterate through all the properties of the JavaBean to map values
        for (Bean.Property<T> property : bean.getProperties()) {
            PropertyPath     propertyName = PropertyPath.forPath(getPreferredName(property));
            JavaType         propertyType = property.getType();
            Supplier<Object> value        = property.getValue(factory);

            // Check if the property is writable then perform value binding for the property
            if (property.isWritable()) {
                PropertyPath       propertyPath     = name.append(propertyName);
                Bindable<Object>   bindableProperty = of(propertyType).withSuppliedInstance(value);
                BindResult<Object> result           = bindValue(propertyPath, bindableProperty, accessor, callback);

                checkRequirements(result, propertyName, property);

                // If no value was found in the data source, skip this property
                if (result.isEmpty()) {
                    callback.onUnbound(propertyPath, bindableProperty, context);
                    continue;
                }

                property.setValue(factory, result.getValue());

                callback.onBound(propertyPath, bindableProperty, context, result);
            }
        }

        return BindResult.of(factory.create());
    }

    /**
     * Check if property is required
     * <p>
     * If the property has a setter method annotated with {@link BindRequired},
     * and no value present will be thrown {@link IllegalStateException}
     * </p>
     */
    protected void checkRequirements(BindResult<?> result, PropertyPath propertyName, Bean.Property<?> property) {
        if (result.isEmpty()) {
            if (property.getRawSetter().isAnnotationPresent(BindRequired.class)) {
                String message = getAnnotationValue(property.getRawSetter(), BindRequired.class, BindRequired::value);

                if (Strings.isEmpty(message)) {
                    message = "Property '%s' required to be bound but value do not present on path: '%s'"
                            .formatted(property, propertyName);
                }

                throw new IllegalStateException(message);
            }
        }
    }

    /**
     * Determines the preferred property name for binding.
     * <p>
     * If the property has a setter method annotated with {@link BindName},
     * the preferred name is taken from the annotation. Otherwise, the default
     * property name is used.
     * </p>
     *
     * @param property the JavaBean property
     * @return the preferred property name
     */
    protected String getPreferredName(Bean.Property<?> property) {
        String name = property.getName();

        if (property.isWritable()) {
            Method setter = property.getRawSetter();
            if (setter != null && setter.isAnnotationPresent(BindName.class)) {
                String preferredPath = setter.getAnnotation(BindName.class).value();
                if (preferredPath != null && !preferredPath.isEmpty()) {
                    name = preferredPath;
                }
            }
        }

        return name;
    }

    /**
     * Determines whether this binder supports the given {@link Bindable}.
     *
     * @param bindable the bindable instance to check
     * @param <T>      the type of the bindable
     * @return {@code true}, indicating support for all bindable types
     */
    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return bindable.getType().isBean();
    }
}
