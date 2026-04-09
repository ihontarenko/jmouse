package org.jmouse.jdbc.operation.resolve;

/**
 * Loads SQL text from an external resource.
 *
 * @author Ivan Hontarenko
 */
public interface SqlTextLoader {

    /**
     * Loads SQL text from the given resource location.
     *
     * @param location resource location
     *
     * @return loaded SQL text
     */
    String load(String location);

}