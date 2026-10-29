package org.jmouse.cad.document.part;

import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Identified;

import java.util.List;

/**
 * 🔎 One place parts are looked for: the catalogue, the KiCad libraries installed on the server, a supplier's search.
 *
 * <p>A provider finds; what it found becomes the reader's own documents only through {@link #fetch} — a component, its
 * symbol, its footprint written as jMX into the reader's place — and only when the source's terms let it be kept. A
 * provider that may show what it finds but not keep it says so with {@link PartCandidate#fetchable()} false.</p>
 *
 * <p>⚠️ A provider that cannot answer — no key configured, its libraries still being read — says so in {@link #state()}
 * and is listed anyway: a provider hidden because it is not configured is a feature nobody can find.</p>
 */
public interface PartSearchProvider extends Identified {

    /** Where the places beyond the catalogue are brought: KiCad's installed libraries, a supplier's search. */
    Contributions<PartSearchProvider> POINT = new Contributions<>("cad.parts.providers", PartSearchProvider.class);

    /** How the provider is named in an address: {@code catalogue}, {@code kicad}, {@code nexar}. */
    @Override
    String id();

    /** How it is named to a reader. */
    String name();

    /** Whether it can answer now. */
    ProviderState state();

    /**
     * The parts that match every word of the query, the best first.
     *
     * @param query what the reader typed
     * @param limit how many at most
     * @return nothing when the provider is not {@link ProviderState.Status#READY}
     */
    List<PartCandidate> search(String query, int limit);

    /**
     * A candidate this provider found, made into documents the reader may keep.
     *
     * @param identifier the candidate's {@link PartCandidate#identifier()}
     * @throws UnsupportedOperationException when the provider keeps nothing — its terms allow showing, not keeping
     * @throws IllegalArgumentException      when it holds no such candidate
     */
    default FetchedPart fetch(String identifier) {
        throw new UnsupportedOperationException(name() + " shows parts but does not hand them over to be kept.");
    }
}
