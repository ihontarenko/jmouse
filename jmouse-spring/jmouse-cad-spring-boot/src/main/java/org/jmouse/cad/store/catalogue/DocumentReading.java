package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.jpa.catalogue.DocumentStore;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.DocumentIndex;
import org.jmouse.cad.library.DocumentModels;
import org.jmouse.cad.store.CadProperties;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.el.grammar.GrammarRefusal;

import java.util.Collections;
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

    private final int                       maximumLength;
    private final Map<String, ReadDocument> cache;
    private final CadLanguage               language;

    public DocumentReading(CadProperties properties, CadLanguage language) {
        this.language = language;
        this.maximumLength = properties.documents().maximumLength();
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
        if (source.length() > maximumLength) {
            throw CadProblem.invalid("A document is at most " + maximumLength + " characters.");
        }
        String checksum = DocumentStore.checksumOf(source);
        ReadDocument cached = cache.get(checksum);
        if (cached != null) {
            return cached;
        }
        ReadDocument read = parse(source, checksum, origin);
        cache.put(checksum, read);
        return read;
    }

    /**
     * A stored revision, read once: the text is fetched only when nothing is kept under its checksum.
     *
     * @param checksum the stored checksum of the text
     * @param source   fetches the text
     * @param origin   what a refusal names it by
     */
    public ReadDocument stored(String checksum, Supplier<String> source, String origin) {
        ReadDocument cached = cache.get(checksum);
        if (cached != null) {
            return cached;
        }
        ReadDocument read = parse(source.get(), checksum, origin);
        cache.put(checksum, read);
        return read;
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
            Optional<Object> model = DocumentModels.of(document);
            return new ReadDocument(source, checksum, document, DocumentIndex.of(document), model);
        } catch (GrammarRefusal refusal) {
            throw CadProblem.invalidDocument(refusal.detail(), refusal.line(), refusal.column());
        } catch (IllegalArgumentException | IllegalStateException refused) {
            throw CadProblem.invalidDocument(refused.getMessage(), document.line(), 0);
        }
    }
}
