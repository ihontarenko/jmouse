package org.jmouse.core.access;

import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;

import java.util.HashSet;
import java.util.Set;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link ObjectAccessor} implementation for accessing properties of a structured instance.
 * <p>
 * This class allows retrieving properties dynamically from a wrapped structured instance.
 * It does not support indexed access since beans are typically key-value structures.
 * </p>
 */
abstract public class AbstractBeanAccessor extends AbstractAccessor {

    private final ObjectDescriptor<Object> descriptor;

    /**
     * Creates a {@link AbstractBeanAccessor} for the given structured instance.
     *
     * @param source the structured instance to wrap
     * @throws IllegalArgumentException if the source is {@code null}
     */
    public AbstractBeanAccessor(Object source) {
        super(source);
        this.descriptor = getDescriptor(source.getClass());
    }

    /**
     * Sets a property value by name.
     *
     * <p>⚠️ One lookup, and the absent case is the {@code null} it hands back. Fetching the descriptor
     * and then asking a second time whether it exists is a question already answered — and it used to be
     * asked in that order, so the map was read twice to decide whether the first read had found
     * anything.</p>
     *
     * @param name  the property name
     * @param value the value to set
     * @throws BeanPropertyNotFound when this type has no such property
     */
    @Override
    public void set(String name, Object value) {
        PropertyDescriptor<Object> property = descriptor.getProperty(name);

        if (property == null) {
            throw new BeanPropertyNotFound(
                    "Accessor '%s' does not have property: '%s'.".formatted(descriptor, name));
        }

        property.getAccessor().writeValue(unwrap(), value);
    }

    /**
     * Sets a property value by index.
     *
     * <p>The default implementation throws an {@link UnsupportedOperationException},
     * indicating that indexed access is not supported unless overridden by an implementation.</p>
     *
     * @param index the property index
     * @param value the value to set
     * @throws UnsupportedOperationException if indexed access is not supported
     */
    @Override
    public void set(int index, Object value) {
        throw new UnsupportedOperationException(
                "Accessor '%s' does not support indexed accessing"
                        .formatted(descriptor));
    }

    /**
     * Retrieves a property from the structured instance as a {@link ObjectAccessor}.
     *
     * <p>⚠️ The absence check belongs to {@link #read(String)} and is not repeated here. This method
     * used to ask whether the property existed and then call {@code read}, which asked again and then
     * looked the descriptor up — three probes of one map to answer one question, carrying two copies of
     * the same message.</p>
     *
     * @param name the name of the property to retrieve
     * @return a {@link ObjectAccessor} wrapping the property value
     * @throws IllegalArgumentException if the property does not exist
     */
    @Override
    public ObjectAccessor get(String name) {
        return wrap(read(name));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads straight through the property's accessor, so nothing is allocated to carry a value the
     * caller is about to take out again.</p>
     *
     * <h2>⚠️ One lookup, because this is the mapping engine's hot path</h2>
     *
     * <p>Asking {@code hasProperty} and then {@code getProperty} reads the same map twice for every
     * property of every mapped object. Under a sampling profile that pair was the <strong>largest single
     * cost</strong> of a flat bean-to-bean mapping — more than the generated getter and setter that do
     * the actual work, put together. A {@code get} answers both questions at once: the descriptor, or
     * {@code null}.</p>
     *
     * <p>The two forms differ only for a name mapped to a {@code null} descriptor, which cannot occur:
     * {@code addProperty} is the only writer of that map and it dereferences the descriptor to key it.
     * Where it somehow did occur, the old form reached {@code getAccessor()} on {@code null} and threw a
     * {@link NullPointerException} naming nothing; this one says which property, on which accessor.</p>
     */
    @Override
    public Object read(String name) {
        PropertyDescriptor<Object> property = descriptor.getProperty(name);

        if (property == null) {
            throw new IllegalArgumentException(
                    "Accessor '%s' does not have property: '%s'.".formatted(descriptor, name));
        }

        return property.getAccessor().readValue(source);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Answered from the descriptor this accessor already holds, so a caller never has to learn the
     * answer from a thrown exception.</p>
     */
    @Override
    public boolean hasProperty(String name) {
        return descriptor.hasProperty(name);
    }

    /**
     * {@inheritDoc}
     *
     * <p>⚠️ The whole point of the override: the descriptor is fetched once and its absence <em>is</em>
     * the answer to "does this property exist". The inherited default asks
     * {@link #hasProperty(String)} and then {@link #read(String)}, which reads the same map a second
     * time — and this is the call the mapping engine makes for every property of every object it
     * copies.</p>
     */
    @Override
    public Object readIfPresent(String name) {
        PropertyDescriptor<Object> property = descriptor.getProperty(name);

        return property == null ? null : property.getAccessor().readValue(source);
    }

    /**
     * Throws an exception since structured instances do not support indexed access.
     *
     * @param index the index to retrieve
     * @return never returns a value
     * @throws UnsupportedOperationException always, since indexed access is not supported
     */
    @Override
    public ObjectAccessor get(int index) {
        throw new UnsupportedOperationException(
                "Accessor '%s' does not support indexed accessing"
                        .formatted(descriptor));
    }

    /**
     * Retrieves a collection of keys representing the entries in this {@link ObjectAccessor}.
     *
     * @return a collection of keys as strings
     */
    @Override
    public Set<Object> keySet() {
        Set<Object> keys = new HashSet<>();

        descriptor.getProperties().forEach((name, property) -> keys.add(name));

        return keys;
    }

    /**
     * Returns a string representation of this data source.
     *
     * @return a formatted string with the class name and the type of the stored source
     */
    @Override
    public String toString() {
        return "%s: %s".formatted(getShortName(this), descriptor);
    }

    abstract protected ObjectDescriptor<Object> getDescriptor(Class<?> type);

}
