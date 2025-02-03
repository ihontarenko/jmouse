package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.TypeDescriptor;
import org.jmouse.util.Priority;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static org.jmouse.context.binding.Bindable.of;

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
     * {@link #bindValue(NamePath, Bindable, DataSource)} if necessary. Otherwise, it
     * iterates over the JavaBean's properties and attempts to bind each writable
     * property.
     * </p>
     *
     * @param name     the structured name path of the binding target
     * @param bindable the bindable instance representing the JavaBean
     * @param source   the data source from which values are retrieved
     * @param <T>      the type of the JavaBean
     * @return a {@link BindingResult} containing the bound JavaBean instance
     */
    @Override
    public <T> BindingResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source) {
        TypeDescriptor typeDescriptor = bindable.getTypeDescriptor();
        JavaType       type           = bindable.getType();
        JavaBean<T>    bean           = JavaBean.of(type);
        Supplier<T>    supplier       = bean.getSupplier(bindable);

        if (typeDescriptor.isObject() || typeDescriptor.isScalar()) {
            return bindValue(name, bindable, source);
        }

        for (JavaBean.Property property : bean.getProperties()) {
            JavaType         propertyType = property.getType();
            Supplier<Object> value        = property.getValue(supplier);
            NamePath         propertyName = NamePath.of(getPreferredName(property));

            if (property.isWritable()) {
                BindingResult<Object> result = bindValue(
                        name.append(propertyName), of(propertyType).withInstance(value), source);

                if (result.isEmpty()) {
                    continue;
                }

                property.setValue(supplier, result.getValue());
            }
        }

        return BindingResult.of(supplier.get());
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
    protected String getPreferredName(JavaBean.Property property) {
        String name = property.getName();

        if (property.isWritable()) {
            Method setter = property.getSetter();
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
     * <p>
     * This implementation always returns {@code true}, as it acts as a fallback binder
     * that handles JavaBeans by default.
     * </p>
     *
     * @param bindable the bindable instance to check
     * @param <T>      the type of the bindable
     * @return {@code true}, indicating support for all bindable types
     */
    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return true;
    }
}
