package org.jmouse.cad.store.parts;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.part.FetchedPart;
import org.jmouse.cad.document.part.PartCandidate;
import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.document.part.ProviderState;
import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.jpa.catalogue.DocumentStore;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.plugin.Extensions;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔎 Parts looked for in every provider at once, and a found one made an owner's documents — {@code JMOUSE_CAD_API.md}
 * §2.7.
 *
 * <p>The catalogue is asked first, then every provider the plugins brought ({@link PartSearchProvider#POINT}) — KiCad's
 * installed libraries, a supplier's search —, read anew on each call.</p>
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

    private final CataloguePartSearch      catalogueSearch;
    private final Extensions               extensions;
    private final CadCatalogue             catalogue;
    private final DocumentStore            documents;
    private final CadAuthority             authority;
    private final CadPlaces                places;
    private final TransactionTemplate      transactions;
    private final int                      limit;
    private final CadLanguage              language;

    public PartSearch(CataloguePartSearch catalogueSearch, Extensions extensions, CadCatalogue catalogue,
                      DocumentStore documents, CadAuthority authority, CadPlaces places, TransactionTemplate transactions,
                      int limit, CadLanguage language) {
        this.language = language;
        this.catalogueSearch = catalogueSearch;
        this.extensions = extensions;
        this.catalogue = catalogue;
        this.documents = documents;
        this.authority = authority;
        this.places = places;
        this.transactions = transactions;
        this.limit = limit;
    }

    /** Every provider and whether it can answer, with nothing searched. */
    public List<ProviderResults> providers() {
        return everyProvider().stream().map(provider -> new ProviderResults(provider.id(), provider.name(), provider.state(),
                List.of())).toList();
    }

    /**
     * @param query    what the reader typed
     * @param provider one provider's code, or null for every one
     */
    public List<ProviderResults> search(String query, String provider) {
        if (query == null || query.isBlank()) {
            throw CadProblem.invalid("Say what to look for: a part number, a name or a few words.");
        }
        List<ProviderResults> answered = new ArrayList<>();
        for (PartSearchProvider candidate : chosen(provider)) {
            ProviderState state = candidate.state();
            if (!state.ready()) {
                answered.add(new ProviderResults(candidate.id(), candidate.name(), state, List.of()));
                continue;
            }
            try {
                answered.add(new ProviderResults(candidate.id(), candidate.name(), state, candidate.search(query, limit)));
            } catch (RuntimeException failed) {
                answered.add(new ProviderResults(candidate.id(), candidate.name(), ProviderState.failed(failed.getMessage()),
                        List.of()));
            }
        }
        return answered;
    }

    /**
     * A candidate made into the owner's documents. A document whose slug is taken already — an earlier fetch, an import,
     * the catalogue's own — is left as it is and reused.
     *
     * @param ownerAddress the owner, as {@code TYPE:id}
     */
    public Fetched fetch(String ownerAddress, String provider, String identifier) {
        DocumentOwner owner = DocumentOwner.parse(ownerAddress)
                .orElseThrow(() -> CadProblem.invalid("An owner is written TYPE:id — PROJECT:42."));
        authority.require(CadCapability.OWNER_WRITE, places.requireOwner(owner));
        if (identifier == null || identifier.isBlank()) {
            throw CadProblem.invalid("Name the part to fetch as 'identifier': what the search answered it by.");
        }
        PartSearchProvider source = chosen(provider).getFirst();
        FetchedPart made;
        try {
            made = source.fetch(identifier.strip());
        } catch (UnsupportedOperationException refused) {
            throw CadProblem.conflict(refused.getMessage());
        } catch (IllegalArgumentException unknown) {
            throw CadProblem.invalid(unknown.getMessage());
        } catch (IllegalStateException failed) {
            throw new CadProblem.Unprocessable("fetch-failed", failed.getMessage());
        }
        return transactions.execute(status -> store(owner, made));
    }

    private Fetched store(DocumentOwner owner, FetchedPart made) {
        List<String> created = new ArrayList<>();
        List<String> reused = new ArrayList<>();
        for (DocumentNode document : made.documents()) {
            if (documents.findBySlug(document.slug()).isPresent()) {
                reused.add(document.slug());
                continue;
            }
            catalogue.createOwned(owner, language.language().write(document));
            created.add(document.slug());
        }
        return new Fetched(made.open(), created, reused, made.notes());
    }

    private List<PartSearchProvider> chosen(String provider) {
        if (provider == null || provider.isBlank()) {
            return everyProvider();
        }
        List<PartSearchProvider> named = everyProvider().stream().filter(candidate -> candidate.id().equals(provider.strip()))
                .toList();
        if (named.isEmpty()) {
            throw CadProblem.notFound("The part search '" + provider + "'");
        }
        return named;
    }

    /** The catalogue first, then every provider a plugin brought. */
    private List<PartSearchProvider> everyProvider() {
        List<PartSearchProvider> every = new ArrayList<>();
        every.add(catalogueSearch);
        every.addAll(extensions.contributions(PartSearchProvider.POINT));
        return every;
    }
}
