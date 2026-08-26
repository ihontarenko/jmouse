package org.jmouse.query.translate.row;

import org.jmouse.query.el.node.QueryDocumentNode;
import org.jmouse.query.el.node.SourceNode;
import org.jmouse.query.el.node.ViewNode;
import org.jmouse.query.schema.QuerySchema;
import org.jmouse.el.translate.Bindings;
import org.jmouse.query.translate.SourceBinding;
import org.jmouse.el.translate.TranslationRefusedException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The shapes a row backend knows about, by name — so a late-bound view resolves here exactly as it does
 * against a database.
 *
 * <h2>⚠️ Why the row side needs a registry at all</h2>
 *
 * <p>{@code from: $source} is only worth having if BOTH destinations can answer it. A late binding that
 * resolved against a database and had to be pinned by hand for a list of maps would leave the two paths
 * carrying different text — which is the one thing the whole design exists to prevent, and it would show
 * up as a test passing against rows and a screen failing against SQL.</p>
 *
 * <p>So the rule is the same rule, in the same place: {@link SourceBinding} decides, and what a caller
 * binds can only ever CHOOSE among the names registered here.</p>
 *
 * <h2>⚠️ A schema and a mapping are both accepted, and they are not the same</h2>
 *
 * <p>A bare {@link QuerySchema} says the rows are already keyed the way a query writes them. A
 * {@link SourceNode} — a structure bound to a place — additionally says where each attribute is read from
 * in a raw row, which is what a file needs. Registering a document gives the second for every mapping it
 * declares.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class RowSources {

    private final Map<String, QuerySchema> schemas;
    private final Map<String, SourceNode>  mappings;
    private final Set<String>              views;

    private RowSources(Map<String, QuerySchema> schemas, Map<String, SourceNode> mappings,
                       Set<String> views) {
        this.schemas = Map.copyOf(schemas);
        this.mappings = Map.copyOf(mappings);
        this.views = Set.copyOf(views);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** One shape, for a caller with nothing to choose between. */
    public static RowSources of(String name, QuerySchema schema) {
        return builder().source(name, schema).build();
    }

    /**
     * Every mapping a document declares, ready to be chosen between.
     *
     * <p>⚠️ Registered under the mapping's qualified name — {@code request}, {@code request:export} — so a
     * late-bound view resolves here exactly as it does against a database, and the two paths carry one
     * text rather than two.</p>
     */
    public static RowSources of(QueryDocumentNode document) {
        Builder builder = builder();

        for (SourceNode source : document.getSources()) {
            builder.source(source);
        }

        document.getViews().forEach(view -> view.getIdentifier().ifPresent(builder::view));

        return builder.build();
    }

    /**
     * The translator for one view — resolving {@code from: $source} against what the caller supplies.
     *
     * @param view     a parsed view, pinned or late-bound
     * @param bindings what the caller supplies by name
     * @return a translator over whatever it turned out to be about
     */
    public RowTranslator translator(ViewNode view, Bindings bindings) {
        String        name       = SourceBinding.resolve(view, bindings, declared());
        SourceNode    mapping    = mappings.get(name);
        RowTranslator translator = mapping == null
                ? new RowTranslator(requireSchema(name))
                : new RowTranslator(mapping);

        translator.subqueries(views);

        return translator;
    }

    /** The names anything late-bound may resolve to. */
    public Set<String> declared() {
        Set<String> names = new LinkedHashSet<>(mappings.keySet());

        names.addAll(schemas.keySet());

        return names;
    }

    private QuerySchema requireSchema(String name) {
        QuerySchema schema = schemas.get(name);

        if (schema == null) {
            throw new TranslationRefusedException(
                    "there is nothing called '%s' to read; this registry has %s".formatted(
                            name, declared().isEmpty() ? "nothing at all" : String.join(", ", declared())));
        }

        return schema;
    }

    public static final class Builder {

        private final Map<String, QuerySchema> schemas  = new LinkedHashMap<>();
        private final Map<String, SourceNode>  mappings = new LinkedHashMap<>();
        private final Set<String>              views    = new LinkedHashSet<>();

        /** A name that stands in for a set — held so a refusal can say why, not merely that. */
        public Builder view(String identifier) {
            views.add(identifier);

            return this;
        }

        /** Rows already keyed the way a query writes them. */
        public Builder source(String name, QuerySchema schema) {
            schemas.put(name, schema);

            return this;
        }

        /** A structure bound to a place — the mapping says where each attribute is read from. */
        public Builder source(SourceNode source) {
            mappings.put(source.getName(), source);

            return this;
        }

        public RowSources build() {
            return new RowSources(schemas, mappings, views);
        }
    }
}
