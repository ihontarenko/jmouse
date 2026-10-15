package org.jmouse.parallax.store.scene;

import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Computations;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.authority.ParallaxCapability;
import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.authority.ParallaxPlace;
import org.jmouse.parallax.authority.ParallaxPlaces;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.store.catalogue.ObjectAccess;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.jpa.scene.Comparison;
import org.jmouse.parallax.jpa.scene.ComparisonLayout;
import org.jmouse.parallax.jpa.scene.ComparisonSlot;
import org.jmouse.parallax.jpa.scene.ComparisonStore;
import org.jmouse.parallax.support.ParallaxIdentifiers;
import org.jmouse.parallax.store.support.ParallaxJson;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Compare mode: two to six slots side by side — sin(x), sin(2x), sin(x²) — each an object under its own
 * representation and state, with explicit switches for sharing the camera and the parameters across them.
 */
public class Comparisons {

    private static final int MAXIMUM_SLOTS = 6;
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Double>> VALUES = new TypeReference<>() {
    };

    /**
     * One slot: a stored object or an inline one, the representation it is shown in, and its own state. An inline one
     * is kept as its jMX text (`source`); `definition` and `parameters` are what that text defines, filled in on
     * reading and accepted on a save from a reader that builds a definition.
     */
    public record Slot(String label, String objectId, String source, ObjectDefinition definition, List<ParameterDefinition> parameters,
                       RepresentationKind representation, Map<String, Object> settings, Map<String, Double> values,
                       Map<String, Object> camera) {
        public Slot {
            parameters = parameters == null ? List.of() : List.copyOf(parameters);
            settings = settings == null ? Map.of() : Map.copyOf(settings);
            values = values == null ? Map.of() : Map.copyOf(values);
        }
    }

    public record ComparisonDraft(String title, String description, ComparisonLayout layout, boolean synchronizeCamera,
                                  boolean synchronizeParameters, List<Slot> slots) {
    }

    /** A comparison, with the owner it belongs to and the realm it is seen in. */
    public record ComparisonView(String id, ParallaxOwner realm, ParallaxOwner owner, String title, String description,
                                 ComparisonLayout layout, boolean synchronizeCamera, boolean synchronizeParameters,
                                 List<Slot> slots, Instant updatedAt, long version) {
    }

    private final ComparisonStore comparisons;
    private final ParallaxPlaces places;
    private final ObjectAccess objectAccess;
    private final Catalogue catalogue;
    private final Computations computations;
    private final ParallaxAuthority authority;
    private final ParallaxJson json;
    private final Clock clock;
    private final InlineSources inline;

    public Comparisons(ComparisonStore comparisons, ParallaxPlaces places, ObjectAccess objectAccess,
                       Catalogue catalogue, Computations computations, ParallaxAuthority authority, ParallaxJson json,
                       Clock clock, InlineSources inline) {
        this.comparisons = comparisons;
        this.places = places;
        this.objectAccess = objectAccess;
        this.catalogue = catalogue;
        this.computations = computations;
        this.authority = authority;
        this.json = json;
        this.clock = clock;
        this.inline = inline;
    }

    @Transactional
    public ComparisonView create(ParallaxOwner owner, ComparisonDraft draft) {
        ParallaxPlace place = places.requireOwner(owner);
        authority.require(ParallaxCapability.SCENE_WRITE, place);
        Comparison comparison = new Comparison(ParallaxIdentifiers.newIdentifier(), owner, place.chain().getFirst(),
                authority.requireActor(), clock.instant());
        apply(comparison, draft);
        comparisons.save(comparison);
        writeSlots(comparison, draft.slots());
        return view(comparison);
    }

    @Transactional
    public ComparisonView update(String comparisonId, ComparisonDraft draft, long version) {
        Comparison comparison = require(comparisonId);
        authority.require(ParallaxCapability.SCENE_WRITE, placeOf(comparison));
        if (comparison.getVersion() != version) {
            throw ParallaxProblem.staleRevision("The comparison");
        }
        apply(comparison, draft);
        writeSlots(comparison, draft.slots());
        return view(comparison);
    }

    @Transactional(readOnly = true)
    public ComparisonView get(String comparisonId) {
        Comparison comparison = require(comparisonId);
        authority.require(ParallaxCapability.SCENE_READ, placeOf(comparison));
        return view(comparison);
    }

    @Transactional(readOnly = true)
    public List<ComparisonView> owned(ParallaxOwner owner) {
        authority.require(ParallaxCapability.SCENE_READ, places.requireOwner(owner));
        return comparisons.owned(owner).stream()
                .map(this::view).toList();
    }

    @Transactional
    public void delete(String comparisonId) {
        Comparison comparison = require(comparisonId);
        authority.require(ParallaxCapability.SCENE_DELETE, placeOf(comparison));
        comparisons.remove(comparison);
    }

    public Comparison require(String comparisonId) {
        return comparisons.find(comparisonId).orElseThrow(() -> ParallaxProblem.notFound("The comparison"));
    }

    public ComparisonView view(Comparison comparison) {
        List<Slot> slots = comparisons.slots(comparison.getId()).stream().map(this::slotOf).toList();
        return new ComparisonView(comparison.getId(), comparison.realm(), comparison.owner(),
                comparison.getTitle(), comparison.getDescription(), comparison.getLayout(), comparison.isSynchronizeCamera(),
                comparison.isSynchronizeParameters(), slots, comparison.getUpdatedAt(), comparison.getVersion());
    }

    /** A stored slot as the interface reads it: an inline text is hydrated into its definition and parameters. */
    private Slot slotOf(ComparisonSlot stored) {
        String source = stored.inlineSource();
        DefinedObject object = source == null ? null : inline.read(source);
        String camera = stored.camera();
        return new Slot(stored.label(), stored.objectId(), source,
                object == null ? null : object.definition(), object == null ? List.of() : object.parameters(),
                RepresentationKind.valueOf(stored.representationKind()),
                json.read(stored.settings(), MAP), json.read(stored.parameterValues(), VALUES),
                camera == null ? null : json.read(camera, MAP));
    }

    private void apply(Comparison comparison, ComparisonDraft draft) {
        if (draft.title() == null || draft.title().isBlank() || draft.title().length() > 160) {
            throw ParallaxProblem.invalid("A title of 1–160 characters is required.");
        }
        List<Slot> slots = draft.slots() == null ? List.of() : draft.slots();
        if (slots.size() < 2 || slots.size() > MAXIMUM_SLOTS) {
            throw ParallaxProblem.invalid("A comparison has 2 to " + MAXIMUM_SLOTS + " slots.");
        }
        for (Slot slot : slots) {
            check(slot, comparison.realm());
        }
        comparison.describe(draft.title().strip(), draft.description(),
                draft.layout() == null ? ComparisonLayout.SIDE_BY_SIDE : draft.layout(), draft.synchronizeCamera(),
                draft.synchronizeParameters(), clock.instant());
    }

    private void check(Slot slot, ParallaxOwner realm) {
        CompiledObject compiled;
        if (slot.objectId() != null) {
            MathematicalObject stored = objectAccess.requireReadable(slot.objectId());
            if (!stored.isCatalogue() && !stored.owner().orElseThrow().equals(realm)) {
                throw ParallaxProblem.invalid("A slot's object belongs to another workspace.");
            }
            compiled = catalogue.compile(stored, slot.values());
        } else if (slot.source() != null || slot.definition() != null) {
            DefinedObject defined = inline.read(inline.sourceOf(slot.source(), slot.definition(), slot.parameters(), slot.label()));
            compiled = catalogue.compileChecked(defined.definition(), defined.parameters(), slot.values());
        } else {
            throw ParallaxProblem.invalid("A slot names a stored object or defines one.");
        }
        if (slot.representation() == null) {
            throw ParallaxProblem.invalid("A slot names its representation.");
        }
        ComputationSettings.resolve(computations.forObject(slot.representation(), compiled.definition())
                .orElseThrow(() -> ParallaxProblem.invalid(slot.representation() + " does not apply to a slot's object."))
                .settings(), slot.settings());
    }

    private void writeSlots(Comparison comparison, List<Slot> slots) {
        List<ComparisonSlot> stored = slots.stream().map(this::storedSlotOf).toList();
        comparisons.replaceSlots(comparison.getId(), stored);
    }

    /** A slot as its row keeps it: a stored object by its identifier, an inline one as its jMX text. */
    private ComparisonSlot storedSlotOf(Slot slot) {
        String objectId = slot.objectId() == null ? null : objectAccess.requireReadable(slot.objectId()).getId();
        String inlineSource = slot.objectId() != null ? null
                : inline.sourceOf(slot.source(), slot.definition(), slot.parameters(), slot.label());
        return new ComparisonSlot(slot.label(), objectId, inlineSource, slot.representation().name(),
                json.write(slot.settings()), json.write(slot.values()),
                slot.camera() == null ? null : json.write(slot.camera()));
    }

    /** A comparison lives in its owner inside its realm: the workspace, then the project. */
    public static ParallaxPlace placeOf(Comparison comparison) {
        return ParallaxPlace.of(comparison.realm(), comparison.owner());
    }
}
