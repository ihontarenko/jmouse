package org.jmouse.parallax.store.scene;

import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Computations;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.store.catalogue.ObjectAccess;
import org.jmouse.parallax.limits.ParallaxLimits;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.support.ParallaxIdentifiers;
import org.jmouse.parallax.store.support.ParallaxJson;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks a scene document before it becomes a revision: within the installation's limits, every id unique, every
 * stored object readable by the saver, every inline definition compiling, every representation applying to its
 * object with settings in range, the timeline animating a parameter that exists, every viewport showing
 * representations that exist. A revision that was saved is therefore one that opens.
 */
public class SceneDocuments {

    private final ObjectAccess objectAccess;
    private final Catalogue catalogue;
    private final Computations computations;
    private final ParallaxLimits limits;
    private final ParallaxJson documents;
    private final InlineSources inline;

    public SceneDocuments(ObjectAccess objectAccess, Catalogue catalogue, Computations computations,
                          ParallaxLimits limits, ParallaxJson documents, InlineSources inline) {
        this.objectAccess = objectAccess;
        this.catalogue = catalogue;
        this.computations = computations;
        this.limits = limits;
        this.documents = documents;
        this.inline = inline;
    }

    /** A checked document, written canonically, with its digest and the stored objects it refers to. */
    public record Checked(SceneDocument document, String json, String hash, Set<String> referencedObjectIds) {
    }

    /**
     * @return the document with every inline object as text and as what the text defines; the JSON kept for it holds
     *         only the text
     */
    public Checked check(SceneDocument sent, ParallaxOwner realm) {
        if (sent == null) {
            throw ParallaxProblem.invalid("A scene document is required.");
        }
        SceneDocument kept = withObjects(sent, sent.objects().stream().map(this::asText).toList());
        SceneDocument document = hydrated(kept);
        if (document.formatVersion() != SceneDocument.FORMAT_VERSION) {
            throw ParallaxProblem.invalid("This server reads scene documents of format " + SceneDocument.FORMAT_VERSION + ".");
        }
        if (document.objects().size() > limits.maximumSceneObjects()) {
            throw ParallaxProblem.limit("A scene holds at most " + limits.maximumSceneObjects() + " objects.");
        }
        Set<String> objectIds = new HashSet<>();
        Set<String> representationIds = new HashSet<>();
        Set<String> referenced = new LinkedHashSet<>();
        for (SceneDocument.SceneObject object : document.objects()) {
            requireIdentifier(object.id(), "object");
            if (!objectIds.add(object.id())) {
                throw ParallaxProblem.invalid("Two objects of the scene are called '" + object.id() + "'.");
            }
            CompiledObject compiled = compile(object, realm, referenced);
            for (SceneDocument.SceneRepresentation representation : object.representations()) {
                requireIdentifier(representation.id(), "representation");
                if (!representationIds.add(representation.id())) {
                    throw ParallaxProblem.invalid("Two representations of the scene are called '" + representation.id() + "'.");
                }
                if (representation.kind() == null) {
                    throw ParallaxProblem.invalid("Representation '" + representation.id() + "' names no kind.");
                }
                Computation computation = computations.forObject(representation.kind(), compiled.definition())
                        .orElseThrow(() -> ParallaxProblem.invalid(representation.kind() + " does not apply to object '"
                                + object.id() + "'."));
                ComputationSettings.resolve(computation.settings(), representation.settings());
            }
        }
        checkTimeline(document);
        for (SceneDocument.Viewport viewport : document.viewports()) {
            requireIdentifier(viewport.id(), "viewport");
            for (String shown : viewport.representationIds()) {
                if (!representationIds.contains(shown)) {
                    throw ParallaxProblem.invalid("Viewport '" + viewport.id() + "' shows '" + shown + "', which is not in the scene.");
                }
            }
        }
        String json = documents.write(kept);
        if (json.getBytes(StandardCharsets.UTF_8).length > limits.maximumSceneDocumentBytes()) {
            throw ParallaxProblem.limit("The scene document is larger than the installation allows.");
        }
        return new Checked(document, json, ParallaxIdentifiers.sha256(json), referenced);
    }

    private CompiledObject compile(SceneDocument.SceneObject object, ParallaxOwner realm, Set<String> referenced) {
        if (object.objectId() != null && object.source() != null) {
            throw ParallaxProblem.invalid("Object '" + object.id() + "' is either a stored object or defined inline, not both.");
        }
        if (object.objectId() != null) {
            MathematicalObject stored = objectAccess.requireReadable(object.objectId());
            if (!stored.isCatalogue() && !stored.owner().orElseThrow().equals(realm)) {
                throw ParallaxProblem.invalid("Object '" + object.id() + "' belongs to another workspace; copy it here first.");
            }
            referenced.add(stored.getId());
            return catalogue.compile(stored, object.values());
        }
        if (object.definition() == null) {
            throw ParallaxProblem.invalid("Object '" + object.id() + "' names no stored object and defines none.");
        }
        return catalogue.compileChecked(object.definition(), object.parameters(), object.values());
    }

    private void checkTimeline(SceneDocument document) {
        SceneDocument.Timeline timeline = document.timeline();
        if (timeline == null) {
            return;
        }
        SceneDocument.SceneObject animated = document.objects().stream().filter(object -> object.id().equals(timeline.objectId()))
                .findFirst().orElseThrow(() -> ParallaxProblem.invalid("The timeline animates an object that is not in the scene."));
        List<ParameterDefinition> declared = animated.objectId() != null
                ? catalogue.parametersOf(objectAccess.requireReadable(animated.objectId()))
                : animated.parameters();
        ParameterDefinition parameter = declared.stream().filter(candidate -> candidate.name().equals(timeline.parameter()))
                .findFirst().orElseThrow(() -> ParallaxProblem.invalid("The timeline animates '" + timeline.parameter()
                        + "', which is not a parameter of that object."));
        if (!(timeline.start() >= parameter.minimum() && timeline.end() <= parameter.maximum() && timeline.start() < timeline.end())) {
            throw ParallaxProblem.invalid("The timeline runs within the parameter's range, start before end.");
        }
        if (!(timeline.durationSeconds() > 0) || !(timeline.speed() > 0)) {
            throw ParallaxProblem.invalid("The timeline has a positive duration and speed.");
        }
    }

    public SceneDocument read(String json) {
        return hydrated(documents.read(json, SceneDocument.class));
    }

    /** An inline object as it is kept: its text — the one sent, or the one written for the definition sent — alone. */
    private SceneDocument.SceneObject asText(SceneDocument.SceneObject object) {
        if (object.objectId() != null) {
            return object;
        }
        String source = inline.sourceOf(object.source(), object.definition(), object.parameters(), object.label());
        return new SceneDocument.SceneObject(object.id(), object.label(), null, source, null, List.of(), object.values(),
                object.representations(), object.visible(), object.color());
    }

    /** Every inline object with what its text defines, for whoever opens the scene. */
    private SceneDocument hydrated(SceneDocument document) {
        return withObjects(document, document.objects().stream().map(object -> {
            if (object.objectId() != null || object.source() == null) {
                return object;
            }
            DefinedObject defined = inline.read(object.source());
            return new SceneDocument.SceneObject(object.id(), object.label(), null, object.source(), defined.definition(),
                    defined.parameters(), object.values(), object.representations(), object.visible(), object.color());
        }).toList());
    }

    private static SceneDocument withObjects(SceneDocument document, List<SceneDocument.SceneObject> objects) {
        return new SceneDocument(document.formatVersion(), objects, document.camera(), document.timeline(), document.viewports());
    }

    private static void requireIdentifier(String id, String what) {
        if (id == null || !id.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")) {
            throw ParallaxProblem.invalid("A " + what + " id is 1–64 letters, digits, '-' or '_'.");
        }
    }
}
