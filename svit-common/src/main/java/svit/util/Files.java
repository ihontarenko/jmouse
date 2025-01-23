package svit.util;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Paths;

final public class Files {

    public static final char COLON = ':';
    public static final String SLASH = "/";
    public static final String SYSTEM_SLASH = FileSystems.getDefault().getSeparator();


    private Files() {
    }

    /**
     * Extracts the protocol from a given path.
     *
     * @param path           the input path
     * @param defaultProtocol the default protocol to return if no protocol is found
     * @return the extracted protocol, or the default protocol if none exists
     * <p>
     */
    public static String extractProtocol(String path, String defaultProtocol) {
        int colonIndex = path.indexOf(COLON);

        if (colonIndex == -1) {
            return defaultProtocol;
        }

        return path.substring(0, colonIndex);
    }

    public static String removeProtocol(String path) {
        String protocol = extractProtocol(path, null);

        if (protocol != null) {
            path = path.substring(protocol.length() + 1);
        }

        return path;
    }

    public static String getPath(URL url) {
        try {
            return Paths.get(url.toURI()).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
