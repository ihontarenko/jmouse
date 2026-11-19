package org.jmouse.cad.store.project;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.DocumentParser;
import org.jmouse.cad.document.DocumentStatus;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.simulation.ProjectDocument;
import org.jmouse.cad.simulation.ProjectPurpose;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.store.CadProperties;
import org.jmouse.cad.store.CadRead;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentSummary;
import org.jmouse.cad.store.catalogue.CatalogueViews.SheetView;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.jmouse.cad.store.library.PartLibrary;
import org.jmouse.cad.store.library.PartLibrary.DrawnDocument;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.core.io.PortablePaths;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The engine's projects: each one a {@code project} document ({@link ProjectDocument}), kept like every document. A reader's
 * projects are kept by their own place ({@link CadAuthority#actorOwner()}); a library ships its own, read only. Whether a
 * project is shipped or kept is where its document is held, never a word of it; what it holds is the documents owned by it —
 * or, for a shipped one, those shipped beside it, in its folder.
 */
public class CadProjects {

    private static final String PROJECT = SimulationModule.PROJECT.word();
    private static final String CIRCUIT = SimulationModule.CIRCUIT.word();

    private final DocumentStore     documents;
    private final CadCatalogue      catalogue;
    private final DocumentAccess    access;
    private final DocumentReading   reading;
    private final CadAuthority      authority;
    private final CadPlaces         places;
    private final CadProperties     properties;
    private final PartLibrary       library;

    public CadProjects(DocumentStore documents, CadCatalogue catalogue, DocumentAccess access, DocumentReading reading,
                       CadAuthority authority, CadPlaces places, CadProperties properties, PartLibrary library) {
        this.documents = documents;
        this.catalogue = catalogue;
        this.access = access;
        this.reading = reading;
        this.authority = authority;
        this.places = places;
        this.properties = properties;
        this.library = library;
    }

    /**
     * The reader's projects, then those the libraries ship, each as a list shows it; an archived one only when asked for. A
     * reader with no place of their own sees the shipped ones alone.
     */
    @CadRead
    public List<ProjectView> list(boolean includeArchived) {
        List<StoredDocument> own = authority.actorOwner().map(this::ownedProjects).orElse(List.of());
        List<StoredDocument> shipped = documents.findShipped().stream().filter(CadProjects::isProject)
                .sorted(Comparator.comparing(StoredDocument::getTitle)).toList();
        return Stream.concat(own.stream(), shipped.stream())
                .filter(project -> includeArchived || project.getStatus() != DocumentStatus.ARCHIVED)
                .filter(access::canRead).map(this::view).toList();
    }

    /** One project by its address. */
    @CadRead
    public ProjectView project(String slug) {
        return view(requireProject(slug));
    }

    /** The reader's Drafts — the place their loose work is kept —, made on the first question. */
    @Transactional
    public ProjectView drafts() {
        DocumentOwner owner = requireActorOwner();
        return ownedProjects(owner).stream()
                .filter(project -> projectOf(project).purpose() == ProjectPurpose.DRAFTS)
                .findFirst().map(this::view)
                .orElseGet(() -> create(owner, properties.projects().draftsTitle(), null, ProjectPurpose.DRAFTS));
    }

    /** A new design of the reader's, its address made from its title — the first free one. */
    @Transactional
    public ProjectView create(String title, String summary) {
        return create(requireActorOwner(), requireTitle(title), summary, ProjectPurpose.DESIGN);
    }

    /** A project called and described anew; refused when it changed since {@code version} was read. */
    @Transactional
    public ProjectView describe(String slug, String title, String summary, long version) {
        StoredDocument project = requireProject(slug);
        return rewrite(project, projectOf(project).described(requireTitle(title), summary), version);
    }

    /**
     * A project archived — kept, listed no more, taking nothing new — or brought back. A reader's Drafts is never archived: it is
     * where their loose work always has a place.
     */
    @Transactional
    public ProjectView archive(String slug, boolean archived) {
        StoredDocument project = requireProject(slug);
        ProjectDocument read = projectOf(project);
        if (archived && read.purpose() == ProjectPurpose.DRAFTS) {
            throw CadProblem.conflict("Drafts is where loose work is kept; it is never archived");
        }
        return rewrite(project, read.archived(archived), project.getVersion());
    }

    /**
     * What a project holds, of one kind or of every kind, each as a library shelf lists it: the documents owned by a kept
     * project, those shipped beside a shipped one.
     *
     * @param kind the kind's word — {@code circuit}, {@code pcb} —; null for every kind
     */
    @CadRead
    public List<DrawnDocument> documents(String slug, String kind) {
        return held(requireProject(slug)).filter(document -> kind == null || kind.equals(document.getKind()))
                .filter(access::canRead)
                .sorted(Comparator.comparing(StoredDocument::getTitle, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(library::drawn).toList();
    }

    /**
     * The project that holds a document: the one it is owned by, or the shipped one it ships beside. Refused for a document no
     * project holds — a part of a library.
     */
    @CadRead
    public ProjectView holding(String documentSlug) {
        StoredDocument document = access.requireReadable(documentSlug);
        Optional<StoredDocument> project = document.owner().filter(CadPlaces::isProject).map(owner -> requireProject(owner.id()))
                .or(() -> document.isShipped()
                        ? documents.findShipped().stream().filter(CadProjects::isProject).filter(shipped -> shippedBeside(shipped, document)).findFirst()
                        : Optional.empty());
        return project.map(this::view).orElseThrow(() -> CadProblem.notFound("A project holding '" + documentSlug + "'"));
    }

    /** A project's circuits as the sheets of its design — their ports, global nets and blocks —, kept or shipped alike. */
    @CadRead
    public List<SheetView> sheets(String slug) {
        StoredDocument project = requireProject(slug);
        List<StoredDocument> circuits = held(project).filter(document -> CIRCUIT.equals(document.getKind())).filter(access::canRead).toList();
        return catalogue.sheetsOf(circuits, project.isShipped() ? null : CadPlaces.projectOwner(slug));
    }

    private ProjectView create(DocumentOwner owner, String title, String summary, ProjectPurpose purpose) {
        ProjectDocument project = new ProjectDocument(freeSlugOf(title), title, summary, purpose, DocumentStatus.PUBLISHED);
        catalogue.createOwned(owner, written(project));
        return view(documents.find(project.slug()).orElseThrow());
    }

    private ProjectView rewrite(StoredDocument stored, ProjectDocument project, long version) {
        catalogue.update(stored.getId(), written(project), version);
        return view(documents.find(stored.getSlug()).orElseThrow());
    }

    /** What a project holds: the documents owned by a kept one, those shipped beside a shipped one. */
    private Stream<StoredDocument> held(StoredDocument project) {
        return project.isShipped()
                ? documents.findShipped().stream().filter(document -> shippedBeside(project, document))
                : documents.owned(CadPlaces.projectOwner(project.getSlug())).stream();
    }

    /** A project as a list shows it: its document, what it is for, whether the reader may change it, when it last changed. */
    private ProjectView view(StoredDocument project) {
        ProjectDocument read = projectOf(project);
        return new ProjectView(DocumentSummary.of(project), read.purpose(), access.canWrite(project), project.getVersion(),
                               lastChange(project));
    }

    /** When anything of a project last changed: its own document, or any it holds. */
    private Instant lastChange(StoredDocument project) {
        if (project.isShipped()) {
            return project.getUpdatedAt();
        }
        return documents.owned(CadPlaces.projectOwner(project.getSlug())).stream().map(StoredDocument::getUpdatedAt)
                .filter(held -> held.isAfter(project.getUpdatedAt())).findFirst().orElse(project.getUpdatedAt());
    }

    private List<StoredDocument> ownedProjects(DocumentOwner owner) {
        authority.require(CadCapability.OWNER_READ, places.requireOwner(owner));
        return documents.owned(owner, PROJECT);
    }

    /**
     * The folder a project's documents stand in on the server's disk, for whoever may read the project: where a plugin that keeps
     * history — Git — keeps the project's. Empty for a project whose file is no file of the library folder (a shipped one).
     */
    @CadRead
    public Optional<Path> folderForReading(String slug) {
        return folderOf(requireProject(slug));
    }

    /** The same folder, for whoever may change the project; refused for whoever may only read it. */
    @CadRead
    public Optional<Path> folderForChanging(String slug) {
        StoredDocument project = requireProject(slug);
        access.requireWritable(project);
        return folderOf(project);
    }

    private Optional<Path> folderOf(StoredDocument project) {
        return documents.pathInFolder(project.getId())
                .map(file -> documents.folder().resolve(file).getParent());
    }

    private StoredDocument requireProject(String slug) {
        StoredDocument project = access.requireReadable(slug);
        if (!isProject(project)) {
            throw CadProblem.notFound("The project '" + slug + "'");
        }
        return project;
    }

    private ProjectDocument projectOf(StoredDocument project) {
        return ProjectDocument.of(catalogue.scope(project.owner().orElse(null)).head(project).document());
    }

    private DocumentOwner requireActorOwner() {
        authority.requireActor();
        return authority.actorOwner().orElseThrow(() -> CadProblem.conflict("You have no place of your own here to keep a project in."));
    }

    private String written(ProjectDocument project) {
        return reading.language().language().write(project.document());
    }

    /** The first address the title gives that no document holds: {@code amplifier}, then {@code amplifier-2}. */
    private String freeSlugOf(String title) {
        String base = DocumentParser.slugOf(title);
        String slug = base;
        for (int next = 2; documents.existsBySlug(slug); next++) {
            slug = base + "-" + next;
        }
        return slug;
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw CadProblem.invalid("A project needs a name.");
        }
        return title.strip();
    }

    /** Whether a shipped document stands in a shipped project's folder, from the same library — the project itself aside. */
    private static boolean shippedBeside(StoredDocument project, StoredDocument document) {
        return !document.getSlug().equals(project.getSlug())
               && project.shipped().orElseThrow().library().equals(document.shipped().orElseThrow().library())
               && document.shipped().orElseThrow().path().startsWith(project.getFolder() + PortablePaths.SEPARATOR);
    }

    private static boolean isProject(StoredDocument document) {
        return PROJECT.equals(document.getKind());
    }
}
