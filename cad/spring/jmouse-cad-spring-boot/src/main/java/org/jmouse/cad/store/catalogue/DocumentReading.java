package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.DocumentIndex;
import org.jmouse.cad.store.CadProperties;
import org.jmouse.cad.store.plugin.EngineConfiguration;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.el.grammar.GrammarRefusal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Reads a document's text with the whole jM language, and refuses one that does not read in the API's words: where it
 * went wrong when the grammar says so, what the engine refused when its model does not read.
 *
 * <p>A document is read once per revision: what was read is kept by the text's checksum, the most recently used
 * {@code jmouse.cad.documents.cached-readings} of them.</p>
 */
public class DocumentReading {

    private final EngineConfiguration       configuration;
    private final Map<String, ReadDocument> cache;
    /** The readings of the pass this thread is in ({@link #readingEachOnce}); none outside one. */
    private final ThreadLocal<Map<String, ReadDocument>> pass = new ThreadLocal<>();
    private final CadLanguage               language;

    public DocumentReading(CadProperties properties, EngineConfiguration configuration, CadLanguage language) {
        this.language = language;
        this.configuration = configuration;
        int capacity = properties.documents().cachedReadings();
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ReadDocument> eldest) {
                return size() > capacity;
            }
        });
    }

    /** The language every document is read with: the engine's modules and the plugins'. */
    public CadLanguage language() {
        return language;
    }

    /**
     * A text read now, and refused when it does not read.
     *
     * @param source the text
     * @param origin what a refusal names it by: its slug, "the new document"
     */
    public ReadDocument read(String source, String origin) {
        if (source == null || source.isBlank()) {
            throw CadProblem.invalid("A document is its text: send it as 'source'.");
        }
        int maximumLength = configuration.maximumDocumentLength();
        if (source.length() > maximumLength) {
            throw CadProblem.invalid("A document is at most " + maximumLength + " characters.");
        }
        String checksum = DocumentStore.checksumOf(source);
        return kept(checksum, () -> source, origin);
    }

    /**
     * A stored revision, read once: the text is fetched only when nothing is kept under its checksum.
     *
     * @param checksum the stored checksum of the text
     * @param source   fetches the text
     * @param origin   what a refusal names it by
     */
    public ReadDocument stored(String checksum, Supplier<String> source, String origin) {
        return kept(checksum, source, origin);
    }

    /**
     * Work that reads many documents more than once — the catalogue's index, made at start: within it each text is read once,
     * however many there are, the most recently used kept besides as always. Outside it the readings kept are only those.
     */
    public <T> T readingEachOnce(Supplier<T> work) {
        Map<String, ReadDocument> outer = pass.get();
        if (outer != null) {
            return work.get();
        }
        pass.set(new HashMap<>());
        try {
            return work.get();
        } finally {
            pass.remove();
        }
    }

    /** What was read under a checksum: kept by the pass or the cache, else read now and kept by both. */
    private ReadDocument kept(String checksum, Supplier<String> source, String origin) {
        Map<String, ReadDocument> passing = pass.get();
        ReadDocument known = passing != null ? passing.get(checksum) : null;
        if (known == null) {
            known = cache.get(checksum);
        }
        if (known == null) {
            known = parse(source.get(), checksum, origin);
            cache.put(checksum, known);
        }
        if (passing != null) {
            passing.put(checksum, known);
        }
        return known;
    }

    private ReadDocument parse(String source, String checksum, String origin) {
        DocumentNode document;
        try {
            document = language.language().readOne(DocumentInput.text(origin, source));
        } catch (GrammarRefusal refusal) {
            throw CadProblem.invalidDocument(refusal.detail(), refusal.line(), refusal.column());
        } catch (IllegalArgumentException | IllegalStateException refused) {
            throw CadProblem.invalidDocument(refused.getMessage(), 0, 0);
        }
        try {
            Optional<Object> model = language.modelOf(document);
            return new ReadDocument(source, checksum, document, DocumentIndex.of(document), model);
        } catch (GrammarRefusal refusal) {
            throw CadProblem.invalidDocument(refusal.detail(), refusal.line(), refusal.column());
        } catch (IllegalArgumentException | IllegalStateException refused) {
            throw CadProblem.invalidDocument(refused.getMessage(), document.line(), 0);
        }
    }
}
