package svit.env;

import java.util.Map;
import java.util.Properties;

/**
 * A {@link PropertySource} implementation that uses {@link Properties} as the underlying property source.
 * <p>
 * This class extends {@link MapPropertySource}, treating the {@link Properties} object as a {@link Map}.
 * It allows seamless integration of Java's {@link Properties} with the property source abstraction.
 * </p>
 */
public class JavaPropertiesPropertySource extends MapPropertySource {

    /**
     * Constructs a new {@link JavaPropertiesPropertySource} with the specified name and {@link Properties} source.
     *
     * @param name   the name of the property source
     * @param source the {@link Properties} object containing the property key-value pairs
     */
    @SuppressWarnings("unchecked") // Safe cast from Properties to Map<String, Object>
    public JavaPropertiesPropertySource(String name, Properties source) {
        super(name, (Map) source);
    }

}
