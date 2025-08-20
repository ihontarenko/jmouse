package org.jmouse.core.bind;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.reflection.TypeInformation;

/**
 * Base implementation of the {@link ObjectBinder} interface.
 * <p>
 * This abstract class provides common logic for handling value binding,
 * type conversion, and deep binding delegation.
 * It is intended to be extended by other binders that define specific
 * binding behavior for different scenarios.
 * </p>
 */
abstract public class AbstractBinder implements ObjectBinder {

    /**
     * The binding context used for managing the binding process.
     */
    protected final BindContext context;

    /**
     * Constructs an {@link AbstractBinder} with the given binding context.
     *
     * @param context the binding context
     */
    public AbstractBinder(BindContext context) {
        this.context = context;
    }

    /**
     * Attempts to bind a scalar or object value from the {@link ObjectAccessor}.
     * <p>
     * If the requested name is present in the accessor, this method attempts to
     * retrieve and convert the value based on the target {@link TypeInformation}.
     * If deep binding is enabled, it delegates to the root binder for nested binding.
     * </p>
     *
     * @param name     the structured name path of the binding target
     * @param bindable the bindable instance representing the target type
     * @param accessor the data accessor containing the value
     * @param callback the binding callback for customization
     * @param <T>      the type of the target object
     * @return a {@link BindResult} containing the bound value, or empty if binding failed
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public <T> BindResult<T> bindValue(
            PropertyPath name, Bindable<T> bindable, ObjectAccessor accessor, BindCallback callback) {
        PropertyPath   path  = callback.onKeyCreated(name, context);
        ObjectAccessor value = accessor.navigate(path);

        // If the value is null, return an empty binding result
        if (value.isNull()) {
            return BindResult.empty();
        }

        bindable = (Bindable<T>) refreshBindable(bindable, value);

        TypeInformation descriptor      = bindable.getTypeInformation();
        TypeInformation valueDescriptor = TypeInformation.forClass(value.getDataType());
        ObjectBinder    rootBinder      = context.getRootBinder();

        boolean isConvertible = context.getConversion().hasConverter(
                valueDescriptor.getClassType(), descriptor.getClassType()
        );

        // Scalar or object binding logic
        if (descriptor.isScalar() || descriptor.isClass() || descriptor.isEnum()
                || (isConvertible && !descriptor.isCollection())) {
            Object result = value.asObject();

            // If the value is scalar, convert it to the target type
            if (valueDescriptor.isScalar()) {
                Object converted = convert(result, descriptor);

                if (converted != null) {
                    converted = callback.onBinding(path, bindable, context, converted);
                }

                return (BindResult<T>) BindResult.of(converted);
            }

            return BindResult.empty();
        } else if (context.isDeepBinding()) {
            // If deep binding is enabled, delegate to root rootBinder
            return rootBinder.bind(path, bindable, accessor, callback);
        }

        callback.onUnbound(path, bindable, context);

        return BindResult.empty();
    }

    /**
     * Refreshes the bindable instance based on the type of the value.
     * <p>
     * If the target is an object and the value is not scalar, it overrides
     * the bindable's type to match the value's type.
     * </p>
     *
     * @param bindable the bindable instance
     * @param value    the value to bind
     * @return the refreshed bindable instance
     */
    protected Bindable<?> refreshBindable(Bindable<?> bindable, ObjectAccessor value) {
        TypeInformation valueDescriptor = TypeInformation.forClass(value.getDataType());
        TypeInformation descriptor      = bindable.getTypeInformation();

        // Override bindable type if necessary based on value descriptor
        if (descriptor.isObject() && !valueDescriptor.isScalar()) {
            bindable = Bindable.of(value.getDataType());
        }

        return bindable;
    }

    /**
     * Converts a value to the target type using the configured {@link Conversion}.
     * <p>
     * This method ensures that scalar values are converted to match the expected
     * {@link TypeInformation}. If no conversion is needed, the original value is returned.
     * </p>
     *
     * @param value          the raw value to convert
     * @param typeDescriptor the target type descriptor
     * @return the converted value, or the original value if no conversion was needed
     */
    protected Object convert(Object value, TypeInformation typeDescriptor) {
        Conversion conversion = context.getConversion();
        Class<?>   targetType = typeDescriptor.getRawType();

        // Convert if necessary, using the configured conversion
        if (conversion != null && targetType != Object.class) {
            value = conversion.convert(value, targetType);
        }

        return value;
    }
}
