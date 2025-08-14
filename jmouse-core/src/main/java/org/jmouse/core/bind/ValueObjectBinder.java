package org.jmouse.core.bind;

import org.jmouse.core.bind.descriptor.AnnotationDescriptor;
import org.jmouse.core.bind.descriptor.MethodDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.core.Factory;
import org.jmouse.core.Priority;
import org.jmouse.core.Setter;

/**
 * Binder for Java records, enabling their instantiation and property binding from a data source.
 * <p>
 * This binder extracts record components, binds them using the provided {@link ObjectAccessor},
 * and creates a new immutable record instance via a {@link ValueObject}.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BindContext context = new BindContext();
 * ValueObjectBinder binder = new ValueObjectBinder(context);
 *
 * PropertyPath namePath = PropertyPath.of("person");
 * Bindable<Person> bindable = Bindable.of(Person.class);
 * DataSource source = DataSource.of(Map.of("name", "John"));
 *
 * BindingResult<Person> result = binder.bind(namePath, bindable, source);
 * Person person = result.orElse(null); // Person [name=John]
 * }</pre>
 */
@Priority(ValueObjectBinder.PRIORITY)
public class ValueObjectBinder extends AbstractBinder {

    public static final int PRIORITY = -500;

    /**
     * Constructs a binder for handling Java records.
     *
     * @param context the binding context
     */
    public ValueObjectBinder(BindContext context) {
        super(context);
    }

    /**
     * Binds a Java record by extracting its properties, resolving values from the {@link ObjectAccessor},
     * and creating a new immutable record instance.
     *
     * @param name     the hierarchical name path of the object being bound
     * @param bindable the target bindable describing the type to bind
     * @param accessor   the data source providing values for binding
     * @param callback a binding callback for customization
     * @param <T>      the type of the bound object
     * @return a {@link BindResult} containing the bound record instance, or an empty result if binding was unsuccessful
     */
    @Override
    public <T> BindResult<T> bind(PropertyPath name, Bindable<T> bindable, ObjectAccessor accessor, BindCallback callback) {
        TypeInformation sourceDescriptor = TypeInformation.forClass(accessor.navigate(name).getDataType());
        TypeInformation targetDescriptor = bindable.getTypeInformation();

        // Ensure the target is a record and the source is a compatible type (map or JavaBean)
        if (targetDescriptor.isRecord() && (sourceDescriptor.isMap() || sourceDescriptor.isBean())) {
            return bindValueObject(name, bindable, accessor, callback);
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
    protected <T> BindResult<T> bindValueObject(PropertyPath name, Bindable<T> bindable, ObjectAccessor source, BindCallback callback) {
        Class<Record> rawType = bindable.getType().getRawType();

        // Create a ValueObject representation of the record
        ValueObject<?>     vo      = ValueObject.of((Class<? extends Record>) rawType);
        ValueObject.Values values  = vo.getRecordValues();
        Factory<?>         factory = vo.getInstance(values);

        // Iterate over record properties and bind values from the data source
        for (PropertyDescriptor<?> property : vo.getProperties()) {
            String      propertyName = property.getName();
            var         setter       = Setter.ofMap(propertyName);
            Bindable<?> component    = Bindable.of(property.getType().getClassType());

            @SuppressWarnings("unchecked")
            BindResult<T> result = (BindResult<T>) bindValue(name.append(propertyName), component, source, callback);

            // Set the bound value if available
            result.ifPresent(bound -> setter.set(values, bound));
            result.ifAbsent(() -> setter.set(values, getDefaultIfPresent(property)));
        }

        // Invoke the callback after binding
        callback.onBound(name, bindable, context, null);

        @SuppressWarnings({"unchecked"})
        T instance = (T) factory.create();

        // Instantiate and return the record
        return BindResult.of(instance);
    }

    protected Object getDefaultIfPresent(PropertyDescriptor<?> property) {
        Object           defaultValue     = null;
        MethodDescriptor methodDescriptor = property.getGetterMethod();

        if (methodDescriptor != null && methodDescriptor.getAnnotations() != null) {
            AnnotationDescriptor annotationDescriptor = methodDescriptor.getAnnotations().getFirst();
            if (annotationDescriptor.unwrap() instanceof BindDefault bindDefault) {
                defaultValue = bindDefault.value();
            }
        }

        if (defaultValue != null) {
            Class<?> targetType = property.getType().getClassType();
            defaultValue = context.getConversion().convert(defaultValue, targetType);
        }

        return defaultValue;
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
        return bindable.getType().isRecord();
    }

}
