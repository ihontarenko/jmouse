package org.jmouse.parallax.store.scene;

import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.authority.ParallaxCapability;
import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.authority.ParallaxPlace;
import org.jmouse.parallax.authority.ParallaxPlaces;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.jpa.catalogue.Instrument;
import org.jmouse.parallax.jpa.catalogue.InstrumentStore;
import org.jmouse.parallax.jpa.catalogue.ObjectStore;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.jpa.scene.Scene;
import org.jmouse.parallax.jpa.scene.SceneRevision;
import org.jmouse.parallax.jpa.scene.SceneSnapshot;
import org.jmouse.parallax.jpa.scene.SceneStore;
import org.jmouse.parallax.support.ParallaxIdentifiers;
import org.jmouse.parallax.store.support.ParallaxJson;
import org.jmouse.parallax.store.support.ParallaxPage;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scenes and their history. A save checks the document and writes the next revision — or nothing, when the
 * document is the head's unchanged; the scene names its head; the stored objects the head uses are indexed so the
 * catalogue can say which scenes are built on an object.
 */
public class Scenes {

    /** A scene in a list, with the owner it belongs to (a project, in Parallax) and the realm it is seen in. */
    public record SceneSummary(String id, ParallaxOwner realm, ParallaxOwner owner, String title, String description,
                               String instrumentCode, int headRevision, String forkedFromSceneId, Instant updatedAt,
                               Instant archivedAt, long version) {

        public static SceneSummary of(Scene scene) {
            return new SceneSummary(scene.getId(), scene.realm(), scene.owner(), scene.getTitle(),
                    scene.getDescription(), scene.getInstrumentCode(), scene.getHeadRevisionNumber(),
                    scene.getForkedFromSceneId(), scene.getUpdatedAt(), scene.getArchivedAt(), scene.getVersion());
        }
    }

    public record SceneView(SceneSummary scene, SceneDocument document, Map<String, Boolean> abilities) {
    }

    public record RevisionView(int number, String message, String authorId, Instant createdAt, String documentHash) {
    }

    public record SnapshotView(String id, int revision, String title, double timelinePosition, Map<String, Object> camera,
                               Map<String, Map<String, Double>> parameterValues, String createdBy, Instant createdAt) {
    }

    private static final TypeReference<Map<String, Object>> CAMERA = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Map<String, Double>>> VALUES = new TypeReference<>() {
    };

    private final SceneStore scenes;
    private final SceneDocuments documents;
    private final ParallaxPlaces places;
    private final InstrumentStore instruments;
    private final ObjectStore objects;
    private final Catalogue catalogue;
    private final ParallaxAuthority authority;
    private final ParallaxJson json;
    private final Clock clock;

    public Scenes(SceneStore scenes, SceneDocuments documents, ParallaxPlaces places, InstrumentStore instruments,
                  ObjectStore objects, Catalogue catalogue, ParallaxAuthority authority, ParallaxJson json, Clock clock) {
        this.scenes = scenes;
        this.documents = documents;
        this.places = places;
        this.instruments = instruments;
        this.objects = objects;
        this.catalogue = catalogue;
        this.authority = authority;
        this.json = json;
        this.clock = clock;
    }

    /**
     * A new scene. Given no document it starts from its instrument: the instrument's preset object with the
     * instrument's representations — so "open the Surface explorer" is a scene that already shows a surface.
     */
    @Transactional
    public SceneView create(ParallaxOwner owner, String title, String description, String instrumentCode,
                            SceneDocument document) {
        ParallaxPlace place = places.requireOwner(owner);
        authority.require(ParallaxCapability.SCENE_WRITE, place);
        places.requireAcceptsNew(owner);
        Instrument instrument = instrumentCode == null ? null : instruments.find(instrumentCode)
                .orElseThrow(() -> ParallaxProblem.invalid("'" + instrumentCode + "' is not an instrument."));
        SceneDocument initial = document != null ? document : startingDocument(instrument);
        Instant now = clock.instant();
        Scene scene = scenes.save(new Scene(ParallaxIdentifiers.newIdentifier(), owner, place.chain().getFirst(),
                requireTitle(title), description, instrumentCode, null, null, authority.requireActor(), now));
        writeRevision(scene, documents.check(initial, scene.realm()), "Created", now);
        places.recordChange(owner, now);
        return view(scene);
    }

    @Transactional(readOnly = true)
    public SceneView get(String sceneId) {
        Scene scene = requireScene(sceneId);
        authority.require(ParallaxCapability.SCENE_READ, placeOf(scene));
        return view(scene);
    }

    @Transactional(readOnly = true)
    public ParallaxPage<SceneSummary> owned(ParallaxOwner owner, int page, int size) {
        authority.require(ParallaxCapability.SCENE_READ, places.requireOwner(owner));
        ParallaxPage.requireBounds(page, size);
        return ParallaxPage.of(scenes.owned(owner, page, size), SceneSummary::of);
    }

    /** The realm's (a workspace's) recent scenes, those of owners the caller may open. */
    @Transactional(readOnly = true)
    public List<SceneSummary> recentInRealm(ParallaxOwner realm, int size) {
        places.requireOwner(realm);
        int candidates = Math.min(size, 50) * 4;
        ParallaxPage.requireBounds(0, candidates);
        return scenes.recentInRealm(realm, candidates).stream()
                .filter(scene -> authority.permits(ParallaxCapability.SCENE_READ, placeOf(scene)))
                .limit(size).map(SceneSummary::of).toList();
    }

    /** Saves the next revision; `baseRevision` is the head the editor started from, so concurrent saves are refused. */
    @Transactional
    public SceneView save(String sceneId, SceneDocument document, String message, int baseRevision) {
        Scene scene = requireScene(sceneId);
        authority.require(ParallaxCapability.SCENE_WRITE, placeOf(scene));
        if (scene.getHeadRevisionNumber() != baseRevision) {
            throw ParallaxProblem.staleRevision("The scene");
        }
        SceneDocuments.Checked checked = documents.check(document, scene.realm());
        SceneRevision head = requireRevision(sceneId, scene.getHeadRevisionNumber());
        if (!head.getDocumentHash().equals(checked.hash())) {
            writeRevision(scene, checked, message, clock.instant());
        }
        return view(scene);
    }

    @Transactional
    public SceneView describe(String sceneId, String title, String description, long version) {
        Scene scene = requireScene(sceneId);
        authority.require(ParallaxCapability.SCENE_WRITE, placeOf(scene));
        if (scene.getVersion() != version) {
            throw ParallaxProblem.staleRevision("The scene");
        }
        scene.describe(requireTitle(title), description, clock.instant());
        return view(scene);
    }

    @Transactional(readOnly = true)
    public List<RevisionView> revisions(String sceneId) {
        Scene scene = requireScene(sceneId);
        authority.require(ParallaxCapability.SCENE_READ, placeOf(scene));
        return scenes.revisionsNewestFirst(sceneId).stream()
                .map(revision -> new RevisionView(revision.getRevisionNumber(), revision.getMessage(), revision.getAuthorId(),
                        revision.getCreatedAt(), revision.getDocumentHash())).toList();
    }

    @Transactional(readOnly = true)
    public SceneDocument revision(String sceneId, int number) {
        Scene scene = requireScene(sceneId);
        authority.require(ParallaxCapability.SCENE_READ, placeOf(scene));
        return documents.read(requireRevision(sceneId, number).getDocument());
    }

    /** Restoring writes the old document as a new revision; history is never rewritten. */
    @Transactional
    public SceneView restore(String sceneId, int number) {
        Scene scene = requireScene(sceneId);
        authority.require(ParallaxCapability.SCENE_WRITE, placeOf(scene));
        SceneDocument old = documents.read(requireRevision(sceneId, number).getDocument());
        writeRevision(scene, documents.check(old, scene.realm()), "Restored revision " + number, clock.instant());
        return view(scene);
    }

    /** A copy of a revision as a new scene in a project the caller may write to — theirs or a colleague's. */
    @Transactional
    public SceneView fork(String sceneId, Integer revisionNumber, ParallaxOwner target, String title) {
        Scene source = requireScene(sceneId);
        authority.require(ParallaxCapability.SCENE_READ, placeOf(source));
        return forkFrom(source, revisionNumber, target, title);
    }

    /** The fork itself, once the right to read the source has been established (by membership or a share link). */
    @Transactional
    public SceneView forkFrom(Scene source, Integer revisionNumber, ParallaxOwner target, String title) {
        int number = revisionNumber == null ? source.getHeadRevisionNumber() : revisionNumber;
        SceneDocument document = documents.read(requireRevision(source.getId(), number).getDocument());
        ParallaxPlace targetPlace = places.requireOwner(target);
        authority.require(ParallaxCapability.SCENE_WRITE, targetPlace);
        Instant now = clock.instant();
        Scene fork = scenes.save(new Scene(ParallaxIdentifiers.newIdentifier(), target, targetPlace.chain().getFirst(),
                requireTitle(title == null ? source.getTitle() : title), source.getDescription(), source.getInstrumentCode(),
                source.getId(), number, authority.requireActor(), now));
        writeRevision(fork, documents.check(document, fork.realm()), "Copied from a scene", now);
        return view(fork);
    }

    @Transactional
    public SceneView setArchived(String sceneId, boolean archived) {
        Scene scene = requireScene(sceneId);
        authority.require(ParallaxCapability.SCENE_DELETE, placeOf(scene));
        if (archived) {
            scene.archive(clock.instant());
        } else {
            scene.restore(clock.instant());
        }
        return view(scene);
    }

    @Transactional
    public void delete(String sceneId) {
        Scene scene = requireScene(sceneId);
        authority.require(ParallaxCapability.SCENE_DELETE, placeOf(scene));
        scenes.remove(scene);
    }

    // ── Snapshots ────────────────────────────────────────────────────────────────────────────────────────────────

    @Transactional
    public SnapshotView snapshot(String sceneId, String title, double timelinePosition, Map<String, Object> camera,
                                 Map<String, Map<String, Double>> parameterValues) {
        Scene scene = requireScene(sceneId);
        authority.require(ParallaxCapability.SCENE_WRITE, placeOf(scene));
        SceneSnapshot snapshot = scenes.addSnapshot(new SceneSnapshot(ParallaxIdentifiers.newIdentifier(), sceneId,
                scene.getHeadRevisionNumber(), requireTitle(title), timelinePosition,
                json.write(camera == null ? Map.of() : camera), json.write(parameterValues == null ? Map.of() : parameterValues),
                authority.requireActor(), clock.instant()));
        return snapshotView(snapshot);
    }

    @Transactional(readOnly = true)
    public List<SnapshotView> snapshots(String sceneId) {
        Scene scene = requireScene(sceneId);
        authority.require(ParallaxCapability.SCENE_READ, placeOf(scene));
        return scenes.snapshotsNewestFirst(sceneId).stream().map(this::snapshotView).toList();
    }

    @Transactional
    public void deleteSnapshot(String sceneId, String snapshotId) {
        Scene scene = requireScene(sceneId);
        authority.require(ParallaxCapability.SCENE_WRITE, placeOf(scene));
        SceneSnapshot snapshot = scenes.findSnapshot(snapshotId).filter(found -> found.getSceneId().equals(sceneId))
                .orElseThrow(() -> ParallaxProblem.notFound("The snapshot"));
        scenes.removeSnapshot(snapshot);
    }

    // ── Internals ────────────────────────────────────────────────────────────────────────────────────────────────

    public Scene requireScene(String sceneId) {
        return scenes.find(sceneId).orElseThrow(() -> ParallaxProblem.notFound("The scene"));
    }

    public SceneDocument headDocument(Scene scene, Integer pinnedRevision) {
        int number = pinnedRevision == null ? scene.getHeadRevisionNumber() : pinnedRevision;
        return documents.read(requireRevision(scene.getId(), number).getDocument());
    }

    private void writeRevision(Scene scene, SceneDocuments.Checked checked, String message, Instant now) {
        int next = scene.getHeadRevisionNumber() + 1;
        scenes.addRevision(new SceneRevision(ParallaxIdentifiers.newIdentifier(), scene.getId(), next, SceneDocument.FORMAT_VERSION,
                checked.json(), checked.hash(), message == null || message.isBlank() ? null : message.strip(),
                authority.requireActor(), now));
        scene.advanceTo(next, now);
        scenes.replaceReferences(scene.getId(), checked.referencedObjectIds());
    }

    private SceneRevision requireRevision(String sceneId, int number) {
        return scenes.findRevision(sceneId, number)
                .orElseThrow(() -> ParallaxProblem.notFound("Revision " + number));
    }

    private SceneDocument startingDocument(Instrument instrument) {
        if (instrument == null || instrument.getPresetObjectId() == null) {
            return SceneDocument.empty();
        }
        MathematicalObject preset = objects.find(instrument.getPresetObjectId()).orElse(null);
        if (preset == null) {
            return SceneDocument.empty();
        }
        List<RepresentationKind> kinds = json.read(instrument.getDefaultRepresentations(),
                new TypeReference<List<RepresentationKind>>() {
                });
        List<String> ownStack = catalogue.representationsOf(preset).stream()
                .map(representation -> representation.kind().name()).toList();
        List<RepresentationKind> chosen = kinds.stream().filter(kind -> ownStack.contains(kind.name())).toList();
        if (chosen.isEmpty() && !ownStack.isEmpty()) {
            chosen = List.of(RepresentationKind.valueOf(ownStack.getFirst()));
        }
        List<SceneDocument.SceneRepresentation> stack = new ArrayList<>();
        for (RepresentationKind kind : chosen) {
            stack.add(new SceneDocument.SceneRepresentation(kind.name().toLowerCase().replace('_', '-'), kind, Map.of(),
                    stack.isEmpty(), Map.of()));
        }
        List<String> shown = stack.isEmpty() ? List.of() : List.of(stack.getFirst().id());
        SceneDocument.SceneObject object = new SceneDocument.SceneObject("object-1", preset.getTitle(), preset.getId(), null,
                null, List.of(), Map.of(), stack, true, null);
        return new SceneDocument(SceneDocument.FORMAT_VERSION, List.of(object), null, null,
                List.of(new SceneDocument.Viewport("main", instrument.getTitle(), shown, true)));
    }

    private SceneView view(Scene scene) {
        Map<String, Boolean> abilities = new java.util.LinkedHashMap<>();
        // The keys are the product's permission names, which the interface reads.
        ParallaxPlace place = placeOf(scene);
        abilities.put("scene:write", authority.permits(ParallaxCapability.SCENE_WRITE, place));
        abilities.put("scene:delete", authority.permits(ParallaxCapability.SCENE_DELETE, place));
        abilities.put("scene:share", authority.permits(ParallaxCapability.SCENE_SHARE, place));
        abilities.put("compute:run", authority.permits(ParallaxCapability.COMPUTE, place));
        return new SceneView(SceneSummary.of(scene), headDocument(scene, null), abilities);
    }

    private SnapshotView snapshotView(SceneSnapshot snapshot) {
        return new SnapshotView(snapshot.getId(), snapshot.getRevisionNumber(), snapshot.getTitle(), snapshot.getTimelinePosition(),
                json.read(snapshot.getCamera(), CAMERA), json.read(snapshot.getParameterValues(), VALUES),
                snapshot.getCreatedBy(), snapshot.getCreatedAt());
    }

    /** A scene inside its owner inside its realm: the workspace, the project, the scene. */
    public static ParallaxPlace placeOf(Scene scene) {
        return ParallaxPlace.of(scene.realm(), scene.owner()).within(ParallaxOwner.SCENE, scene.getId());
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank() || title.strip().length() > 160) {
            throw ParallaxProblem.invalid("A title of 1–160 characters is required.");
        }
        return title.strip();
    }
}
