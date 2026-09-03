package org.jmouse.search;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🔎 One thing a search found, what kind of thing it is, and why it is in the answer.
 *
 * <h3>⚠️ It carries the {@link Relevance}, not a score</h3>
 *
 * <p>The reckoning travels with the hit so that an interface can explain the match without asking
 * anything a second time. {@link #score()} reads through to it, so ordinary ranking code never has to
 * know the difference — but the rows are there when a screen wants to say <em>why</em>.</p>
 *
 * <h3>⚠️ {@code type} is a word, not a class</h3>
 *
 * <p>A federated result is drawn by an interface that has no classes, so the kind of thing a hit is has
 * to survive as text: {@code "page"}, {@code "issue"}, {@code "movie"}. It is also what a type filter
 * sends back, which is why {@link SearchType} publishes the same word beside a label and an icon.</p>
 *
 * <h3>⚠️ {@code metadata} is for what the caller must not have to look up again</h3>
 *
 * <p>A workspace, a project key, a poster's identifier — whatever an interface needs to draw the row or
 * to build a link. It is deliberately {@code Map<String, String>}: a payload that can hold anything is a
 * payload nobody can render generically, and this one is read by code that knows nothing about the
 * product it came from.
 *
 * @param snippets the passages that matched, when the provider extracted any. ⚠️ Plain text, without
 *                 highlight markup — the same strings are read by a person, a screen and a model, and
 *                 only one of those wants markup. Marking the terms is the caller's two lines.
 */
public record SearchHit(
        String              type,
        String              id,
        String              title,
        String              subtitle,
        Relevance           relevance,
        List<String>        snippets,
        Map<String, String> metadata
) {

    public SearchHit {
        snippets = snippets == null ? List.of() : List.copyOf(snippets);
        metadata = metadata == null ? Map.of()  : Map.copyOf(metadata);
    }

    public static Builder of(String type, String id) {
        return new Builder(type, id);
    }

    public double score() {
        return relevance == null ? 0.0 : relevance.score();
    }

    /** Why this is here, in one line. Empty when the provider brought no reckoning. */
    public String explain() {
        return relevance == null ? "" : relevance.explain();
    }

    public static final class Builder {

        private final String type;
        private final String id;

        private String              title;
        private String              subtitle;
        private Relevance           relevance;
        private List<String>        snippets = List.of();
        private Map<String, String> metadata = new LinkedHashMap<>();

        private Builder(String type, String id) {
            this.type = type;
            this.id   = id;
        }

        public Builder title(String title) {
            this.title = title;

            return this;
        }

        public Builder subtitle(String subtitle) {
            this.subtitle = subtitle;

            return this;
        }

        public Builder relevance(Relevance relevance) {
            this.relevance = relevance;

            return this;
        }

        public Builder snippets(List<String> snippets) {
            this.snippets = snippets;

            return this;
        }

        public Builder meta(String key, String value) {
            if (value != null) {
                metadata.put(key, value);
            }

            return this;
        }

        public SearchHit build() {
            return new SearchHit(type, id, title, subtitle, relevance, snippets, metadata);
        }
    }

}
