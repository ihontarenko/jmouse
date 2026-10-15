package org.jmouse.parallax.store.collection;

import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.authority.ParallaxCapability;
import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.authority.ParallaxPlaces;
import org.jmouse.parallax.jpa.catalogue.ObjectStore;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.jpa.collection.CollectionItem;
import org.jmouse.parallax.jpa.collection.CollectionStore;
import org.jmouse.parallax.jpa.collection.ObjectCollection;
import org.jmouse.parallax.jpa.collection.ObjectCollection.Kind;
import org.jmouse.parallax.jpa.collection.ObjectCollection.Purpose;
import org.jmouse.parallax.jpa.collection.ObjectCollection.Visibility;
import org.jmouse.parallax.store.catalogue.ObjectAccess;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.store.scene.Comparisons;
import org.jmouse.parallax.jpa.scene.Scene;
import org.jmouse.parallax.store.scene.Scenes;
import org.jmouse.parallax.support.ParallaxIdentifiers;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Collections: the curated shelves every reader sees (Beautiful surfaces, Chaos, 4D objects…) and the ones a person
 * keeps for themselves. An item is an object, a scene or a comparison; what a reader cannot open is left out of
 * the listing rather than shown as a hole.
 *
 * <p>A formula set is a collection too — the examples of a language, in sections, in the order it is learned — with
 * the purpose {@link Purpose#FORMULA_SET}. It is listed on the Formulas page, not among the shelves.</p>
 */
public class Collections {

    /** The owner type of a personal collection: its account. */
    private static final String ACCOUNT_OWNER_TYPE = "ACCOUNT";

    /** {@code realm} is the host owner a collection of visibility WORKSPACE is seen in — a workspace in Parallax. */
    public record CollectionDraft(String title, String summary, String description, Visibility visibility,
                                  ParallaxOwner realm, String coverObjectSlug, int position) {
    }

    /** One item; {@code section} is the title of the section it is filed under, null in an unsectioned collection. */
    public record ItemView(String id, int position, CollectionItem.Kind kind, String targetId, String slug, String title,
                           String summary, String note, String section, Instant addedAt) {
    }

    /** A titled part of a collection written in sections, its objects in order; an unsectioned one has no title. */
    public record SectionSeed(String title, List<String> objectSlugs) {
    }

    public record CollectionSummary(String id, String slug, String title, String summary, Kind kind, Purpose purpose,
                                    Visibility visibility, String coverObjectSlug, long itemCount, Instant updatedAt,
                                    long version) {
    }

    public record CollectionView(CollectionSummary collection, String description, List<ItemView> items,
                                 boolean editable) {
    }

    private final CollectionStore collections;
    private final ObjectStore objects;
    private final ObjectAccess objectAccess;
    private final Scenes scenes;
    private final Comparisons comparisons;
    private final ParallaxAuthority authority;
    private final ParallaxPlaces places;
    private final Clock clock;

    public Collections(CollectionStore collections, ObjectStore objects, ObjectAccess objectAccess,
                       Scenes scenes, Comparisons comparisons, ParallaxAuthority authority, ParallaxPlaces places,
                       Clock clock) {
        this.collections = collections;
        this.objects = objects;
        this.objectAccess = objectAccess;
        this.scenes = scenes;
        this.comparisons = comparisons;
        this.authority = authority;
        this.places = places;
        this.clock = clock;
    }

    /** The curated collections of one purpose: the shelves, or the formula sets. */
    @Transactional(readOnly = true)
    public List<CollectionSummary> curated(Purpose purpose) {
        return collections.ofKind(Kind.CURATED, purpose == null ? Purpose.SHELF : purpose)
                .stream()
                .filter(collection -> collection.getVisibility() == Visibility.PUBLIC || canEdit(collection))
                .map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionSummary> mine() {
        ParallaxOwner owner = accountOwner(authority.requireActor());
        return collections.owned(owner).stream()
                .map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public CollectionView get(String idOrSlug) {
        ObjectCollection collection = requireReadable(idOrSlug);
        return view(collection);
    }

    @Transactional
    /** @param realm the workspace the draft names for visibility WORKSPACE, as the host builds it; null when none */
    public CollectionView create(Kind kind, String slug, CollectionDraft draft, ParallaxOwner realm) {
        String actor = authority.requireActor();
        if (kind == Kind.CURATED) {
            authority.requireGlobally(ParallaxCapability.COLLECTION_CURATE);
        } else {
            authority.requireGlobally(ParallaxCapability.COLLECTION_CREATE);
        }
        String base = slug == null || slug.isBlank() ? ParallaxIdentifiers.slugOf(requireTitle(draft.title())) : slug;
        String candidate = base;
        for (int suffix = 2; collections.existsBySlug(candidate); suffix++) {
            candidate = base + "-" + suffix;
        }
        ObjectCollection collection = new ObjectCollection(ParallaxIdentifiers.newIdentifier(), candidate, kind, Purpose.SHELF,
                kind == Kind.PERSONAL ? accountOwner(actor) : null, clock.instant());
        apply(collection, draft, realm);
        collections.save(collection);
        return view(collection);
    }

    /** @param realm the workspace the draft names for visibility WORKSPACE, as the host builds it; null when none */
    @Transactional
    public CollectionView update(String collectionId, CollectionDraft draft, long version, ParallaxOwner realm) {
        ObjectCollection collection = requireEditable(collectionId);
        if (collection.getVersion() != version) {
            throw ParallaxProblem.staleRevision("The collection");
        }
        apply(collection, draft, realm);
        return view(collection);
    }

    @Transactional
    public void delete(String collectionId) {
        collections.remove(requireEditable(collectionId));
    }

    /** Adds an item at the end; the item must be something the editor can open. */
    @Transactional
    public CollectionView add(String collectionId, CollectionItem.Kind kind, String targetId, String note) {
        ObjectCollection collection = requireEditable(collectionId);
        String resolved = switch (kind) {
            case OBJECT -> objectAccess.requireReadable(targetId).getId();
            case SCENE -> scenes.get(targetId).scene().id();
            case COMPARISON -> comparisons.get(targetId).id();
        };
        if (collection.getKind() == Kind.CURATED && kind == CollectionItem.Kind.OBJECT
                && !objectAccess.requireReadable(resolved).isCatalogue()) {
            throw ParallaxProblem.invalid("A curated collection holds catalogue objects.");
        }
        int position = collections.nextItemPosition(collectionId);
        collections.addItem(collectionId, new CollectionItem(ParallaxIdentifiers.newIdentifier(), position, kind,
                kind == CollectionItem.Kind.OBJECT ? resolved : null, kind == CollectionItem.Kind.SCENE ? resolved : null,
                kind == CollectionItem.Kind.COMPARISON ? resolved : null, note, null, authority.requireActor(),
                clock.instant()));
        collection.touch(clock.instant());
        return view(collection);
    }

    @Transactional
    public CollectionView remove(String collectionId, String itemId) {
        ObjectCollection collection = requireEditable(collectionId);
        if (!collections.removeItem(collectionId, itemId)) {
            throw ParallaxProblem.notFound("The item");
        }
        collections.renumber(collections.itemIds(collectionId));
        collection.touch(clock.instant());
        return view(collection);
    }

    /** Puts the items in the given order; every item of the collection must be named once. */
    @Transactional
    public CollectionView reorder(String collectionId, List<String> itemIds) {
        ObjectCollection collection = requireEditable(collectionId);
        List<String> current = collections.itemIds(collectionId);
        if (itemIds == null || itemIds.size() != current.size() || !current.containsAll(itemIds)) {
            throw ParallaxProblem.invalid("Name every item of the collection exactly once.");
        }
        collections.renumber(itemIds);
        collection.touch(clock.instant());
        return view(collection);
    }

    /**
     * The bootstrap's path: a curated collection of catalogue objects, written once and never rewritten — a curator may
     * have reordered it since. An object the catalogue does not hold is left out.
     *
     * @return whether the collection was written now
     */
    @Transactional
    public boolean seed(String slug, Purpose purpose, String title, String summary, String description,
                        List<SectionSeed> sections, int position) {
        if (collections.existsBySlug(slug)) {
            return false;
        }
        Instant now = clock.instant();
        ObjectCollection collection = new ObjectCollection(ParallaxIdentifiers.newIdentifier(), slug, Kind.CURATED, purpose, null, now);
        String cover = sections.stream().flatMap(section -> section.objectSlugs().stream()).findFirst()
                .flatMap(objects::findBySlug).map(MathematicalObject::getId).orElse(null);
        collection.describe(title, summary, description, Visibility.PUBLIC, null, cover, position, now);
        collections.save(collection);
        int index = 0;
        for (SectionSeed section : sections) {
            for (String objectSlug : section.objectSlugs()) {
                Optional<MathematicalObject> object = objects.findBySlug(objectSlug);
                if (object.isPresent()) {
                    collections.addItem(collection.getId(), new CollectionItem(ParallaxIdentifiers.newIdentifier(),
                            index++, CollectionItem.Kind.OBJECT, object.get().getId(), null, null, null, section.title(),
                            "00000000-0000-4000-8000-000000000001", now));
                }
            }
        }
        return true;
    }

    /** The collection as a share link shows it: the link is the authority, so its own visibility is not asked. */
    @Transactional(readOnly = true)
    public CollectionView sharedView(String collectionId) {
        return view(collections.find(collectionId).orElseThrow(() -> ParallaxProblem.notFound("The collection")));
    }

    /** Only whoever may edit a collection hands out links to it. */
    @Transactional(readOnly = true)
    public void requireEditableForSharing(String collectionId) {
        requireEditable(collectionId);
    }

    // ── Internals ────────────────────────────────────────────────────────────────────────────────────────────────

    public ObjectCollection requireReadable(String idOrSlug) {
        ObjectCollection collection = collections.find(idOrSlug).or(() -> collections.findBySlug(idOrSlug))
                .orElseThrow(() -> ParallaxProblem.notFound("The collection"));
        if (!canRead(collection)) {
            throw ParallaxProblem.notFound("The collection");
        }
        return collection;
    }

    private boolean canRead(ObjectCollection collection) {
        return switch (collection.getVisibility()) {
            case PUBLIC -> true;
            case WORKSPACE -> canEdit(collection) || collection.realm()
                    .map(realm -> authority.permits(ParallaxCapability.OWNER_READ, places.requireOwner(realm)))
                    .orElse(false);
            case PRIVATE -> canEdit(collection);
        };
    }

    private boolean canEdit(ObjectCollection collection) {
        if (collection.getKind() == Kind.CURATED) {
            return authority.permitsGlobally(ParallaxCapability.COLLECTION_CURATE);
        }
        return authority.actor().map(actor -> collection.owner().equals(Optional.of(accountOwner(actor)))).orElse(false);
    }

    private ObjectCollection requireEditable(String collectionId) {
        ObjectCollection collection = requireReadable(collectionId);
        if (!canEdit(collection)) {
            throw ParallaxProblem.refused("Only its owner changes a personal collection, and a curator a curated one.");
        }
        return collection;
    }

    private void apply(ObjectCollection collection, CollectionDraft draft, ParallaxOwner realm) {
        Visibility visibility = draft.visibility() == null ? Visibility.PRIVATE : draft.visibility();
        if (visibility == Visibility.WORKSPACE) {
            if (realm == null) {
                throw ParallaxProblem.invalid("A collection shared with a workspace names it.");
            }
            authority.require(ParallaxCapability.OWNER_READ, places.requireOwner(realm));
        }
        String cover = draft.coverObjectSlug() == null ? null : objectAccess.requireReadable(draft.coverObjectSlug()).getId();
        collection.describe(requireTitle(draft.title()), draft.summary(), draft.description(), visibility,
                visibility == Visibility.WORKSPACE ? realm : null, cover, draft.position(), clock.instant());
    }

    private CollectionSummary summary(ObjectCollection collection) {
        long count = collections.countItems(collection.getId());
        String cover = collection.getCoverObjectId() == null ? null
                : objects.find(collection.getCoverObjectId()).map(MathematicalObject::getSlug).orElse(null);
        return new CollectionSummary(collection.getId(), collection.getSlug(), collection.getTitle(), collection.getSummary(),
                collection.getKind(), collection.getPurpose(), collection.getVisibility(), cover, count,
                collection.getUpdatedAt(), collection.getVersion());
    }

    private CollectionView view(ObjectCollection collection) {
        List<ItemView> items = collections.items(collection.getId()).stream()
                .map(this::item).flatMap(Optional::stream).toList();
        return new CollectionView(summary(collection), collection.getDescription(), items, canEdit(collection));
    }

    /** An item the reader can open, described; empty for one they cannot. */
    private Optional<ItemView> item(CollectionItem row) {
        try {
            return switch (row.kind()) {
                case OBJECT -> {
                    MathematicalObject object = objectAccess.requireReadable(row.objectId());
                    yield Optional.of(new ItemView(row.id(), row.position(), row.kind(), object.getId(), object.getSlug(),
                            object.getTitle(), object.getSummary(), row.note(), row.section(), row.addedAt()));
                }
                case SCENE -> {
                    Scene scene = scenes.requireScene(row.sceneId());
                    var view = scenes.get(row.sceneId()).scene();
                    yield Optional.of(new ItemView(row.id(), row.position(), row.kind(), scene.getId(), null, view.title(),
                            view.description(), row.note(), row.section(), row.addedAt()));
                }
                case COMPARISON -> {
                    var view = comparisons.get(row.comparisonId());
                    yield Optional.of(new ItemView(row.id(), row.position(), row.kind(), view.id(), null, view.title(),
                            view.description(), row.note(), row.section(), row.addedAt()));
                }
            };
        } catch (ParallaxProblem hidden) {
            return Optional.empty();
        }
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank() || title.strip().length() > 160) {
            throw ParallaxProblem.invalid("A title of 1–160 characters is required.");
        }
        return title.strip();
    }

    /**
     * The owner a personal collection is stored under: the acting account. It is never a place the host is asked
     * about — only its owner edits one — so the type is the library's own name for an actor.
     */
    private static ParallaxOwner accountOwner(String actor) {
        return ParallaxOwner.of(ACCOUNT_OWNER_TYPE, actor);
    }
}
