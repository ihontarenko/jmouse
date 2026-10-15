package org.jmouse.parallax.dimension;

import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.Polytope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Four dimensions and more, never drawn literally: rotated in a chosen plane of Rⁿ, then projected (perspective
 * from the last axis, or orthographic) or sliced by the hyperplane x_n = c. (A hypersurface f(x, y, z, w) = 0 is
 * drawn as an implicit surface with w held fixed.)
 */
public final class HigherDimensionComputations {

    private HigherDimensionComputations() {
    }

    static double distance(double[] first, double[] second) {
        double sum = 0;
        for (int axis = 0; axis < first.length; axis++) {
            sum += (first[axis] - second[axis]) * (first[axis] - second[axis]);
        }
        return Math.sqrt(sum);
    }

    /** Rotations in coordinate planes, applied in order: each is [first axis, second axis, angle in degrees]. */
    static double[] rotate(double[] point, List<double[]> rotations) {
        double[] rotated = point.clone();
        for (double[] rotation : rotations) {
            int first = (int) rotation[0];
            int second = (int) rotation[1];
            double angle = Math.toRadians(rotation[2]);
            double a = rotated[first];
            double b = rotated[second];
            rotated[first] = a * Math.cos(angle) - b * Math.sin(angle);
            rotated[second] = a * Math.sin(angle) + b * Math.cos(angle);
        }
        return rotated;
    }

    static List<double[]> rotations(ComputationSettings settings, int dimension) {
        double[] flat = settings.vectorOr("rotations", dimension >= 4 ? new double[]{0, 3, 30, 1, 3, 20}
                : new double[]{0, 1, 30, 1, 2, 20});
        if (flat.length % 3 != 0) {
            throw new IllegalArgumentException("Rotations are triples [axis, axis, degrees].");
        }
        List<double[]> rotations = new ArrayList<>();
        for (int index = 0; index < flat.length; index += 3) {
            int first = (int) flat[index];
            int second = (int) flat[index + 1];
            if (first == second || first < 0 || second < 0 || first >= dimension || second >= dimension) {
                throw new IllegalArgumentException("A rotation names two different axes below " + dimension + ".");
            }
            rotations.add(new double[]{first, second, flat[index + 2]});
        }
        return rotations;
    }

    static SettingSpecification rotationSetting() {
        return SettingSpecification.vector("rotations", List.of(), 0, -360, 360,
                "Rotations as triples [axis i, axis j, degrees]; the default turns in the xw and yw planes");
    }

    public enum Projection { PERSPECTIVE, ORTHOGRAPHIC }

    public static class PolytopeProjection implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.POLYTOPE_PROJECTION;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof Polytope;
        }

        public List<SettingSpecification> settings() {
            return List.of(rotationSetting(),
                    SettingSpecification.choice("projection", Projection.PERSPECTIVE, "How each extra axis is dropped"),
                    SettingSpecification.number("distance", 3, 1.01, 1000, "Eye distance along each dropped axis"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return 1L << Math.min(20, object.definition().ambientDimension());
        }

        /** Drops axes one at a time from the last until three remain, by perspective from that axis or by discarding it. */
        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            Polytope polytope = (Polytope) object.definition();
            PolytopeSkeleton skeleton = PolytopeSkeleton.of(polytope);
            int dimension = skeleton.dimension();
            List<double[]> rotations = rotations(settings, dimension);
            Projection projection = settings.choice("projection", Projection.class);
            double distance = settings.number("distance");
            int shown = Math.min(3, dimension);
            List<double[]> projected = new ArrayList<>();
            List<Dataset.Feature> vertices = new ArrayList<>();
            for (int index = 0; index < skeleton.vertices().size(); index++) {
                double[] rotated = rotate(skeleton.vertices().get(index), rotations);
                double[] point = rotated;
                for (int dropped = dimension - 1; dropped >= shown; dropped--) {
                    double scale = projection == Projection.PERSPECTIVE ? distance / (distance - point[dropped]) : 1;
                    double[] lower = new double[dropped];
                    for (int axis = 0; axis < dropped; axis++) {
                        lower[axis] = point[axis] * scale;
                    }
                    point = lower;
                }
                projected.add(point);
                Map<String, Object> facts = new LinkedHashMap<>();
                facts.put("coordinates", skeleton.vertices().get(index));
                facts.put("rotated", rotated);
                vertices.add(new Dataset.Feature(point, null, "VERTEX", facts));
            }
            List<Dataset.Polyline> edges = new ArrayList<>();
            for (int[] edge : skeleton.edges()) {
                float[] points = new float[shown * 2];
                for (int axis = 0; axis < shown; axis++) {
                    points[axis] = (float) projected.get(edge[0])[axis];
                    points[shown + axis] = (float) projected.get(edge[1])[axis];
                }
                edges.add(new Dataset.Polyline(points, null, edge[0] + "-" + edge[1], false));
            }
            return new Dataset.Composite(Map.of("edges", new Dataset.Polylines(shown, edges, new double[shown], new double[shown]),
                    "vertices", new Dataset.FeaturePoints(vertices)));
        }
    }

    /**
     * The intersection of the rotated polytope's edges with the hyperplane x_last = c: as c moves, a tesseract's
     * slice grows from a point to a cube and back.
     */
    public static class PolytopeSlice implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.POLYTOPE_SLICE;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof Polytope polytope && polytope.dimension() >= 3;
        }

        public List<SettingSpecification> settings() {
            return List.of(rotationSetting(), SettingSpecification.number("offset", 0, -1e6, 1e6, "Where the hyperplane cuts the last axis"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return 1L << Math.min(20, object.definition().ambientDimension());
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            PolytopeSkeleton skeleton = PolytopeSkeleton.of((Polytope) object.definition());
            int dimension = skeleton.dimension();
            List<double[]> rotations = rotations(settings, dimension);
            double offset = settings.number("offset");
            List<double[]> rotated = skeleton.vertices().stream().map(vertex -> rotate(vertex, rotations)).toList();
            int last = dimension - 1;
            List<Float> points = new ArrayList<>();
            for (int[] edge : skeleton.edges()) {
                double[] first = rotated.get(edge[0]);
                double[] second = rotated.get(edge[1]);
                double a = first[last] - offset;
                double b = second[last] - offset;
                if ((a > 0) == (b > 0) || a == b) {
                    continue;
                }
                double fraction = a / (a - b);
                for (int axis = 0; axis < last; axis++) {
                    points.add((float) (first[axis] + fraction * (second[axis] - first[axis])));
                }
            }
            float[] flat = new float[points.size()];
            for (int index = 0; index < flat.length; index++) {
                flat[index] = points.get(index);
            }
            return new Dataset.PointCloud(last, flat, null, null);
        }
    }
}
