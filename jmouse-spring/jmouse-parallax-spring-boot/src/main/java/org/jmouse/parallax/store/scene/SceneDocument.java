package org.jmouse.parallax.store.scene;

import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.List;
import java.util.Map;

/**
 * What a scene is: the objects in it, each with its parameter values and its representation stack, the camera,
 * the timeline and how the viewports are laid out. Saved whole, as one immutable revision; the backend checks it
 * and never draws it.
 */
public record SceneDocument(int formatVersion, List<SceneObject> objects, Camera camera, Timeline timeline,
                            List<Viewport> viewports) {

    public static final int FORMAT_VERSION = 1;

    public SceneDocument {
        objects = objects == null ? List.of() : List.copyOf(objects);
        viewports = viewports == null ? List.of() : List.copyOf(viewports);
        if (formatVersion == 0) {
            formatVersion = FORMAT_VERSION;
        }
    }

    public static SceneDocument empty() {
        return new SceneDocument(FORMAT_VERSION, List.of(), Camera.standard(), null,
                List.of(new Viewport("main", "Scene", List.of(), true)));
    }

    /**
     * One object of the scene: a stored object (`objectId`, from the catalogue or the workspace) or one defined here.
     * One defined here is kept as its jMX text (`source`); `definition` and `parameters` are what that text defines,
     * filled in when a scene is read, and accepted on a save from a reader that builds a definition rather than a text.
     * `values` are its parameter values in this scene.
     */
    public record SceneObject(String id, String label, String objectId, String source, ObjectDefinition definition,
                              List<ParameterDefinition> parameters, Map<String, Double> values,
                              List<SceneRepresentation> representations, boolean visible, String color) {
        public SceneObject {
            parameters = parameters == null ? List.of() : List.copyOf(parameters);
            values = values == null ? Map.of() : Map.copyOf(values);
            representations = representations == null ? List.of() : List.copyOf(representations);
        }
    }

    /** An entry of an object's representation stack, with its settings and how it is styled. */
    public record SceneRepresentation(String id, RepresentationKind kind, Map<String, Object> settings, boolean visible,
                                      Map<String, Object> style) {
        public SceneRepresentation {
            settings = settings == null ? Map.of() : Map.copyOf(settings);
            style = style == null ? Map.of() : Map.copyOf(style);
        }
    }

    public enum Projection { PERSPECTIVE, ORTHOGRAPHIC }

    /** The viewpoint, and for objects beyond R³ the rotations applied before projecting (axis, axis, degrees). */
    public record Camera(Projection projection, List<Double> position, List<Double> target, List<Double> up,
                         double fieldOfView, List<List<Double>> rotations) {
        public Camera {
            if (projection == null) {
                projection = Projection.PERSPECTIVE;
            }
            position = position == null ? List.of(6.0, 6.0, 6.0) : List.copyOf(position);
            target = target == null ? List.of(0.0, 0.0, 0.0) : List.copyOf(target);
            up = up == null ? List.of(0.0, 0.0, 1.0) : List.copyOf(up);
            rotations = rotations == null ? List.of() : rotations.stream().map(List::copyOf).toList();
            if (!(fieldOfView > 0)) {
                fieldOfView = 45;
            }
        }

        static Camera standard() {
            return new Camera(Projection.PERSPECTIVE, null, null, null, 45, null);
        }
    }

    /** Which parameter of which object the timeline animates, over what range, how fast, and where it stands. */
    public record Timeline(String objectId, String parameter, double start, double end, double durationSeconds,
                           boolean loop, double speed, double position, boolean trail) {
    }

    /** A viewport shows some representations; compare mode puts several side by side with a shared camera. */
    public record Viewport(String id, String title, List<String> representationIds, boolean synchronizeCamera) {
        public Viewport {
            representationIds = representationIds == null ? List.of() : List.copyOf(representationIds);
        }
    }
}
