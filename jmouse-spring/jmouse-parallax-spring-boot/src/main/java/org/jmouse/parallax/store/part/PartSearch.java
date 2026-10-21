package org.jmouse.parallax.store.part;

import org.jmouse.parallax.jpa.catalogue.ObjectStore;
import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.part.FetchedPart;
import org.jmouse.parallax.part.PartCandidate;
import org.jmouse.parallax.part.PartSearchProvider;
import org.jmouse.parallax.part.ProviderState;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.writer.DocumentWriter;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.store.catalogue.ObjectDraft;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔎 Parts looked for in every provider at once, and a found one made the reader's own.
 *
 * <p>A provider that cannot answer is answered for by its state — not configured, still indexing, failed — and never
 * left out, so a reader sees which places were asked. A provider that fails while searching does not fail the others.</p>
 *
 * <p>A fetch converts first and stores after: the provider's reading — a KiCad library file, a supplier's answer — is
 * done outside the transaction that writes the documents.</p>
 */
public class PartSearch {

    /**
     * What one provider answered.
     *
     * @param provider   its code
     * @param name       its name to a reader
     * @param state      whether it could answer
     * @param candidates what it found, the best first
     */
    public record ProviderResults(String provider, String name, ProviderState state, List<PartCandidate> candidates) {
    }

    /**
     * What a fetch stored.
     *
     * @param open    the slug to open
     * @param created what was stored new, by slug
     * @param reused  what was there already under the same slug and was left as it is
     * @param notes   what the source said that the documents could not carry
     */
    public record Fetched(String open, List<String> created, List<String> reused, List<String> notes) {
    }

    private final List<PartSearchProvider> providers;
    private final Catalogue catalogue;
    private final ObjectStore objects;
    private final TransactionTemplate transactions;
    private final int limit;

    public PartSearch(List<PartSearchProvider> providers, Catalogue catalogue, ObjectStore objects,
                      TransactionTemplate transactions, int limit) {
        this.providers = List.copyOf(providers);
        this.catalogue = catalogue;
        this.objects = objects;
        this.transactions = transactions;
        this.limit = limit;
    }

    /** Every provider and whether it can answer, with nothing searched. */
    public List<ProviderResults> providers() {
        return providers.stream().map(provider -> new ProviderResults(provider.code(), provider.name(), provider.state(),
                List.of())).toList();
    }

    /**
     * @param query    what the reader typed
     * @param provider one provider's code, or null for every one
     */
    public List<ProviderResults> search(String query, String provider) {
        if (query == null || query.isBlank()) {
            throw ParallaxProblem.invalid("Say what to look for: a part number, a name or a few words.");
        }
        List<ProviderResults> answered = new ArrayList<>();
        for (PartSearchProvider candidate : chosen(provider)) {
            ProviderState state = candidate.state();
            if (!state.ready()) {
                answered.add(new ProviderResults(candidate.code(), candidate.name(), state, List.of()));
                continue;
            }
            try {
                answered.add(new ProviderResults(candidate.code(), candidate.name(), state,
                        candidate.search(query, limit)));
            } catch (RuntimeException failed) {
                answered.add(new ProviderResults(candidate.code(), candidate.name(),
                        ProviderState.failed(failed.getMessage()), List.of()));
            }
        }
        return answered;
    }

    /**
     * A candidate made into the owner's documents. A document whose slug is taken already — an earlier fetch, an
     * import, the catalogue's own — is left as it is and reused.
     */
    public Fetched fetch(ParallaxOwner owner, String provider, String identifier) {
        PartSearchProvider source = providers.stream().filter(candidate -> candidate.code().equals(provider)).findFirst()
                .orElseThrow(() -> ParallaxProblem.notFound("The part search '" + provider + "'"));
        FetchedPart made;
        try {
            made = source.fetch(identifier);
        } catch (UnsupportedOperationException refused) {
            throw ParallaxProblem.conflict(refused.getMessage());
        } catch (IllegalArgumentException unknown) {
            throw ParallaxProblem.invalid(unknown.getMessage());
        }
        return transactions.execute(status -> store(owner, made));
    }

    private Fetched store(ParallaxOwner owner, FetchedPart made) {
        List<String> created = new ArrayList<>();
        List<String> reused = new ArrayList<>();
        for (ExperienceDocument document : made.documents()) {
            if (objects.findBySlug(document.slug()).isPresent()) {
                reused.add(document.slug());
                continue;
            }
            catalogue.createOwned(owner, draft(DocumentWriter.write(document)));
            created.add(document.slug());
        }
        return new Fetched(made.open(), created, reused, made.notes());
    }

    private List<PartSearchProvider> chosen(String provider) {
        if (provider == null || provider.isBlank()) {
            return providers;
        }
        List<PartSearchProvider> named = providers.stream().filter(candidate -> candidate.code().equals(provider)).toList();
        if (named.isEmpty()) {
            throw ParallaxProblem.notFound("The part search '" + provider + "'");
        }
        return named;
    }

    private static ObjectDraft draft(String source) {
        return new ObjectDraft(source, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
