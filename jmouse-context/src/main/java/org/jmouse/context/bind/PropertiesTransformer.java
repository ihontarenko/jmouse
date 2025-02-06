package org.jmouse.context.bind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Transforms a flat key-value {@link Map} into a nested structure based on hierarchical keys.
 * <p>
 * Keys are parsed using {@link NamePath}, allowing for structured representation with maps and lists.
 * <p>
 * Example input:
 * <pre>
 *     a.b.c = 10
 *     a.b.d = 20
 *     a.list[0] = "first"
 *     a.list[1] = "second"
 * </pre>
 * <p>
 * Output:
 * <pre>
 * {
 *     "a": {
 *         "b": {
 *             "c": 10,
 *             "d": 20
 *         },
 *         "list": ["first", "second"]
 *     }
 * }
 * </pre>
 */

public class PropertiesTransformer {

    private static final Function<String, List<Object>>        FACTORY_LIST = (key) -> new ArrayList<>();
    private static final Function<String, Map<String, Object>> FACTORY_MAP  = (key) -> new HashMap<>();

    /**
     * Transforms a flat key-value map into a nested structure.
     *
     * @param properties the flat map with dot-separated or indexed keys
     * @return a nested map representation
     */
    @SuppressWarnings({"unchecked"})
    public static Map<String, Object> transform(Map<String, Object> properties) {
        Map<String, Object> nested = new HashMap<>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            NamePath.Entries    entries    = NamePath.of(entry.getKey()).entries();
            Object              value      = entry.getValue();
            Map<String, Object> current    = nested;
            List<Object>        collection = null;

            for (int i = 0; i < entries.size(); i++) {
                NamePath.Type type = entries.type(i);
                String        key  = entries.get(i).toString();

                if (type.isEmpty()) {
                    continue;
                }

                if (type.isNumeric() && collection != null) {
                    int index = Integer.parseInt(key);
                    if (entries.isLast(i)) {
                        collection.set(index, value);
                    } else {
                        current = (Map<String, Object>) collection.get(index);
                        if (current == null) {
                            current = FACTORY_MAP.apply(key);
                            collection.set(index, current);
                        }
                    }
                } else {
                    if (entries.isLast(i)) {
                        current.put(key, value);
                    } else if (entries.type(i + 1).isNumeric()) {
                        Object source = current.get(key);

                        if (source instanceof Map<?, ?>) {
                            throw new IllegalStateException(
                                    "Property key '%s' cannot be converted to collection".formatted(entries));
                        } else if (source == null) {
                            source = FACTORY_LIST.apply(key);
                            current.put(key, source);
                        }

                        collection = (List<Object>) source;
                        expand(collection, Integer.parseInt(entries.get(i + 1).toString()));
                    } else {
                        Object source = current.get(key);

                        if (source instanceof List<?>) {
                            throw new IllegalStateException(
                                    "Property key '%s' cannot be converted to map".formatted(entries));
                        } else if (source == null) {
                            source = FACTORY_MAP.apply(key);
                            current.put(key, source);
                        }

                        current = (Map<String, Object>) source;
                    }
                }
            }
        }

        return nested;
    }

    /**
     * Expands a list to ensure it has at least the given index.
     *
     * @param collection the list to expand
     * @param index      the target index
     */
    public static void expand(List<Object> collection, int index) {
        while (collection.size() <= index) {
            collection.add(null);
        }
    }

}
