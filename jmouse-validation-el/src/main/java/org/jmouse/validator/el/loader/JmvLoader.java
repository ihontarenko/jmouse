package org.jmouse.validator.el.loader;

import org.jmouse.validator.el.JmvReader;
import org.jmouse.validator.el.node.ValidationDocumentNode;
import org.jmouse.validator.el.runtime.CompiledValidation;
import org.jmouse.validator.el.translate.JmvCompiler;
import org.jmouse.validator.el.translate.ValidationTranslator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads every {@code .jmv} a set of locations names, compiles each one, and hands back a lookup. 📚
 *
 * <h2>⚠️ Everything happens here, and nothing happens later</h2>
 *
 * <p>Parsing, compiling and refusing a bad document all belong to load time. A document reached for the
 * first time while somebody is submitting a record turns a typo in a file into a failure in front of a
 * person who did not write it — and into a lexer and a parser behind a cache miss, on the one path that
 * runs per record.</p>
 *
 * <h2>⚠️ A document is addressed by what it calls itself, not by its file name</h2>
 *
 * <p>{@code validation "innoventa/part"} is the identity, and it survives the file being renamed or
 * moved — which is the whole reason the name is written inside. Two files claiming one name are refused
 * naming both, because the alternative is a product validating against whichever the classpath happened
 * to hand over first.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmvLoader {

    private final JmvSources                            sources;
    private final JmvReader                             reader;
    private final ValidationTranslator<CompiledValidation> compiler;

    /** A loader over the classpath and the standard vocabulary. */
    public JmvLoader() {
        this(JmvSources.ofClasspath(), new JmvReader(), new JmvCompiler());
    }

    public JmvLoader(
            JmvSources sources,
            JmvReader reader,
            ValidationTranslator<CompiledValidation> compiler
    ) {
        this.sources = sources;
        this.reader = reader;
        this.compiler = compiler;
    }

    /**
     * Loads every document the locations name.
     *
     * @param locations where to look — {@code classpath:validation}, a directory, whatever the
     *                  {@link JmvSources} understands
     * @return what was found, by the name each document calls itself
     * @throws JmvLoadException when a file will not read, or two claim one name
     */
    public Loaded load(String... locations) {
        Map<String, CompiledValidation> compiled = new LinkedHashMap<>();
        Map<String, String>             origins  = new LinkedHashMap<>();

        for (String location : locations) {
            for (JmvSource source : sources.at(location)) {
                register(source, compiled, origins);
            }
        }

        return new Loaded(compiled, origins);
    }

    /**
     * Reads and compiles one document, refusing a name already claimed.
     *
     * @param source   the file
     * @param compiled where compiled documents go
     * @param origins  which file each name came from, for the refusal
     */
    private void register(
            JmvSource source, Map<String, CompiledValidation> compiled, Map<String, String> origins) {

        ValidationDocumentNode document;

        try {
            document = reader.parse(source.text(), source.location());
        } catch (RuntimeException failure) {
            throw new JmvLoadException(
                    "'%s' could not be read: %s".formatted(source.location(), failure.getMessage()),
                    failure);
        }

        String claimed = origins.get(document.getName());

        if (claimed != null) {
            throw new JmvLoadException(
                    ("'%s' is declared twice — by '%s' and by '%s'. A document is addressed by the name "
                     + "it gives itself, so two files claiming one leave nothing to say which a product "
                     + "validates against")
                            .formatted(document.getName(), claimed, source.location()));
        }

        try {
            compiled.put(document.getName(), compiler.translate(document));
        } catch (RuntimeException refused) {
            throw new JmvLoadException(
                    "'%s' could not be compiled: %s".formatted(source.location(), refused.getMessage()),
                    refused);
        }

        origins.put(document.getName(), source.location());
    }

    /**
     * What one load produced.
     *
     * @param documents by the name each one gives itself
     * @param origins   which file each name came from
     */
    public record Loaded(Map<String, CompiledValidation> documents, Map<String, String> origins) {

        /**
         * ⚠️ <strong>Not {@code Map.copyOf}, which does not keep insertion order.</strong> The sources
         * are sorted so that two runs report the same file as the duplicate, and copying the result
         * into an unordered map threw that away on the last line — {@link #names()} came back shuffled
         * while every comment above it claimed a stable order. A defensive copy that changes the
         * meaning of what it copies is worse than none.
         */
        public Loaded {
            documents = Collections.unmodifiableMap(new LinkedHashMap<>(documents));
            origins = Collections.unmodifiableMap(new LinkedHashMap<>(origins));
        }

        /**
         * One document by name.
         *
         * @param name what it calls itself — {@code innoventa/part}
         * @return it, or empty when nothing was loaded under that name
         */
        public Optional<CompiledValidation> get(String name) {
            return Optional.ofNullable(documents.get(name));
        }

        /** @return every name loaded, so a refusal can list what would have worked */
        public List<String> names() {
            return List.copyOf(documents.keySet());
        }
    }
}
