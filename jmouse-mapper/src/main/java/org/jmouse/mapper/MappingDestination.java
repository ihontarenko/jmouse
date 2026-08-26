package org.jmouse.mapper;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.reflection.InferredType;

/**
 * The slot a mapped value is on its way into. 📍
 *
 * <p>Every variant carries the object being populated and the {@link PropertyPath} reached so far,
 * plus whatever identifies the slot within it. Plugins receive this through
 * {@link org.jmouse.mapper.plugin.MappingValue} and use it to tell "the third element of a
 * list" from "a bean property named {@code price}" without re-deriving either from the path.</p>
 */
public sealed interface MappingDestination
        permits MappingDestination.RootTarget,
                MappingDestination.BeanProperty,
                MappingDestination.RecordComponent,
                MappingDestination.MapEntry,
                MappingDestination.CollectionElement {

    /**
     * The object being populated.
     *
     * @return target object, or the root object when the slot is the root itself
     */
    Object target();

    /**
     * Path reached so far within the mapping.
     *
     * @return current property path
     */
    PropertyPath path();

    /**
     * The root of the mapping, used when no finer slot applies.
     *
     * @param target root target object
     * @param path current property path
     */
    record RootTarget(Object target, PropertyPath path) implements MappingDestination {
    }

    /**
     * A writable property of a JavaBean.
     *
     * @param target bean being populated
     * @param path current property path
     * @param propertyDescriptor descriptor of the property being written
     */
    record BeanProperty(Object target, PropertyPath path, PropertyDescriptor<?> propertyDescriptor)
            implements MappingDestination {
    }

    /**
     * A component of a record, collected before the record is constructed.
     *
     * <p>{@link #target()} is {@code null} here: a record does not exist until every component is
     * known, so there is no instance to point at yet.</p>
     *
     * @param target always {@code null}
     * @param path current property path
     * @param componentDescriptor descriptor of the component being filled
     */
    record RecordComponent(Object target, PropertyPath path, PropertyDescriptor<?> componentDescriptor)
            implements MappingDestination {
    }

    /**
     * One entry of a map.
     *
     * @param target map being populated
     * @param path current property path
     * @param key key the value is stored under
     * @param valueType declared type of the map's values
     */
    record MapEntry(Object target, PropertyPath path, Object key, InferredType valueType)
            implements MappingDestination {
    }

    /**
     * One element of a collection or array.
     *
     * @param target collection or array being populated
     * @param path current property path
     * @param index position of the element
     * @param elementType declared type of the elements
     */
    record CollectionElement(Object target, PropertyPath path, int index, InferredType elementType)
            implements MappingDestination {
    }
}
