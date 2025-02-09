package org.jmouse.core.bind;

import org.jmouse.core.reflection.TypeDescriptor;
import org.jmouse.util.Priority;
import org.jmouse.util.Setter;

import java.util.function.Supplier;

/**
 * Binder for Java records, enabling their instantiation and property binding from a data source.
 * <p>
 * This binder extracts record components, binds them using the provided {@link DataSource},
 * and creates a new immutable record instance via a {@link ValueObject}.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BindContext context = new BindContext();
 * ValueObjectBinder binder = new ValueObjectBinder(context);
 *
 * NamePath namePath = NamePath.of("person");
 * Bindable<Person> bindable = Bindable.of(Person.class);
 * DataSource source = DataSource.of(Map.of("name", "John"));
 *
 * BindingResult<Person> result = binder.bind(namePath, bindable, source);
 * Person person = result.orElse(null); // Person [name=John]
 * }</pre>
 */
@Priority(Integer.MIN_VALUE / 2)
public class ValueObjectBinder extends AbstractBinder {

    /**
     * Constructs a binder for handling Java records.
     *
     * @param context the binding context
     */
    public ValueObjectBinder(BindContext context) {
        super(context);
    }

    /**
     * Binds a Java record by extracting its properties, resolving values from the {@link DataSource},
     * and creating a new immutable record instance.
     *
     * @param name     the hierarchical name path of the object being bound
     * @param bindable the target bindable describing the type to bind
     * @param source   the data source providing values for binding
     * @param callback a binding callback for customization
     * @param <T>      the type of the bound object
     * @return a {@link BindResult} containing the bound record instance, or an empty result if binding was unsuccessful
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public <T> BindResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source, BindCallback callback) {
        TypeDescriptor sourceDescriptor = TypeDescriptor.forClass(source.get(name).getType());
        TypeDescriptor targetDescriptor = bindable.getTypeDescriptor();

        // Ensure the target is a record and the source is a compatible type (map or JavaBean)
        if (targetDescriptor.isRecord() && (sourceDescriptor.isMap() || sourceDescriptor.isBean())) {
            return bindValueObject(name, bindable, source, callback);
        }

        return BindResult.empty();
    }

    /**
     * Performs the binding of a Java record by mapping its components from the data source.
     *
     * @param name     the name path of the object being bound
     * @param bindable the bindable target describing the record type
     * @param source   the source of values for binding
     * @param callback the binding callback for customization
     * @param <T>      the type of the bound object
     * @return a {@link BindResult} containing the bound record instance if successful
     */
    protected <T> BindResult<T> bindValueObject(NamePath name, Bindable<T> bindable, DataSource source, BindCallback callback) {
        Class<?> rawType = bindable.getType().getRawType();

        // Create a ValueObject representation of the record
        @SuppressWarnings("unchecked")
        ValueObject<?>     vo      = ValueObject.of((Class<? extends Record>) rawType);
        ValueObject.Values values  = vo.getRecordValues();
        Supplier<?>        factory = vo.getInstance(values);

        // Iterate over record properties and bind values from the data source
        for (Bean.Property<?> property : vo.getProperties()) {
            String      propertyName = property.getName();
            var         setter       = Setter.ofMap(propertyName);
            Bindable<?> component    = Bindable.of(property.getType());

            @SuppressWarnings("unchecked")
            BindResult<T> result = (BindResult<T>) bindValue(name.append(propertyName), component, source, callback);

            // Set the bound value if available
            result.ifPresent(bound -> setter.set(values, bound));
        }

        // Invoke the callback after binding
        callback.onBound(name, bindable, context, null);

        // Instantiate and return the record
        return BindResult.of((T) factory.get());
    }

    /**
     * Determines whether this binder supports the given type.
     * This binder only supports Java records.
     *
     * @param bindable the bindable type descriptor
     * @param <T>      the type of the object
     * @return {@code true} if the type is a record, {@code false} otherwise
     */
    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return bindable.getTypeDescriptor().isRecord();
    }

}
