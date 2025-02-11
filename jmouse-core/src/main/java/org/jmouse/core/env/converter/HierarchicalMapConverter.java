package org.jmouse.core.env.converter;

import org.jmouse.core.bind.NamePath;
import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toSet;

/**
 * A converter that transforms a flat key-value {@link Map} into a hierarchical structure.
 * It parses dotted and indexed keys (e.g., {@code "server.hosts[0].name"}) into nested maps and lists.
 */
public class HierarchicalMapConverter implements GenericConverter<Map<String, Object>, Map<String, Object>> {

    /**
     * Expands a list to ensure it has at least the specified index.
     * Missing indexes are filled with newly created maps.
     *
     * @param collection the list to expand
     * @param index      the target index
     * @param factory    the supplier that provides a new map instance
     */
    public static void expand(List<Object> collection, int index, Supplier<Map<String, Object>> factory) {
        while (collection.size() <= index) {
            collection.add(factory.get());
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Map<String, Object> convert(Map<String, Object> source, Class<Map<String, Object>> sourceType, Class<Map<String, Object>> targetType) {
        Map<String, Object> hierarchical = new HashMap<>();
        Set<NamePath>       keys         = source.keySet().stream().map(NamePath::new).collect(toSet());

        for (NamePath path : keys) {
            NamePath.Entries    entries    = path.entries();
            Object              value      = source.get(path.path());
            Map<String, Object> map        = hierarchical;
            List<Object>        collection = Collections.EMPTY_LIST;

            for (int index = 0; index < entries.size(); index++) {
                String        key  = entries.get(index).toString();
                NamePath.Type type = entries.type(index);

                // Skip empty keys
                if (type.isEmpty()) {
                    continue;
                }

                // If this is the last entry, assign the value
                if (entries.isLast(index)) {
                    if (type.isNumeric()) {
                        collection.set(ensureCollection(collection, key), value);
                    } else {
                        map.put(key, value);
                    }
                    continue;
                }

                // Navigate deeper into the structure
                if (type.isNumeric() && index > 0) {
                    map = (Map<String, Object>) collection.get(ensureCollection(collection, key));
                } else {
                    NamePath.Type nextType = entries.type(index + 1);
                    if (nextType.isNumeric()) {
                        collection = (List<Object>) map.computeIfAbsent(key, listFactory());
                    } else {
                        try {
                            map = (Map<String, Object>) map.computeIfAbsent(key, mapFactory());
                        } catch (ClassCastException e) {
                            // for duplicate properties such as
                            // java.property.name.key=1
                            // java.property.name=2
                        }
                    }
                }
            }
        }

        return hierarchical;
    }

    /**
     * Provides factory methods for creating new instances of lists and maps.
     * These methods are useful for dynamically constructing hierarchical data structures.
     */
    private Function<String, List<Object>> listFactory() {
        return k -> new ArrayList<>();
    }

    /**
     * Provides a factory method for creating new instances of maps.
     * This is typically used when constructing nested key-value structures.
     */
    private Function<String, Map<String, Object>> mapFactory() {
        return k -> new HashMap<>();
    }
    /**
     * Ensures the list is large enough to contain the specified index and fills missing slots with empty maps.
     *
     * @param collection the list to expand
     * @param key        the string representation of the index
     * @return the parsed index as an integer
     */
    private int ensureCollection(List<Object> collection, String key) {
        int index = Integer.parseInt(key);
        expand(collection, index, HashMap::new);
        return index;
    }

    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(ClassPair.of(Map.class, Map.class));
    }

}
