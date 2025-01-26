package svit.io;

import java.net.URL;

/**
 * A specialized {@link URLResource} implementation for resources located in the Java Runtime (JRT) module system.
 * <p>
 * This class adds specific handling and naming conventions for JRT-based resources.
 * </p>
 */
public class JrtURLResource extends URLResource {

    /**
     * Constructs a new {@link JrtURLResource} for the specified JRT {@link URL}.
     *
     * @param url the {@link URL} of the JRT resource
     */
    public JrtURLResource(URL url) {
        super(url);
    }

    /**
     * Returns a human-readable name for the resource, prefixed with "JRT_".
     */
    @Override
    public String getResourceName() {
        return "JRT_%s".formatted(super.getResourceName());
    }

}
