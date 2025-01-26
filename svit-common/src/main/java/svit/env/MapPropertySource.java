package svit.env;

import java.util.Map;

/**
 * A {@link PropertySource} implementation that uses a {@link Map} as the underlying property source.
 * <p>
 * This class provides methods to access properties stored in the map by their keys and supports operations
 * like checking property existence and retrieving all property names.
 * </p>
 */
public class MapPropertySource extends AbstractPropertySource<Map<String, Object>> {

    /**
     * Constructs a new {@link MapPropertySource} with the specified name and map source.
     *
     * @param name   the name of the property source
     * @param source the map containing property key-value pairs
     */
    public MapPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    /**
     * Retrieves the value of a property by its name.
     *
     * @param name the name of the property to retrieve
     * @return the property value, or {@code null} if the property is not found
     */
    @Override
    public Object getProperty(String name) {
        return this.source.get(name);
    }

    /**
     * Checks if the specified property exists in the map.
     *
     * @param name the name of the property to check
     * @return {@code true} if the property exists, {@code false} otherwise
     */
    @Override
    public boolean containsProperty(String name) {
        return this.source.containsKey(name);
    }

    /**
     * Returns all property names available in the map.
     *
     * @return an array of property names
     */
    @Override
    public String[] getPropertyNames() {
        return getSource().keySet().toArray(String[]::new);
    }
}
