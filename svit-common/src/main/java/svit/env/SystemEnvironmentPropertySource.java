package svit.env;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link PropertySource} implementation that uses system environment variables as the underlying property source.
 * <p>
 * This class extends {@link MapPropertySource} and initializes the property source with the system's environment variables,
 * making them accessible through the property source abstraction.
 * </p>
 */
public class SystemEnvironmentPropertySource extends MapPropertySource {

    /**
     * Constructs a new {@link SystemEnvironmentPropertySource} with the specified name.
     *
     * @param name the name of the property source
     */
    public SystemEnvironmentPropertySource(String name) {
        super(name, new HashMap<>(System.getenv()));
        setOrder(Integer.MAX_VALUE);
    }

}
