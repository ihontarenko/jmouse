package org.jmouse.cad.settings;

import org.jmouse.settings.SectionReader;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * 🧾 How the engine reads a section: a tree — the file a module ships, or a section's rows folded back (ADR 0007), every value
 * text — read strictly into its record, each text taken as the type the record says. A field the record does not have, a value
 * missing from one it does or a null for a number is refused, naming where: a section that is not its record never stands.
 */
public final class CadSectionReader implements SectionReader {

    /** The one reader: it keeps nothing but its mapper. */
    public static final CadSectionReader JSON = new CadSectionReader();

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .build();

    private CadSectionReader() {
    }

    @Override
    public <T> T read(Object tree, Class<T> type) {
        try {
            return MAPPER.convertValue(tree, type);
        } catch (JacksonException refused) {
            throw new IllegalArgumentException(placeOf(refused) + refused.getOriginalMessage(), refused);
        } catch (IllegalArgumentException refused) {
            if (refused.getCause() instanceof JacksonException cause) {
                throw new IllegalArgumentException(placeOf(cause) + cause.getOriginalMessage(), cause);
            }
            throw refused;
        }
    }

    /** A file's text as a tree: records as maps, lists as lists, its values as JSON writes them. */
    public Object treeOf(String text) {
        try {
            return MAPPER.readValue(text, Object.class);
        } catch (JacksonException unreadable) {
            throw new IllegalArgumentException(unreadable.getOriginalMessage(), unreadable);
        }
    }

    /** Where in the section a refusal is — {@code rail: }, {@code layers[2].name: } —; empty for the whole of it. */
    private static String placeOf(JacksonException refused) {
        StringBuilder place = new StringBuilder();
        for (JacksonException.Reference reference : refused.getPath()) {
            if (reference.getPropertyName() != null) {
                place.append(place.isEmpty() ? "" : ".").append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                place.append("[").append(reference.getIndex()).append("]");
            }
        }
        return place.isEmpty() ? "" : place + ": ";
    }

    /** A value as a section writes it: an enum's constant as the word the reader takes for it. */
    static String written(Object value) {
        return MAPPER.valueToTree(value).asString();
    }
}
