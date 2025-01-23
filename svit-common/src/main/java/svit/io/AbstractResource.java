package svit.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * An abstract base class for {@link Resource} implementations.
 * @see Resource
 */
public abstract class AbstractResource implements Resource {

    /**
     * Returns a {@link Reader} for reading the contents of the resource.
     */
    @Override
    public Reader getReader() {
        try {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        } catch (IOException e) {
            throw new ResourceException("Failed to create reader for: " + getResourceName(), e);
        }
    }

    /**
     * Returns a string representation of the resource.
     */
    @Override
    public String toString() {
        return "%s : %s".formatted(getResourceName(), getName());
    }
}
