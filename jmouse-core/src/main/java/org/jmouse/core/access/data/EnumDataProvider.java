package org.jmouse.core.access.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * {@link DataProvider} backed by enum constants. 🧩
 *
 * <p>
 * Builds a key–value map from an enum type where keys are enum constant
 * names and values are extracted using the provided function.
 * </p>
 *
 * @param <E> enum type
 */
public class EnumDataProvider<E extends Enum<E>> implements DataProvider<String, Object> {

    private final MapDataProvider dataProvider;

    /**
     * Creates provider from enum class and value extractor.
     *
     * @param enumClass      enum type
     * @param valueExtractor function extracting value from enum constant
     */
    public EnumDataProvider(Class<E> enumClass, Function<E, String> valueExtractor) {
        this.dataProvider = new MapDataProvider(extractKeyValue(enumClass, valueExtractor));
    }

    /**
     * Extracts key–value pairs from enum constants.
     */
    private Map<String, Object> extractKeyValue(Class<E> enumClass, Function<E, String> valueExtractor) {
        Map<String, Object> keyValue = new HashMap<>();

        for (E enumConstant : enumClass.getEnumConstants()) {
            keyValue.put(enumConstant.name(), valueExtractor.apply(enumConstant));
        }

        return keyValue;
    }

    /**
     * Returns value associated with the given enum key.
     */
    @Override
    public Object getValue(String key) {
        return dataProvider.getValue(key);
    }

    /**
     * Returns all values as a map.
     */
    @Override
    public Map<String, Object> getValuesMap() {
        return dataProvider.getValuesMap();
    }

    /**
     * Returns all values as a set.
     */
    @Override
    public Set<Object> getValuesSet() {
        return dataProvider.getValuesSet();
    }

}