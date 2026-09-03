package org.jmouse.helpers;

import java.net.URL;

final public class Jars {

    public static final String JAR_SEPARATOR = "!";
    public static final String JAR_TOKEN     = "!/";
    public static final String JAR_EXTENSION = ".jar";

    private Jars() {
    }

    public static boolean isJarURL(String url) {
        return url != null && url.contains(JAR_TOKEN);
    }

    /**
     * Everything up to the archive a resource lives in — the half a fresh entry name is appended to.
     *
     * <h2>⚠️ The LAST {@code !/}, never the first, and a Spring Boot fat jar is why</h2>
     *
     * <p>A plain archive URL carries one separator, so first and last are the same string. Spring Boot
     * 3.2 and later reach inside a fat jar through the {@code nested:} scheme, and those carry
     * <strong>two</strong>:</p>
     *
     * <pre>
     * jar:nested:/x/app.jar/!BOOT-INF/classes/!/mapping/device.jmm
     *                      ↑ first             ↑ the real separator
     * </pre>
     *
     * <p>Cutting at the first one answers {@code jar:nested:/x/app.jar} and drops
     * {@code /!BOOT-INF/classes} entirely. Appending an entry to that produces a URL whose nested entry
     * name is empty, and opening it fails with {@code nestedEntryName must not be empty} — a message
     * that names neither the file nor the caller, from a product that worked under
     * {@code spring-boot:run} minutes earlier.</p>
     *
     * <p>⚠️ The resource separator is always the last {@code !/} because everything before it addresses
     * an archive and everything after it addresses a path inside one. That holds for a resource inside
     * a dependency too — {@code …/app.jar/!BOOT-INF/lib/core.jar!/org/jmouse/} — which is the case that
     * makes this about every library, not only about the application's own classes.</p>
     *
     * @param jar the URL pointing to a resource in a JAR file.
     * @return the normalized path to the JAR file.
     */
    public static String getBasePath(URL jar) {
        String path                = jar.toExternalForm();
        int    separatorIndex      = path.lastIndexOf(JAR_TOKEN);

        if (separatorIndex != -1) {
            // ⚠️ Returned as it is. The trailing branch below must NOT run: a correctly cut nested base
            // usually ends in a directory such as `classes/`, and dropping its last segment would throw
            // away the very part this method exists to keep.
            return path.substring(0, separatorIndex);
        }

        // No separator at all, so this is not an archive URL. Keep the older behaviour: trim back to
        // the containing directory unless the path already names the archive itself.
        int lastSlashIndex = path.lastIndexOf(Files.SLASH);

        if (lastSlashIndex != -1 && !path.endsWith(JAR_EXTENSION)) {
            path = path.substring(0, lastSlashIndex);
        }

        return path;
    }

    /**
     * The entry path inside the archive — the other half of what {@link #getBasePath(URL)} keeps.
     *
     * <p>⚠️ Split at the last {@code !/} for the same reason, and it matters even though nothing calls
     * this today: the two methods are read as a pair, and one of them quietly disagreeing with the
     * other about where a URL divides is a defect waiting for its first caller.</p>
     *
     * @param jar the URL pointing to a resource in a JAR file.
     * @return the path of the entry within the archive, leading slash included.
     */
    public static String getFilePath(URL jar) {
        String path           = jar.toExternalForm();
        int    separatorIndex = path.lastIndexOf(JAR_TOKEN);

        if (separatorIndex != -1) {
            path = path.substring(separatorIndex + JAR_SEPARATOR.length());
        }

        return path;
    }

}
