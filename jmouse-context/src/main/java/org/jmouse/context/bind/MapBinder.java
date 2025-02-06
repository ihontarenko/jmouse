package org.jmouse.context.bind;

import org.jmouse.core.reflection.TypeDescriptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.ClassMatchers.isSupertype;

/**
 * A binder for {@link Map} types, extending the {@link AbstractBinder} class.
 * <p>
 * This binder handles the process of binding {@link Map} entries from the {@link DataSource}
 * to the target map in the bindable instance.
 * </p>
 */
public class MapBinder extends AbstractBinder {

    /**
     * Constructs a {@link MapBinder} with the given binding context.
     *
     * @param context the binding context
     */
    public MapBinder(BindContext context) {
        super(context);
    }

    /**
     * Binds the values from the {@link DataSource} to the target {@link Map}.
     * <p>
     * This method checks if the target type is a {@link Map}. If so, it retrieves the
     * list of keys from the data source, then binds each entry by invoking {@link #bindValue}.
     * The bound entries are placed into the map.
     * </p>
     *
     * @param name     the structured name path of the binding target
     * @param bindable the bindable instance representing the target map
     * @param source   the data source containing the values
     * @param callback the binding callback for customization
     * @param <T>      the type of the target object (map in this case)
     * @return a {@link BindResult} containing the bound map, or empty if binding failed
     */
    @Override
    public <T> BindResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source, BindCallback callback) {
        TypeDescriptor typeDescriptor = bindable.getTypeDescriptor();
        List<NamePath> keys           = source.get(name).names();

        // Check if the target is a map and if the keys are present in the source
        if (typeDescriptor.isMap() && !keys.isEmpty()) {
            // Get the map from the bindable instance
            @SuppressWarnings({"unchecked"}) Map<String, Object> map = (Map<String, Object>) getMap(bindable);

            // Iterate through each key and bind the corresponding value
            for (NamePath key : keys) {
                String           keyName       = key.path();
                Bindable<Object> entryBindable = getBindableEntry(bindable, map, keyName);
                NamePath.Entries entries       = key.entries();

                // If the key consists of multiple elements, we explicitly convert it into an indexed format.
                // This ensures that the entire key is treated as a single entity enclosed in brackets,
                // preventing it from being split into separate path segments.
                if (entries.size() != 1) {
                    key = NamePath.of("[" + key.path() + "]");
                }

                if (entries.type(0).isNumeric()) {
                    keyName = entries.first().toString();
                }

                // Bind the value for each map entry and put it into the map
                String entryKey = keyName;
                bindValue(name.append(key), entryBindable, source, callback)
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
     *
     * @param bindable the bindable instance representing the target map
     * @param map      the map to bind values to
     * @param name     the map key to bind
     * @return a {@link Bindable} instance for the map entry
     */
    protected Bindable<Object> getBindableEntry(Bindable<?> bindable, Map<String, Object> map, String name) {
        Bindable<Object> entry = Bindable.of(bindable.getType().getLast());

        // If the map already contains the key, use the existing value
        if (map.containsKey(name)) {
            entry = Bindable.ofInstance(map.get(name));
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
    protected Map<String, ?> getMap(Bindable<?> bindable) {
        TypeDescriptor typeDescriptor = bindable.getTypeDescriptor();
        Supplier<?>    supplier       = bindable.getValue();

        // Check if the bindable type is a map and retrieve it if available
        if (typeDescriptor.isMap() && supplier != null) {
            if (supplier.get() instanceof Map<?, ?>) {
                return (Map<String, ?>) supplier.get();
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
    protected Supplier<Map<String, ?>> getMapSupplier(Bindable<?> bindable) {
        return LinkedHashMap::new;
    }

    /**
     * Checks if the {@link MapBinder} supports binding the given {@link Bindable} instance.
     * <p>
     * This method checks if the target type is a subtype of {@link Map}.
     * </p>
     *
     * @param bindable the bindable instance representing the target type
     * @param <T>      the type of the target object
     * @return true if the binder supports the target type, false otherwise
     */
    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return isSupertype(Map.class).matches(bindable.getType().getRawType());
    }
}
