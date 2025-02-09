package org.jmouse.core.bind;

import org.jmouse.core.reflection.TypeDescriptor;
import org.jmouse.util.Priority;

/**
 * A binder responsible for binding scalar values such as {@link String}, {@link Number}, {@link Boolean}, and {@link Character}.
 * <p>
 * This binder ensures that only scalar types are processed. It throws an exception if a non-scalar type is passed.
 * </p>
 */
@Priority(Integer.MAX_VALUE / 10)
public class ScalarValueBinder extends AbstractBinder {

    /**
     * Constructs a new {@code ScalarValueBinder} with the given binding context.
     *
     * @param context the binding context used for resolving scalar values
     */
    public ScalarValueBinder(BindContext context) {
        super(context);
    }

    /**
     * Binds a scalar value from the {@link DataSource} to the given {@link Bindable} type.
     * <p>
     * This method ensures that only scalar types are handled. If a non-scalar type is provided,
     * an {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param name     the name path representing the target property
     * @param bindable the bindable type to map the value to
     * @param source   the data source containing the value
     * @param callback the binding callback for customization
     * @param <T>      the target type
     * @return a {@link BindResult} containing the bound scalar value
     * @throws IllegalArgumentException if a non-scalar type is provided
     */
    @Override
    public <T> BindResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source, BindCallback callback) {
        TypeDescriptor descriptor  = bindable.getTypeDescriptor();
        boolean        isCompliant = descriptor.isScalar() || descriptor.isEnum() || descriptor.isClass();

        if (!isCompliant) {
            throw new IllegalArgumentException("Scalar binder only handles scalar, class, enum types. But got " + descriptor);
        }

        return bindValue(name, bindable, source, callback);
    }

    /**
     * Determines if this binder supports the given {@link Bindable} type.
     * <p>
     * This binder supports only scalar types such as {@link String}, {@link Number}, {@link Boolean}, and {@link Character}.
     * </p>
     *
     * @param bindable the bindable type to check
     * @param <T>      the target type
     * @return {@code true} if the type is scalar, otherwise {@code false}
     */
    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        TypeDescriptor descriptor = bindable.getTypeDescriptor();

        return descriptor.isScalar() || descriptor.isEnum() || descriptor.isClass();
    }
}
