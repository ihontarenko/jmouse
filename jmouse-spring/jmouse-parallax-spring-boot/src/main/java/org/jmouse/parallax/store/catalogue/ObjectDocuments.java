package org.jmouse.parallax.store.catalogue;

import org.jmouse.parallax.jpa.catalogue.ObjectStore;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.formula.FormulaException;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;
import org.jmouse.parallax.problem.ParallaxProblem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Turns an object's jMX text into what it describes, through the adapter in {@code jmouse-parallax}
 * ({@link ExperienceDefinitions}), and back.
 *
 * <p>A read document is cached under the checksum of its text: an object's definition, sliders, representations,
 * presets and properties are read once per revision, however often the catalogue lists or opens it. A text that does
 * not read is refused where it went wrong, with its line and column.</p>
 */
public class ObjectDocuments {

    private static final int CACHE_CAPACITY = 4096;

    private final Map<String, DefinedObject> readByChecksum = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, DefinedObject> eldest) {
                    return size() > CACHE_CAPACITY;
                }
            });

    /**
     * What a text describes.
     *
     * @param documentName what the text is called in a refusal: a file in the catalogue, an object's slug
     * @throws ParallaxProblem.Invalid {@code invalid-document} with the line and column when the text does not read
     */
    public DefinedObject read(String source, String documentName) {
        return cached(ObjectStore.checksumOf(source), () -> source, documentName);
    }

    /**
     * The object stored under a checksum, reading its text only when nothing under that checksum was read yet.
     *
     * @param source supplies the text on a miss
     */
    public DefinedObject cached(String checksum, Supplier<String> source, String documentName) {
        DefinedObject known = readByChecksum.get(checksum);
        if (known != null) {
            return known;
        }
        DefinedObject read = readText(source.get(), documentName);
        readByChecksum.put(checksum, read);
        return read;
    }

    /**
     * An object as the text the reader accepts — how a definition built by a form becomes a document.
     *
     * @throws ParallaxProblem.Invalid when a value cannot be written so that it reads back the same
     */
    public String write(DefinedObject object) {
        try {
            return ExperienceDefinitions.write(object);
        } catch (IllegalArgumentException refused) {
            throw ParallaxProblem.invalid("invalid-document", refused.getMessage());
        }
    }

    /**
     * A formula of a document that does not compile, refused at its place in the text. Expressions travel from the
     * text verbatim, so the failing expression is found in it and the compiler's own offset added.
     */
    public static ParallaxProblem.Invalid refusal(FormulaException failure, String source) {
        String detail = failure.position() >= 0
                ? failure.getMessage() + " (at character " + (failure.position() + 1) + " of '" + failure.source() + "')"
                : failure.getMessage();
        int start = failure.source() == null || failure.source().isEmpty() ? -1 : source.indexOf(failure.source());
        if (start < 0) {
            return ParallaxProblem.invalidDocument(detail, 1, 0);
        }
        int offset = start + Math.max(failure.position(), 0);
        int lineStart = source.lastIndexOf('\n', offset - 1) + 1;
        int line = (int) source.substring(0, offset).chars().filter(character -> character == '\n').count() + 1;
        int column = offset - lineStart + 1;
        return ParallaxProblem.invalidDocument("Line " + line + ", column " + column + ": " + detail, line, column);
    }

    private static DefinedObject readText(String source, String documentName) {
        try {
            return ExperienceDefinitions.read(source, documentName);
        } catch (ExperienceParseException unreadable) {
            throw ParallaxProblem.invalidDocument(unreadable.getMessage(), unreadable.getLine(), unreadable.getColumn());
        } catch (FormulaException unreadable) {
            throw refusal(unreadable, source);
        } catch (IllegalArgumentException | IllegalStateException unreadable) {
            throw ParallaxProblem.invalidDocument(documentName + ": " + unreadable.getMessage(), 1, 0);
        }
    }
}
