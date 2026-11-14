package org.jmouse.cad.document.part;

import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Identified;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 🔎 One place parts are looked for beyond the library: the KiCad or LibrePCB libraries installed on the server, a supplier's search.
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

    /**
     * Makes ready, in the background, what the provider needs time for before it can answer — an installed library read and
     * indexed —; asked once as the engine comes up, so the first search after a start is answered rather than told to wait.
     * Never blocks; a provider ready at once has nothing to do.
     */
    default void prepare() {
    }

    /** Whether it can answer now. */
    ProviderState state();

    /** The kinds of part it finds: what a search asking for other kinds does not ask it at all. */
    Set<PartCandidate.Kind> offers();

    /**
     * The parts of the kinds asked that match every word of the query, the best first.
     *
     * @param query what the reader typed
     * @param kinds the kinds asked, among those it {@linkplain #offers() offers}: the limit is spent on them alone
     * @param limit how many at most
     * @return nothing when the provider is not {@link ProviderState.Status#READY}
     */
    List<PartCandidate> search(String query, Set<PartCandidate.Kind> kinds, int limit);

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

    /**
     * A provider that is whichever {@code current} answers at each question: one made anew when what it is made of changes — the
     * folders a setting names —, while the point it is contributed to keeps this one.
     */
    static PartSearchProvider following(String id, String name, Supplier<? extends PartSearchProvider> current) {
        return new PartSearchProvider() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public void prepare() {
                current.get().prepare();
            }

            @Override
            public ProviderState state() {
                return current.get().state();
            }

            @Override
            public Set<PartCandidate.Kind> offers() {
                return current.get().offers();
            }

            @Override
            public List<PartCandidate> search(String query, Set<PartCandidate.Kind> kinds, int limit) {
                return current.get().search(query, kinds, limit);
            }

            @Override
            public FetchedPart fetch(String identifier) {
                return current.get().fetch(identifier);
            }
        };
    }
}
