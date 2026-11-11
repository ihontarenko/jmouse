package org.jmouse.cad;

import java.util.Map;
import java.util.Optional;

/**
 * What every document of the model says of itself: its slug, its title, the role its kind gives it ({@code type gate}),
 * and its {@code meta} fields — summary, source, category… — as written.
 */
public record DocumentHead(String slug, String title, String type, Map<String, String> meta) {

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
