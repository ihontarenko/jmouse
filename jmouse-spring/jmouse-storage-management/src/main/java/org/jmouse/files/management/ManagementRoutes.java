package org.jmouse.files.management;

/**
 * 🛣️ Where this module's routes live.
 *
 * <p>⚠️ <strong>Stated once and read by everything.</strong> The address appears in the controller, in
 * a product's {@code ExternalAccessRules}, in its security configuration and in whatever the interface
 * is built against — and when those drift the failure is not an error, it is a screen that renders as
 * an empty installation. That has already happened once to the AI management screen.</p>
 */
public final class ManagementRoutes {

    /** Everything this module serves sits under here. */
    public static final String BASE = "/api/files";

    /** One file. */
    public static final String ONE = BASE + "/{fileId}";

    /** The bytes of one file. */
    public static final String CONTENT = ONE + "/content";

    /** Fetching a file from a web address rather than sending its bytes. */
    public static final String IMPORT = BASE + "/import";

    /** Where one file is filed. */
    public static final String BINDING = ONE + "/binding";

    /** Whether a file is listed only to whoever may already reach it. */
    public static final String PRIVACY = ONE + "/private";

    /** The directory tree. */
    public static final String DIRECTORIES = "/api/directories";

    /** One directory. */
    public static final String DIRECTORY = DIRECTORIES + "/{directoryId}";

    /** One directory and everything under it. */
    public static final String DIRECTORY_SUBTREE = DIRECTORY + "/subtree";

    /** Where one directory sits. */
    public static final String DIRECTORY_PARENT = DIRECTORY + "/parent";

    private ManagementRoutes() {
    }
}
