package org.jmouse.cad.store.parts;

import org.jmouse.cad.folder.RevisionOrigin;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.document.source.DocumentLocation;
import org.jmouse.cad.store.catalogue.CatalogueViews.LocationView;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.jmouse.cad.store.catalogue.ReadDocument;
import org.jmouse.cad.store.library.PartLibrary.DrawnDocument;
import org.jmouse.cad.document.part.FetchCompletion;
import org.jmouse.cad.document.part.FetchedFile;
import org.jmouse.cad.document.part.FetchedPart;
import org.jmouse.cad.document.part.PartCandidate;
import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.document.part.ProviderState;
import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentDetail;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.catalogue.StoredDocuments;
import org.jmouse.cad.store.files.DocumentFiles;
import org.jmouse.cad.store.plugin.EngineConfiguration;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.plugin.Extensions;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * 🔎 Parts looked for beyond the library, in every provider at once, and a found one made an owner's documents —
 * {@code JMOUSE_CAD_API.md} §2.8.
 *
 * <p>Every provider the plugins brought ({@link PartSearchProvider#POINT}) — KiCad's and LibrePCB's installed libraries, a
 * supplier's search — is asked, read anew on each call. The library itself is no provider: what it holds is its own listing,
 * which the same search narrows beside these.</p>
 *
 * <p>A provider that cannot answer is answered for by its state — not configured, still indexing, failed — and never
 * left out, so a reader sees which places were asked. A provider that fails while searching does not fail the others.</p>
 *
 * <p>A fetch converts first — the provider, then every completion a plugin brought ({@link FetchCompletion#POINT}) —
 * and stores after: the provider's reading — a KiCad library file, a supplier's answer — is
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
     * @param reused  what was there already under the same slug: its text left as it is, a file it lacked added
     * @param notes   what the source said that the documents could not carry
     */
    public record Fetched(String open, List<String> created, List<String> reused, List<String> notes) {
    }

    /**
     * A part seen before it is kept: what a fetch would store, read as the library reads its documents, nothing written.
     *
     * @param open      the slug a fetch would open
     * @param documents each document it would store, by its kind, drawn as a shelf draws it — held nowhere: read in place from
     *                  its provider
     * @param notes     what the source said that the documents could not carry
     */
    public record Previewed(String open, List<PreviewedDocument> documents, List<String> notes) {
    }

    /** One document of a part seen before it is kept: its kind — the shelf that would hold it — and the document as drawn. */
    public record PreviewedDocument(String kind, DrawnDocument drawn) {
    }

    private final Extensions               extensions;
    private final CadCatalogue             catalogue;
    private final DocumentStore            documents;
    private final CadAuthority             authority;
    private final CadPlaces                places;
    private final TransactionTemplate      transactions;
    private final EngineConfiguration      configuration;
    private final CadLanguage              language;
    private final DocumentFiles            files;
    private final DocumentAccess           access;
    private final DocumentReading          reading;

    public PartSearch(Extensions extensions, CadCatalogue catalogue,
                      DocumentStore documents, CadAuthority authority, CadPlaces places, TransactionTemplate transactions,
                      EngineConfiguration configuration, CadLanguage language, DocumentFiles files, DocumentAccess access,
                      DocumentReading reading) {
        this.reading = reading;
        this.language = language;
        this.files = files;
        this.access = access;
        this.extensions = extensions;
        this.catalogue = catalogue;
        this.documents = documents;
        this.authority = authority;
        this.places = places;
        this.transactions = transactions;
        this.configuration = configuration;
    }

    /** Every provider and whether it can answer, with nothing searched. */
    public List<ProviderResults> providers() {
        return everyProvider().stream().map(provider -> new ProviderResults(provider.id(), provider.name(), provider.state(),
                List.of())).toList();
    }

    /** Every provider made ready in the background ({@link PartSearchProvider#prepare()}): what the engine asks as it comes up. */
    public void prepareProviders() {
        chosen(null).forEach(PartSearchProvider::prepare);
    }

    /**
     * @param query    what the reader typed
     * @param provider one provider's code, or null for every one
     * @param kinds    the kinds of part looked for — a shelf's —; empty for every kind. A provider offering none of them is not asked.
     */
    public List<ProviderResults> search(String query, String provider, Set<PartCandidate.Kind> kinds) {
        if (query == null || query.isBlank()) {
            throw CadProblem.invalid("Say what to look for: a part number, a name or a few words.");
        }
        List<ProviderResults> answered = new ArrayList<>();
        for (PartSearchProvider candidate : chosen(provider)) {
            Set<PartCandidate.Kind> asked = kinds.isEmpty() ? candidate.offers() : intersection(kinds, candidate.offers());
            if (asked.isEmpty()) {
                continue;
            }
            ProviderState state = candidate.state();
            if (!state.ready()) {
                answered.add(new ProviderResults(candidate.id(), candidate.name(), state, List.of()));
                continue;
            }
            try {
                answered.add(new ProviderResults(candidate.id(), candidate.name(), state, candidate.search(query, asked, configuration.partLimit())));
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
        FetchedPart completed = converted(provider, identifier);
        return transactions.execute(status -> store(owner, completed));
    }

    /**
     * A candidate as a fetch would make it, read and drawn, nothing stored: what a reader looks at before keeping it. Each document
     * is read as the library reads one; a document that does not read is shown without its drawing, never dropped.
     */
    public Previewed preview(String provider, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw CadProblem.invalid("Name the part to preview as 'identifier': what the search answered it by.");
        }
        FetchedPart made = converted(provider, identifier);
        LocationView place = LocationView.of(new DocumentLocation.Foreign(provider, identifier.strip(), null));
        List<PreviewedDocument> seen = made.documents().stream().map(document -> previewed(document, place)).toList();
        return new Previewed(made.open(), seen, made.notes());
    }

    private PreviewedDocument previewed(DocumentNode document, LocationView place) {
        String source = language.language().write(document);
        try {
            ReadDocument read = reading.read(source, document.slug());
            Optional<MetaNode> meta = read.document().block(MetaNode.class);
            return new PreviewedDocument(read.index().kind(), new DrawnDocument(read.index().slug(), read.index().title(),
                    read.index().summary(), place, null, null, read.index().code(), meta.map(Provenance::of).orElse(Provenance.OURS),
                    read.model().orElse(null)));
        } catch (CadProblem unreadable) {
            return new PreviewedDocument(document.kind().word(), new DrawnDocument(document.slug(), document.title(), null, place, null,
                    null, null, new Provenance(null, null, null), null));
        }
    }

    /** A candidate converted by its provider, then completed by every plugin that completes one: what a fetch stores. */
    private FetchedPart converted(String provider, String identifier) {
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
        for (FetchCompletion completion : extensions.contributions(FetchCompletion.POINT)) {
            made = completion.completed(made);
        }
        return made;
    }

    /**
     * The documents, then their files: every file of a document stored new, and of a reused one the reader may write,
     * the files it does not hold yet — an earlier fetch where no files were kept is completed, never written over. Where
     * this installation keeps no files, a note says what was not kept. A document held where the owner cannot see it —
     * by another project — is refused rather than reused: what the owner would place could not be found.
     */
    private Fetched store(DocumentOwner owner, FetchedPart made) {
        List<String> created = new ArrayList<>();
        List<String> reused = new ArrayList<>();
        Map<String, String> filed = new HashMap<>();
        StoredDocuments scope = catalogue.scope(owner);
        for (DocumentNode document : made.documents()) {
            Optional<StoredDocument> taken = documents.findBySlug(document.slug());
            if (taken.isPresent()) {
                StoredDocument held = taken.get();
                if (!scope.visible(held)) {
                    throw CadProblem.conflict("'" + document.slug() + "' is already held by another owner this one cannot"
                            + " see; fetch the part into the workspace to share it between its projects.");
                }
                if (access.canWrite(held)) {
                    filed.put(document.slug(), held.getId());
                }
                reused.add(document.slug());
                continue;
            }
            DocumentDetail detail = catalogue.createOwned(owner, language.language().write(document), RevisionOrigin.IMPORTED);
            filed.put(document.slug(), detail.summary().id());
            created.add(document.slug());
        }
        List<String> notes = new ArrayList<>(made.notes());
        new FetchedFilesKeeper(files).keep(made.files(), filed).ifPresent(notes::add);
        return new Fetched(made.open(), created, reused, notes);
    }

    private boolean holds(String documentId, String name) {
        return files.of(documentId).stream().anyMatch(file -> name.equals(file.name()));
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

    private static Set<PartCandidate.Kind> intersection(Set<PartCandidate.Kind> asked, Set<PartCandidate.Kind> offered) {
        Set<PartCandidate.Kind> both = EnumSet.noneOf(PartCandidate.Kind.class);
        both.addAll(asked);
        both.retainAll(offered);
        return both;
    }

    /** Every provider a plugin brought. */
    private List<PartSearchProvider> everyProvider() {
        return extensions.contributions(PartSearchProvider.POINT);
    }
}
