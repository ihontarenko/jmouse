package org.jmouse.parallax.script;

import org.jmouse.units.Quantity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What describes a document without changing what it computes: the {@code meta { … }} block.
 *
 * <p>The known fields are typed and checked. Every other field is kept with its type and shown as a property of the
 * object — {@code symmetry "rotational about the z axis"}, {@code supply_range 4.5 V .. 16 V} — so a curator can say
 * anything about an object without a new column, and nothing an author wrote is lost.</p>
 */
public record Meta(String summary, String description, String category, List<String> tags, String source,
                   List<String> authors, String license, List<Link> links, Map<String, MetaValue> fields,
                   Map<String, String> labels) {

    public static final Meta EMPTY = new Meta(null, null, null, List.of(), null, List.of(), null, List.of(), Map.of(),
            Map.of());

    /**
     * @param fields the free fields, in the order written
     * @param labels how a free field is shown when its name in words is not enough: {@code cut_off "f = 1 / (2π RC)"
     *               "Cut-off"} — the text in quotes after the value, as a parameter's description follows it
     */
    public Meta {
        tags = List.copyOf(tags);
        authors = List.copyOf(authors);
        links = List.copyOf(links);
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        labels = Collections.unmodifiableMap(new LinkedHashMap<>(labels));
    }

    /** The label written after a free field's value, if one was. */
    public Optional<String> label(String name) {
        return Optional.ofNullable(labels.get(name));
    }

    /** An article, a datasheet, a video: a label and where it is. */
    public record Link(String label, String url) {
    }

    public Optional<String> summaryText() {
        return Optional.ofNullable(summary);
    }

    public Optional<MetaValue> field(String name) {
        return Optional.ofNullable(fields.get(name));
    }

    /** A free field's value, typed as written. */
    public sealed interface MetaValue {

        /** {@code "SOIC-8"} */
        record Text(String text) implements MetaValue {
        }

        /** {@code 1972}, {@code 4.7 nF} */
        record Amount(Quantity quantity) implements MetaValue {
        }

        /** {@code 4.5 V .. 16 V} */
        record Span(Quantity minimum, Quantity maximum) implements MetaValue {
        }

        /** {@code timer, oscillator} */
        record Names(List<String> names) implements MetaValue {
            public Names {
                names = List.copyOf(names);
            }
        }

        /** {@code ["a", 2, 3 V]} */
        record Items(List<MetaValue> items) implements MetaValue {
            public Items {
                items = List.copyOf(items);
            }
        }
    }
}
