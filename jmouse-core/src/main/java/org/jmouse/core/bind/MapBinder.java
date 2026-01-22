package org.jmouse.core.bind;

import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.core.Priority;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.ClassMatchers.isSupertype;

/**
 * A binder for {@link Map} types, extending the {@link AbstractBinder} class.
 * <p>
 * This binder handles the process of binding {@link Map} entries from the {@link ObjectAccessor}
 * to the target map in the bindable instance.
 * </p>
 */
@Priority(MapBinder.PRIORITY)
public class MapBinder extends AbstractBinder {

    public static final int PRIORITY = -1000;

    /**
     * Constructs a {@link MapBinder} with the given binding context.
     *
     * @param context the binding context
     */
    public MapBinder(BindContext context) {
        super(context);
    }

    /**
     * Binds the values from the {@link ObjectAccessor} to the target {@link Map}.
     * <p>
     * This method checks if the target type is a {@link Map}. If so, it retrieves the
     * list of keys from the data accessor, then binds each entry by invoking {@link #bindValue}.
     * The bound entries are placed into the map.
     * </p>
     *
     * @param name     the structured name path of the binding target
     * @param bindable the bindable instance representing the target map
     * @param accessor the data accessor containing the values
     * @param callback the binding callback for customization
     * @param <T>      the type of the target object (map in this case)
     * @return a {@link BindResult} containing the bound map, or empty if binding failed
     */
    @Override
    public <T> BindResult<T> bind(
            PropertyPath name, TypedValue<T> bindable, ObjectAccessor accessor, BindCallback callback) {
        TypeInformation   typeDescriptor = bindable.getTypeInformation();
        Set<PropertyPath> keys           = accessor.navigate(name).nameSet();

        // Check if the target is a map and if the keys are present in the accessor
        if (typeDescriptor.isMap() && !keys.isEmpty()) {
            // Get the map from the bindable instance
            @SuppressWarnings({"unchecked"})
            Map<Object, Object> map     = (Map<Object, Object>) getMap(bindable);
            InferredType    mapType = bindable.getType().locate(Map.class);
            TypeInformation vi      = TypeInformation.forJavaType(mapType.getLast());
            TypeInformation     ki      = TypeInformation.forJavaType(mapType.getFirst());

            // Iterate through each key and bind the corresponding value
            for (PropertyPath key : keys) {
                String               keyName       = key.path();
                TypedValue<Object>   valueBindable = getBindableEntry(vi, map, keyName);
                PropertyPath.Entries entries       = key.entries();

                // If the key consists of multiple elements, we explicitly convert it into an indexed format.
                // This ensures that the entire key is treated as a single entity enclosed in brackets,
                // preventing it from being split into separate path segments.
                if (entries.size() != 1) {
                    key = PropertyPath.forPath("[" + key.path() + "]");
                }

                // unwrap braces [0] -> 0
                if (entries.type(0).isNumeric()) {
                    keyName = entries.first().toString();
                }

                // convert key from String to actual type of Map<K, ?> key
                Object entryKey = convert(keyName, ki);

                // Bind the value for each map entry and put it into the map
                bindValue(name.append(key), valueBindable, accessor, callback)
                        .ifPresent(entry -> map.put(entryKey, entry));
            }

            return (BindResult<T>) BindResult.of(map);
        }

        return BindResult.empty();
    }

    /**
     * Retrieves the bindable entry for a specific map key.
     * <p>
     * If the map already contains the key, the corresponding entry will be used.
     * Otherwise, a new bindable instance will be created for the entry.
     * </p>
     */
    protected TypedValue<Object> getBindableEntry(TypeInformation descriptor, Map<Object, Object> map, String name) {
        TypedValue<Object> entry = TypedValue.of(descriptor.getType());

        // If the map already contains the key, use the existing value
        if (map.containsKey(name)) {
            entry = TypedValue.ofInstance(map.get(name));
        }

        return entry;
    }


    /**
     * Retrieves the map from the bindable instance.
     * <p>
     * If the bindable provides a supplier for a map, it is used to retrieve the map.
     * Otherwise, a new map is created using the default supplier.
     * </p>
     *
     * @param bindable the bindable instance representing the target map
     * @return the map to bind values to
     */
    @SuppressWarnings({"unchecked"})
    protected Map<Object, ?> getMap(TypedValue<?> bindable) {
        TypeInformation typeDescriptor = bindable.getTypeInformation();
        Supplier<?>     supplier       = bindable.getValue();

        // Check if the bindable type is a map and retrieve it if available
        if (typeDescriptor.isMap() && supplier != null) {
            Object mapObject = supplier.get();
            if (mapObject instanceof Map<?, ?>) {

                if (mapObject.getClass().getName().contains("Immutable")) {
                    throw new BindException("Bindable map is present but it is immutable");
                }

                return (Map<Object, ?>) supplier.get();
            }
        }

        return getMapSupplier(bindable).get();
    }

    /**
     * Provides a default supplier that creates an empty {@link LinkedHashMap}.
     *
     * @param bindable the bindable instance
     * @return a supplier that provides a new {@link LinkedHashMap}
     */
    protected Supplier<Map<Object, ?>> getMapSupplier(TypedValue<?> bindable) {
        return LinkedHashMap::new;
    }

    /**
     * Checks if the {@link MapBinder} supports binding the given {@link TypedValue} instance.
     * <p>
     * This method checks if the target type is a subtype of {@link Map}.
     * </p>
     *
     * @param bindable the bindable instance representing the target type
     * @param <T>      the type of the target object
     * @return true if the binder supports the target type, false otherwise
     */
    @Override
    public <T> boolean supports(TypedValue<T> bindable) {
        return isSupertype(Map.class).matches(bindable.getType().getRawType());
    }
}
