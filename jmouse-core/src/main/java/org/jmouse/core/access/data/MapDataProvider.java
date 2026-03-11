package org.jmouse.core.access.data;

import java.util.Map;

/**
 * {@link DataProvider} implementation backed by a {@link Map}. 🗂️
 *
 * <p>
 * Provides simple key–value access where keys are {@link String}
 * and values are arbitrary objects.
 * </p>
 */
public class MapDataProvider extends AbstractDataProvider<String, Object> {

    /**
     * Creates provider using the given map as storage.
     *
     * @param keyValues key–value data
     */
    public MapDataProvider(Map<String, Object> keyValues) {
        super(keyValues);
    }

}