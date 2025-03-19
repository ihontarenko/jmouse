package org.jmouse.core.bind.descriptor.structured.map;

import org.jmouse.core.bind.descriptor.AbstractIntrospector;
import org.jmouse.core.bind.descriptor.ClassTypeDescriptor;
import org.jmouse.core.bind.descriptor.ClassTypeIntrospector;
import org.jmouse.core.bind.descriptor.structured.ObjectData;

import java.util.Map;

import static org.jmouse.core.reflection.JavaType.forClass;

/**
 * An introspector for analyzing {@link Map} structures.
 * <p>
 * This class inspects the key-value pairs of a map, identifying their types and
 * dynamically creating metadata for individual entries.
 * </p>
 *
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 *
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public class MapIntrospector<K, V>
        extends AbstractIntrospector<ObjectData<Map<K, V>>, MapIntrospector<K, V>, Map<K, V>, MapDescriptor<K, V>> {

    /**
     * Constructs a {@code MapIntrospector} for the given map instance.
     *
     * @param target the map to introspect
     */
    public MapIntrospector(Map<K, V> target) {
        super(target);
    }

    /**
     * Assigns a name to this introspector based on the type of the inspected map.
     *
     * @return this introspector instance for method chaining
     */
    @Override
    public MapIntrospector<K, V> name() {
        type().name(container.getType().getName());
        return self();
    }

    /**
     * Performs descriptor on the map, analyzing its structure and properties.
     *
     * @return this introspector instance for method chaining
     */
    @Override
    public MapIntrospector<K, V> introspect() {
        return name().properties();
    }

    /**
     * Determines and assigns the type descriptor for the map being introspected.
     *
     * @return this introspector instance for method chaining
     */
    public MapIntrospector<K, V> type() {
        container.setType(new ClassTypeIntrospector(forClass(container.getTarget().getClass()))
                                  .name()
                                  .toDescriptor());
        return self();
    }

    /**
     * Analyzes the key-value pairs of the map and registers them as properties.
     *
     * @return this introspector instance for method chaining
     */
    public MapIntrospector<K, V> properties() {
        Map<K, V>           kvMap  = container.getTarget();
        MapDescriptor<K, V> parent = toDescriptor();

        for (Map.Entry<K, V> kvEntry : kvMap.entrySet()) {
            MapPropertyIntrospector<K, V> introspector = new MapPropertyIntrospector<>(kvMap);
            K                             key          = kvEntry.getKey();
            V                             value        = kvEntry.getValue();

            // Determine key and value types
            ClassTypeDescriptor keyType   = new ClassTypeIntrospector(forClass(key.getClass()))
                    .name()
                    .toDescriptor();
            ClassTypeDescriptor valueType = new ClassTypeIntrospector(forClass(value.getClass()))
                    .name()
                    .toDescriptor();

            // Assign key, value types, and name
            introspector.keyType(keyType).valueType(valueType);
            introspector.getter(key).setter(key).name(key.toString());

            container.addProperty(introspector.owner(parent).toDescriptor());
        }

        return self();
    }

    /**
     * Generates a {@link MapDescriptor} representing the introspected map.
     *
     * @return a new {@link MapDescriptor} instance
     */
    @Override
    public MapDescriptor<K, V> toDescriptor() {
        return getCachedDescriptor(() -> new MapDescriptor<>(this, container));
    }

    /**
     * Creates a new {@link ObjectData} container for the specified map.
     *
     * @param target the map instance
     * @return an {@link ObjectData} instance wrapping the map
     */
    @Override
    public ObjectData<Map<K, V>> getContainerFor(Map<K, V> target) {
        return new ObjectData<>(target);
    }
}
