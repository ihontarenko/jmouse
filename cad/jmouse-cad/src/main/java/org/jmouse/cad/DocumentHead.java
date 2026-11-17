package org.jmouse.cad;

import org.jspecify.annotations.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * What every document of the model says of itself: its slug, its title, the role its kind gives it ({@code type gate}),
 * and its {@code meta} fields — summary, source, category… — as written.
 *
 * @param slug  what the document is known by: its address among the library's
 * @param title what it is called, for a reader
 * @param type  the role its kind gives it — {@code type gate} —, or null where it says none
 * @param meta  its {@code meta} fields — {@code summary}, {@code source}, {@code category} … —, as written
 */
public record DocumentHead(String slug, String title, @Nullable String type, Map<String, String> meta) {

    public DocumentHead {
        meta = Map.copyOf(meta);
    }

    public Optional<String> role() {
        return Optional.ofNullable(type);
    }

    public Optional<String> field(String key) {
        return Optional.ofNullable(meta.get(key));
    }
}
