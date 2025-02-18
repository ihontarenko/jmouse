package org.jmouse.core.bind;

import org.jmouse.util.Factory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link PropertyValuesAccessor} implementation for accessing properties of a structured instance.
 * <p>
 * This class allows retrieving properties dynamically from a wrapped structured instance.
 * It does not support indexed access since beans are typically key-value structures.
 * </p>
 */
public class JavaBeanPropertyValuesAccessor extends AbstractPropertyValuesAccessor {

    private final JavaBean<Object> bean;

    /**
     * Creates a {@link JavaBeanPropertyValuesAccessor} for the given structured instance.
     *
     * @param source the structured instance to wrap
     * @throws IllegalArgumentException if the source is {@code null}
     */
    @SuppressWarnings({"unchecked"})
    public JavaBeanPropertyValuesAccessor(Object source) {
        super(source);
        this.bean = (JavaBean<Object>) JavaBean.of(source.getClass());
    }

    /**
     * Retrieves a property from the structured instance as a {@link PropertyValuesAccessor}.
     *
     * @param name the name of the property to retrieve
     * @return a {@link PropertyValuesAccessor} wrapping the property value
     * @throws IllegalArgumentException if the property does not exist
     */
    @Override
    public PropertyValuesAccessor get(String name) {
        Bean.Property<Object> property = bean.getProperty(name);

        if (property == null) {
            throw new IllegalArgumentException(
                    "Bean factory does not have property: '%s'.".formatted(name));
        }

        Factory<Object>  factory = this.getSupplier();
        Supplier<Object> value   = property.getValue(factory);

        return PropertyValuesAccessor.wrap(value.get());
    }

    /**
     * Throws an exception since structured instances do not support indexed access.
     *
     * @param index the index to retrieve
     * @return never returns a value
     * @throws UnsupportedDataSourceException always, since indexed access is not supported
     */
    @Override
    public PropertyValuesAccessor get(int index) {
        throw new UnsupportedDataSourceException(
                "Bean instance '%s' does not support indexed accessing"
                        .formatted(bean));
    }

    /**
     * Retrieves a collection of keys representing the entries in this {@link PropertyValuesAccessor}.
     *
     * @return a collection of keys as strings
     */
    @Override
    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();

        bean.getProperties().forEach(property -> keys.add(property.getName()));

        return keys;
    }

    /**
     * Retrieves a supplier for the structured instance.
     *
     * @return a {@link Supplier} providing values from the structured instance
     */
    private Factory<Object> getSupplier() {
        return bean.getFactory(Bindable.ofInstance(source));
    }

    /**
     * Returns a string representation of this data source.
     *
     * @return a formatted string with the class name and the type of the stored source
     */
    @Override
    public String toString() {
        return "%s: %s".formatted(getShortName(this), bean);
    }
}
