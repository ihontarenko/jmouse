package org.jmouse.parallax.store.part;

import org.jmouse.parallax.part.PartCandidate;
import org.jmouse.parallax.part.PartSearchProvider;
import org.jmouse.parallax.part.ProviderState;
import org.jmouse.parallax.object.ObjectKind;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.store.catalogue.CatalogueViews.ObjectSummary;

import java.util.List;

/**
 * 🔎 The catalogue as a place to find parts: its published objects, through the catalogue's own search — the one the
 * Library's browse asks. What it finds is ours already, so there is nothing to fetch: a candidate names its slug.
 */
public final class CataloguePartSearch implements PartSearchProvider {

    private final Catalogue catalogue;

    public CataloguePartSearch(Catalogue catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public String code() {
        return "catalogue";
    }

    @Override
    public String name() {
        return "Catalogue";
    }

    @Override
    public ProviderState state() {
        return ProviderState.ready("The published catalogue.");
    }

    @Override
    public List<PartCandidate> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return catalogue.browse(query, null, null, null, null, null, false, 0, limit).items().stream()
                .map(this::candidateOf).toList();
    }

    private PartCandidate candidateOf(ObjectSummary summary) {
        return new PartCandidate(code(), summary.slug(), kindOf(summary.kind()), summary.title(), null, null,
                summary.summary(), null, null, null, summary.slug(), false);
    }

    private static PartCandidate.Kind kindOf(ObjectKind kind) {
        return switch (kind) {
            case COMPONENT -> PartCandidate.Kind.COMPONENT;
            case SYMBOL -> PartCandidate.Kind.SYMBOL;
            case FOOTPRINT -> PartCandidate.Kind.FOOTPRINT;
            case CIRCUIT -> PartCandidate.Kind.CIRCUIT;
            default -> PartCandidate.Kind.OTHER;
        };
    }
}
