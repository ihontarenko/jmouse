package org.jmouse.core.bind;

import org.jmouse.util.Factory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link DataSource} implementation for accessing properties of a bean instance.
 * <p>
 * This class allows retrieving properties dynamically from a wrapped bean instance.
 * It does not support indexed access since beans are typically key-value structures.
 * </p>
 */
public class JavaBeanInstanceDataSource extends AbstractDataSource {

    private final JavaBean<Object> bean;

    /**
     * Creates a {@link JavaBeanInstanceDataSource} for the given bean instance.
     *
     * @param source the bean instance to wrap
     * @throws IllegalArgumentException if the source is {@code null}
     */
    @SuppressWarnings({"unchecked"})
    public JavaBeanInstanceDataSource(Object source) {
        super(source);
        this.bean = (JavaBean<Object>) JavaBean.of(source.getClass());
    }

    /**
     * Retrieves a property from the bean instance as a {@link DataSource}.
     *
     * @param name the name of the property to retrieve
     * @return a {@link DataSource} wrapping the property value
     * @throws IllegalArgumentException if the property does not exist
     */
    @Override
    public DataSource get(String name) {
        Bean.Property<Object> property = bean.getProperty(name);

        if (property == null) {
            throw new IllegalArgumentException(
                    "Bean factory does not have property: '%s'.".formatted(name));
        }

        Factory<Object>  factory = this.getSupplier();
        Supplier<Object> value   = property.getValue(factory);

        return DataSource.of(value.get());
    }

    /**
     * Throws an exception since bean instances do not support indexed access.
     *
     * @param index the index to retrieve
     * @return never returns a value
     * @throws UnsupportedDataSourceException always, since indexed access is not supported
     */
    @Override
    public DataSource get(int index) {
        throw new UnsupportedDataSourceException(
                "Bean instance '%s' does not support indexed accessing"
                        .formatted(bean));
    }

    /**
     * Retrieves a collection of keys representing the entries in this {@link DataSource}.
     *
     * @return a collection of keys as strings
     */
    @Override
    public List<String> keys() {
        List<String> keys = new ArrayList<>();

        bean.getProperties().forEach(property -> keys.add(property.getName()));

        return keys;
    }

    /**
     * Retrieves a supplier for the bean instance.
     *
     * @return a {@link Supplier} providing values from the bean instance
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
