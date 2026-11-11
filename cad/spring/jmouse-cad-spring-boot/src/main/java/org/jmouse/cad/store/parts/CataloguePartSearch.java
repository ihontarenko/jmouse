package org.jmouse.cad.store.parts;

import org.jmouse.cad.board.BoardModule;
import org.jmouse.cad.document.part.PartCandidate;
import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.document.part.ProviderState;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentSummary;

import java.util.List;
import java.util.Map;

/**
 * 🔎 The catalogue as a place to find parts: its published documents, through the catalogue's own search — the one the
 * library's browse asks. What it finds is ours already, so there is nothing to fetch: a candidate names its slug.
 */
public final class CataloguePartSearch implements PartSearchProvider {

    /** What each kind of document is to a reader looking for parts. */
    private static final Map<String, PartCandidate.Kind> KINDS = Map.of(
            SimulationModule.COMPONENT.word(), PartCandidate.Kind.COMPONENT,
            SchematicModule.SYMBOL.word(), PartCandidate.Kind.SYMBOL,
            BoardModule.FOOTPRINT.word(), PartCandidate.Kind.FOOTPRINT,
            SimulationModule.CIRCUIT.word(), PartCandidate.Kind.CIRCUIT);

    private final CadCatalogue catalogue;

    public CataloguePartSearch(CadCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public String id() {
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
        return catalogue.browse(query, null, null, null, null, false, 0, limit).rows().stream().map(this::candidateOf)
                .toList();
    }

    private PartCandidate candidateOf(DocumentSummary summary) {
        return new PartCandidate(id(), summary.slug(), KINDS.getOrDefault(summary.kind(), PartCandidate.Kind.OTHER),
                                 summary.title(), null, null, summary.summary(), null, null, null, summary.slug(), false);
    }
}
