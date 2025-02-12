package org.jmouse;

/**
 * Represents a module in the JMouse framework.
 * <p>
 * This class stores descriptor about a module, including its name, version,
 * and a reference to the root class that serves as its entry point.
 * </p>
 */
final public class JMouseModule {

    private String   name;
    private String   version;
    private Class<?> root;

    /**
     * Retrieves the name of the module.
     *
     * @return the module name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the module.
     *
     * @param name the module name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the version of the module.
     *
     * @return the module version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of the module.
     *
     * @param version the module version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Retrieves the root class of the module.
     * <p>
     * The root class serves as the entry point for the module.
     * </p>
     *
     * @return the root class of the module
     */
    public Class<?> getRoot() {
        return root;
    }

    /**
     * Sets the root class of the module.
     *
     * @param root the root class to set
     */
    public void setRoot(Class<?> root) {
        this.root = root;
    }

    /**
     * Returns a string representation of the module.
     *
     * @return a formatted string containing the module's name, version, and root class
     */
    @Override
    public String toString() {
        return "JMouseModule: '%s [%s]'; Anchor Class: %s".formatted(name, version, root);
    }
}
