package org.jmouse.validator.el.loader;

import org.jmouse.core.io.CompositeResourceLoader;
import org.jmouse.core.io.PatternMatcherResourceLoader;
import org.jmouse.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@code .jmv} documents found on the classpath. 📚
 *
 * <h2>⚠️ It walks nothing — {@code jmouse-core} already does</h2>
 *
 * <p>This class was a hundred and eighty lines: enumerate the class loader's URLs, branch on
 * {@code file} versus {@code jar}, list a directory, walk jar entries, filter by extension, sort. Every
 * line of it already existed one module down, in {@code org.jmouse.core.io} — a resource loader that
 * does exactly that, with an Ant matcher on top and the file-versus-jar branch behind it.</p>
 *
 * <p>⚠️ <strong>The duplication was not a shortcut anybody took; it was a module nobody looked in.</strong>
 * The mapping language grew its own copy first and this one was written from that copy. What is left
 * here is the only part that is about validation at all: turning a {@link Resource} into a
 * {@link JmvSource}.</p>
 *
 * <h2>⚠️ A location is a directory, and may be a pattern</h2>
 *
 * <p>{@code classpath:validation} means every {@code .jmv} <strong>directly inside</strong> it, which is
 * what the hand-written version did and what a product's configuration already says. A location that
 * carries its own glob is passed through untouched, so {@code classpath:validation/&#42;&#42;/&#42;.jmv}
 * reaches every one below it too — something the hand-written version could not do at all.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ClasspathJmvSources implements JmvSources {

    private static final String EXTENSION = ".jmv";
    private static final char   GLOB      = '*';

    private final PatternMatcherResourceLoader loader;

    public ClasspathJmvSources() {
        this(new CompositeResourceLoader());
    }

    public ClasspathJmvSources(PatternMatcherResourceLoader loader) {
        this.loader = loader;
    }

    @Override
    public List<JmvSource> at(String location) {
        List<JmvSource> documents = new ArrayList<>();

        for (Resource resource : loader.findResources(patternOf(location))) {
            documents.add(JmvSource.at(resource.getName(), textOf(resource)));
        }

        // ⚠️ Sorted, so two runs report the same file as the duplicate. Classpath order is not stable
        // across machines, and a load error naming a different file each time is one nobody can act on.
        documents.sort(Comparator.comparing(JmvSource::location));

        return documents;
    }

    /**
     * The pattern a configured location means.
     *
     * @param location as configured — a directory, or a glob of its own
     * @return what to hand the resource loader
     */
    private static String patternOf(String location) {
        if (location.indexOf(GLOB) >= 0) {
            return location;
        }

        String directory = location.endsWith("/") ? location.substring(0, location.length() - 1)
                                                  : location;

        return directory + "/*" + EXTENSION;
    }

    /**
     * A resource's text.
     *
     * <p>⚠️ Read here rather than through {@code Resource.asString()}, which swallows an
     * {@link IOException} and answers {@code null}. A document that silently becomes nothing is a
     * document whose rules silently stop applying — the failure a validation library may least afford.</p>
     *
     * @param resource what to read
     * @return its text
     */
    private static String textOf(Resource resource) {
        try (InputStream stream = resource.getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException unreadable) {
            throw new UncheckedIOException(
                    "could not read '%s'".formatted(resource.getName()), unreadable);
        }
    }
}
