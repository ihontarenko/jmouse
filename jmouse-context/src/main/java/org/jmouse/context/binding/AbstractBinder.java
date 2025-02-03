package org.jmouse.context.binding;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.reflection.TypeDescriptor;

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

    /** The binding context used for managing the binding process. */
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
     * Attempts to bind a scalar or object value from the {@link DataSource}.
     * <p>
     * If the requested name is present in the source, this method attempts to
     * retrieve and convert the value based on the target {@link TypeDescriptor}.
     * If deep binding is enabled, it delegates to the root binder for nested binding.
     * </p>
     *
     * @param name     the structured name path of the binding target
     * @param bindable the bindable instance representing the target type
     * @param source   the data source containing the value
     * @param <T>      the type of the target object
     * @return a {@link BindingResult} containing the bound value, or empty if binding failed
     */
    @Override
    public <T> BindingResult<T> bindValue(NamePath name, Bindable<T> bindable, DataSource source) {
        DataSource value = source.get(name);

        // If the value is null, return an empty binding result
        if (value.isNull()) {
            return BindingResult.empty();
        }

        bindable = (Bindable<T>) refreshBindable(bindable, value);

        TypeDescriptor descriptor      = bindable.getTypeDescriptor();
        TypeDescriptor valueDescriptor = TypeDescriptor.forClass(value.getType());
        ObjectBinder   binder          = context.getRootBinder();

        // Scalar or object binding logic
        if (descriptor.isScalar() || descriptor.isObject()) {
            Object result = value.getRaw();

            // If the value is scalar, convert it to the target type
            if (valueDescriptor.isScalar()) {
                result = convert(result, descriptor);
            }

            return BindingResult.of((T) result);
        } else if (context.isDeepBinding()) {
            // If deep binding is enabled, delegate to root binder
            return binder.bind(name, bindable, source);
        }

        return BindingResult.empty();
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
    protected Bindable<?> refreshBindable(Bindable<?> bindable, DataSource value) {
        TypeDescriptor valueDescriptor = TypeDescriptor.forClass(value.getType());
        TypeDescriptor descriptor = bindable.getTypeDescriptor();

        // Override bindable type if necessary based on value descriptor
        if (descriptor.isObject() && !valueDescriptor.isScalar()) {
            bindable = Bindable.of(value.getType());
        }

        return bindable;
    }

    /**
     * Converts a value to the target type using the configured {@link Conversion}.
     * <p>
     * This method ensures that scalar values are converted to match the expected
     * {@link TypeDescriptor}. If no conversion is needed, the original value is returned.
     * </p>
     *
     * @param value         the raw value to convert
     * @param typeDescriptor the target type descriptor
     * @return the converted value, or the original value if no conversion was needed
     */
    protected Object convert(Object value, TypeDescriptor typeDescriptor) {
        Conversion conversion = context.getConversion();
        Class<?>   targetType = typeDescriptor.getRawType();

        // Convert if necessary, using the configured conversion
        if (conversion != null && targetType != Object.class) {
            value = conversion.convert(value, targetType);
        }

        return value;
    }
}
