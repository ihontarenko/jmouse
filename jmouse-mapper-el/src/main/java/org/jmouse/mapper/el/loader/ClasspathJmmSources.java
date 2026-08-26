package org.jmouse.mapper.el.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * {@code .jmm} documents found on the classpath, with no framework underneath. 📚
 *
 * <h2>⚠️ Why the library ships a working one rather than only an interface</h2>
 *
 * <p>{@code .jmp} declares {@code PolicySources} and leaves every implementation to a Spring module,
 * which is right for a policy: an installation has one, it is administered, and it is loaded by an
 * application container. A mapping is not like that — a library that maps objects should be usable
 * from a test, a command-line tool or a plain {@code main} without acquiring a web framework first.
 * So the interface is the seam and this is a default that works, and a Spring module may still replace
 * it with a resource-pattern resolver that does more.</p>
 *
 * <h2>⚠️ What it does and does not resolve</h2>
 *
 * <p>A location is a directory — {@code classpath:mapping}, or bare {@code mapping} — and every
 * {@code .jmm} <strong>directly inside it</strong> is a document. Deliberately not a glob: {@code **}
 * over a classpath means walking every jar on it, which is a scan whose cost nobody expects from a
 * mapping library and whose results depend on packaging. A product that wants patterns supplies its
 * own {@link JmmSources}.</p>
 *
 * <p>Both a directory on disk and an entry in a jar are read, because the second is what a packaged
 * application actually has.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ClasspathJmmSources implements JmmSources {

    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String EXTENSION        = ".jmm";

    private final ClassLoader loader;

    public ClasspathJmmSources() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public ClasspathJmmSources(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public List<JmmSource> at(String location) {
        String directory = directoryOf(location);

        List<JmmSource> documents = new ArrayList<>();

        try {
            Enumeration<URL> roots = loader.getResources(directory);

            while (roots.hasMoreElements()) {
                collect(roots.nextElement(), directory, documents);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "could not read '%s' from the classpath".formatted(location), exception);
        }

        // ⚠️ Sorted, so two runs report the same file as the duplicate. Classpath order is not stable
        // across machines, and a load error that names a different file each time is one nobody can act
        // on.
        documents.sort((first, second) -> first.location().compareTo(second.location()));

        return documents;
    }

    /**
     * Reads every {@code .jmm} under one classpath root.
     *
     * @param root      where the directory was found
     * @param directory the directory, as a classpath path
     * @param into      where to add what is found
     * @throws IOException when the root cannot be read
     */
    private void collect(URL root, String directory, List<JmmSource> into) throws IOException {
        if ("jar".equals(root.getProtocol())) {
            collectFromJar(root, directory, into);

            return;
        }

        // ⚠️ toURI(), not URI.create(toString()). The second throws on any character the URL left
        // unencoded, and a checkout under a directory with a space in its name is the ordinary way to
        // meet one — a failure that depends on where somebody put the project.
        Path base;

        try {
            base = Paths.get(root.toURI());
        } catch (URISyntaxException malformed) {
            throw new IOException("'%s' is not a readable location".formatted(root), malformed);
        }

        if (!Files.isDirectory(base)) {
            return;
        }

        try (Stream<Path> entries = Files.list(base)) {
            for (Path entry : entries.sorted().toList()) {
                if (Files.isRegularFile(entry) && entry.getFileName().toString().endsWith(EXTENSION)) {
                    into.add(JmmSource.at(directory + "/" + entry.getFileName(),
                                          Files.readString(entry, StandardCharsets.UTF_8)));
                }
            }
        }
    }

    /**
     * Reads every {@code .jmm} directly inside one directory of a jar.
     *
     * @param root      the jar URL the directory was found at
     * @param directory the directory, as a classpath path
     * @param into      where to add what is found
     * @throws IOException when the jar cannot be read
     */
    private void collectFromJar(URL root, String directory, List<JmmSource> into) throws IOException {
        URLConnection connection = root.openConnection();

        if (!(connection instanceof JarURLConnection jarConnection)) {
            return;
        }

        // ⚠️ Not closed: a JarFile obtained through a URLConnection with caching on is shared with the
        // class loader, and closing it here closes it for everybody.
        JarFile               jar     = jarConnection.getJarFile();
        Enumeration<JarEntry> entries = jar.entries();
        String                prefix  = directory.endsWith("/") ? directory : directory + "/";

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String   name  = entry.getName();

            if (entry.isDirectory() || !name.startsWith(prefix) || !name.endsWith(EXTENSION)) {
                continue;
            }

            if (name.indexOf('/', prefix.length()) >= 0) {
                continue;
            }

            try (InputStream stream = jar.getInputStream(entry)) {
                into.add(JmmSource.at(name, new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
            }
        }
    }

    /**
     * The classpath directory a location names.
     *
     * @param location as configured, with or without the {@code classpath:} prefix
     * @return the directory
     */
    private String directoryOf(String location) {
        String path = location.startsWith(CLASSPATH_PREFIX)
                ? location.substring(CLASSPATH_PREFIX.length())
                : location;

        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
