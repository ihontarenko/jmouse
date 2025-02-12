package org.jmouse.core.bind;

import org.jmouse.core.bind.descriptor.bean.JavaBeanDescriptor;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.TypeDescriptor;
import org.jmouse.util.Factory;
import org.jmouse.util.Priority;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static org.jmouse.core.bind.Bindable.of;

/**
 * A binder implementation that supports binding JavaBeans.
 * <p>
 * This binder inspects the properties of a JavaBean and attempts to populate them
 * from the provided {@link DataSource}.
 * </p>
 */
@Priority(Integer.MAX_VALUE)
public class JavaBeanBinder extends AbstractBinder {

    /**
     * Creates a {@link JavaBeanBinder} with the given binding context.
     *
     * @param context the binding context to use
     */
    public JavaBeanBinder(BindContext context) {
        super(context);
    }

    /**
     * Binds the given {@link Bindable} JavaBean to values from the {@link DataSource}.
     * <p>
     * This method checks if the target type is an object or scalar, and delegates to
     * {@link #bindValue(NamePath, Bindable, DataSource, BindCallback)} if necessary. Otherwise, it
     * iterates over the JavaBean's properties and attempts to bind each writable
     * property.
     * </p>
     *
     * @param name     the structured name path of the binding target
     * @param bindable the bindable instance representing the JavaBean
     * @param source   the data source from which values are retrieved
     * @param callback the binding callback for customization
     * @param <T>      the type of the JavaBean
     * @return a {@link BindResult} containing the bound JavaBean instance
     */
    @Override
    public <T> BindResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source, BindCallback callback) {
        TypeDescriptor typeDescriptor = bindable.getTypeDescriptor();
        JavaType       type           = bindable.getType();
        JavaBean<T>    bean           = JavaBean.of(type);

        JavaBeanDescriptor.forBean(type.getRawType());

        // Obtain a factory that can create new instances of the class
        Factory<T> factory = bean.getFactory(bindable);

        // If the type is either a simple object or a scalar value, bind it directly
        if (typeDescriptor.isObject() || typeDescriptor.isScalar()) {
            return bindValue(name, bindable, source, callback);
        }

        // Iterate through all the properties of the JavaBean to map values
        for (Bean.Property<T> property : bean.getProperties()) {
            JavaType         propertyType = property.getType();
            Supplier<Object> value        = property.getValue(factory);
            NamePath         propertyName = NamePath.of(getPreferredName(property));

            // Check if the property is writable then perform value binding for the property
            if (property.isWritable()) {
                NamePath           propertyPath     = name.append(propertyName);
                Bindable<Object>   bindableProperty = of(propertyType).withSuppliedInstance(value);
                BindResult<Object> result           = bindValue(propertyPath, bindableProperty, source, callback);

                checkRequirements(result, propertyName, property);

                // If no value was found in the data source, skip this property
                if (result.isEmpty()) {
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
     * If the property has a setter method annotated with {@link PropertyRequired},
     * and no value present will be thrown {@link IllegalStateException}
     * </p>
     */
    protected void checkRequirements(BindResult<?> result, NamePath propertyName, Bean.Property<?> property) {
        if (result.isEmpty()) {
            if (property.getRawSetter().isAnnotationPresent(PropertyRequired.class)) {
                throw new IllegalStateException("No value for required property '%s' in path '%s'"
                        .formatted(property, propertyName));
            }
        }
    }

    /**
     * Determines the preferred property name for binding.
     * <p>
     * If the property has a setter method annotated with {@link PropertyPath},
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
            if (setter != null && setter.isAnnotationPresent(PropertyPath.class)) {
                String preferredPath = setter.getAnnotation(PropertyPath.class).value();
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
        return bindable.getTypeDescriptor().isBean();
    }
}
