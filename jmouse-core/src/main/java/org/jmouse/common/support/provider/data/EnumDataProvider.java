package org.jmouse.common.support.provider.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class EnumDataProvider<E extends Enum<E>> implements DataProvider<String, Object> {

    private final MapDataProvider dataProvider;

    public EnumDataProvider(Class<E> enumClass, Function<E, String> valueExtractor) {
        this.dataProvider = new MapDataProvider(extractKeyValue(enumClass, valueExtractor));
    }

    private Map<String, Object> extractKeyValue(Class<E> enumClass, Function<E, String> valueExtractor) {
        Map<String, Object> keyValue = new HashMap<>();

        for (E enumConstant : enumClass.getEnumConstants()) {
            keyValue.put(enumConstant.name(), valueExtractor.apply(enumConstant));
        }

        return keyValue;
    }

    @Override
    public Object getValue(String key) {
        return dataProvider.getValue(key);
    }

    @Override
    public Map<String, Object> getValuesMap() {
        return dataProvider.getValuesMap();
    }

    @Override
    public Set<Object> getValuesSet() {
        return dataProvider.getValuesSet();
    }

}
