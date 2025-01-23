package svit.util;

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
     * Normalizes the path to the JAR file by removing the internal entry path
     * and retaining only the path to the JAR file itself.
     *
     * @param jar the URL pointing to a resource in a JAR file.
     * @return the normalized path to the JAR file.
     */
    public static String getBasePath(URL jar) {
        String path = jar.toExternalForm();

        // Handle "jar:" protocol and split at "!"
        int jarSeparatorIndex = path.indexOf(JAR_SEPARATOR);
        if (jarSeparatorIndex != -1) {
            path = path.substring(0, jarSeparatorIndex);
        }

        // Ensure the path ends with the actual JAR file name
        int lastSlashIndex = path.lastIndexOf(Files.SLASH);
        if (lastSlashIndex != -1 && !path.endsWith(JAR_EXTENSION)) {
            path = path.substring(0, lastSlashIndex);
        }

        return path;
    }

}
