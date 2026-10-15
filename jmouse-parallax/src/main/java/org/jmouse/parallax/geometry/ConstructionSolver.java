package org.jmouse.parallax.geometry;

import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition.GeometricConstruction;
import org.jmouse.parallax.object.ObjectDefinition.GeometricElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Solves a construction in order: every element is computed from the ones it references, with the parameters'
 * current values, so moving a free point moves everything built on it. An element whose construction has no
 * answer (parallel lines meeting) is reported undefined, and so is everything built on it.
 */
final class ConstructionSolver {

    sealed interface Solved permits SolvedPoint, SolvedLine, SolvedCircle, SolvedPlane, SolvedSphere, SolvedPolygon, Undefined {
    }

    /** A closed polygon through its corners, in order. */
    record SolvedPolygon(List<double[]> corners) implements Solved {
    }

    record SolvedPoint(double[] position) implements Solved {
    }

    /** A line, segment or ray through `origin` along `direction`; a segment ends at origin + direction. */
    record SolvedLine(double[] origin, double[] direction, String form) implements Solved {
    }

    record SolvedCircle(double[] center, double radius, double[] normal) implements Solved {
    }

    record SolvedPlane(double[] point, double[] normal) implements Solved {
    }

    record SolvedSphere(double[] center, double radius) implements Solved {
    }

    record Undefined(String reason) implements Solved {
    }

    record Solution(Dataset.FeaturePoints features, Dataset.Polylines drawing) {
    }

    Solution solve(CompiledObject object, GeometricConstruction construction, double extent) {
        int dimension = construction.dimension();
        Map<String, Solved> solved = new HashMap<>();
        List<Dataset.Feature> features = new ArrayList<>();
        List<Dataset.Polyline> lines = new ArrayList<>();
        // An unnamed element another one is built from (the block a rotation turns) is a blank: it is not drawn itself.
        java.util.Set<String> referenced = new java.util.HashSet<>();
        construction.elements().forEach(element -> referenced.addAll(element.references()));
        for (GeometricElement element : construction.elements()) {
            Solved result = solveOne(object, element, solved, dimension);
            solved.put(element.id(), result);
            features.add(feature(element, result));
            if (!("".equals(element.label()) && referenced.contains(element.id()))) {
                draw(element, result, extent, dimension, lines);
            }
        }
        double[] minimum = new double[dimension];
        double[] maximum = new double[dimension];
        java.util.Arrays.fill(minimum, -extent);
        java.util.Arrays.fill(maximum, extent);
        return new Solution(new Dataset.FeaturePoints(features), new Dataset.Polylines(dimension, lines, minimum, maximum));
    }

    private Solved solveOne(CompiledObject object, GeometricElement element, Map<String, Solved> solved, int dimension) {
        List<Solved> inputs = element.references().stream().map(solved::get).toList();
        for (Solved input : inputs) {
            if (input instanceof Undefined) {
                return new Undefined("built on an undefined element");
            }
        }
        try {
            return switch (element.type()) {
                case POINT -> new SolvedPoint(element.coordinates().stream().mapToDouble(object::constant).toArray());
                case MIDPOINT -> new SolvedPoint(scale(add(point(inputs, 0), point(inputs, 1)), 0.5));
                case LINE, SEGMENT, RAY, VECTOR -> new SolvedLine(point(inputs, 0), subtract(point(inputs, 1), point(inputs, 0)),
                        element.type().name());
                // circle(O, A) through a point, or circle(O, r) by its radius
                case CIRCLE -> inputs.size() == 1 && element.coordinates().size() == 1
                        ? new SolvedCircle(point(inputs, 0), object.constant(element.coordinates().getFirst()), planeNormal(dimension))
                        : new SolvedCircle(point(inputs, 0), norm(subtract(point(inputs, 1), point(inputs, 0))), planeNormal(dimension));
                case POLYGON -> new SolvedPolygon(java.util.stream.IntStream.range(0, inputs.size()).mapToObj(index -> point(inputs, index)).toList());
                case ROTATION -> rotated(inputs.get(0), point(inputs, 1), object.constant(element.coordinates().getFirst()));
                case CIRCLE_THROUGH_THREE -> circleThrough(point(inputs, 0), point(inputs, 1), point(inputs, 2));
                case PLANE -> new SolvedPlane(point(inputs, 0), cross(subtract(point(inputs, 1), point(inputs, 0)),
                        subtract(point(inputs, 2), point(inputs, 0))));
                case SPHERE -> new SolvedSphere(point(inputs, 0), norm(subtract(point(inputs, 1), point(inputs, 0))));
                case INTERSECTION -> intersection(inputs, element.coordinates().isEmpty() ? 0
                        : (int) Math.round(object.constant(element.coordinates().getFirst())));
                case PERPENDICULAR -> perpendicular(line(inputs, 0), point(inputs, 1), dimension);
                case PARALLEL -> new SolvedLine(point(inputs, 1), line(inputs, 0).direction(), "LINE");
                case TANGENT -> tangent(circle(inputs, 0), point(inputs, 1));
                case REFLECTION -> reflection(point(inputs, 0), line(inputs, 1));
            };
        } catch (IllegalArgumentException | ClassCastException | IndexOutOfBoundsException mismatch) {
            return new Undefined(element.type() + " cannot be built from " + element.references() + ": " + mismatch.getMessage());
        }
    }

    /** A solved element turned in the plane about a centre by an angle; a 3-D element turns about the z axis through it. */
    private static Solved rotated(Solved shape, double[] centre, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        java.util.function.UnaryOperator<double[]> turnPoint = point -> {
            double[] turned = point.clone();
            double x = point[0] - centre[0];
            double y = point[1] - centre[1];
            turned[0] = centre[0] + x * cos - y * sin;
            turned[1] = centre[1] + x * sin + y * cos;
            return turned;
        };
        java.util.function.UnaryOperator<double[]> turnDirection = direction -> {
            double[] turned = direction.clone();
            turned[0] = direction[0] * cos - direction[1] * sin;
            turned[1] = direction[0] * sin + direction[1] * cos;
            return turned;
        };
        return switch (shape) {
            case SolvedPoint point -> new SolvedPoint(turnPoint.apply(point.position()));
            case SolvedLine line -> new SolvedLine(turnPoint.apply(line.origin()), turnDirection.apply(line.direction()), line.form());
            case SolvedCircle circle -> new SolvedCircle(turnPoint.apply(circle.center()), circle.radius(), circle.normal());
            case SolvedPolygon polygon -> new SolvedPolygon(polygon.corners().stream().map(turnPoint).toList());
            case SolvedPlane plane -> new Undefined("a plane is not turned");
            case SolvedSphere sphere -> new SolvedSphere(turnPoint.apply(sphere.center()), sphere.radius());
            case Undefined undefined -> undefined;
        };
    }

    private static double[] point(List<Solved> inputs, int index) {
        if (inputs.get(index) instanceof SolvedPoint point) {
            return point.position();
        }
        throw new IllegalArgumentException("reference " + (index + 1) + " must be a point");
    }

    private static SolvedLine line(List<Solved> inputs, int index) {
        if (inputs.get(index) instanceof SolvedLine line) {
            return line;
        }
        throw new IllegalArgumentException("reference " + (index + 1) + " must be a line");
    }

    private static SolvedCircle circle(List<Solved> inputs, int index) {
        if (inputs.get(index) instanceof SolvedCircle circle) {
            return circle;
        }
        throw new IllegalArgumentException("reference " + (index + 1) + " must be a circle");
    }

    private static Solved circleThrough(double[] a, double[] b, double[] c) {
        double ax = a[0];
        double ay = a[1];
        double bx = b[0];
        double by = b[1];
        double cx = c[0];
        double cy = c[1];
        double denominator = 2 * (ax * (by - cy) + bx * (cy - ay) + cx * (ay - by));
        if (Math.abs(denominator) < 1e-12) {
            return new Undefined("the three points are collinear");
        }
        double ux = ((ax * ax + ay * ay) * (by - cy) + (bx * bx + by * by) * (cy - ay) + (cx * cx + cy * cy) * (ay - by)) / denominator;
        double uy = ((ax * ax + ay * ay) * (cx - bx) + (bx * bx + by * by) * (ax - cx) + (cx * cx + cy * cy) * (bx - ax)) / denominator;
        double[] center = a.length == 3 ? new double[]{ux, uy, a[2]} : new double[]{ux, uy};
        return new SolvedCircle(center, norm(subtract(a, center)), planeNormal(a.length));
    }

    private static Solved intersection(List<Solved> inputs, int which) {
        Solved first = inputs.get(0);
        Solved second = inputs.get(1);
        if (first instanceof SolvedLine one && second instanceof SolvedLine two) {
            double[] d1 = one.direction();
            double[] d2 = two.direction();
            double determinant = d1[0] * -d2[1] - d1[1] * -d2[0];
            if (Math.abs(determinant) < 1e-12) {
                return new Undefined("the lines are parallel");
            }
            double[] difference = subtract(two.origin(), one.origin());
            double t = (difference[0] * -d2[1] - difference[1] * -d2[0]) / determinant;
            return new SolvedPoint(add(one.origin(), scale(d1, t)));
        }
        if (first instanceof SolvedCircle circle && second instanceof SolvedLine line) {
            return lineCircle(line, circle, which);
        }
        if (first instanceof SolvedLine line && second instanceof SolvedCircle circle) {
            return lineCircle(line, circle, which);
        }
        if (first instanceof SolvedCircle one && second instanceof SolvedCircle two) {
            double[] between = subtract(two.center(), one.center());
            double distance = norm(between);
            if (distance < 1e-12 || distance > one.radius() + two.radius() || distance < Math.abs(one.radius() - two.radius())) {
                return new Undefined("the circles do not meet");
            }
            double along = (one.radius() * one.radius() - two.radius() * two.radius() + distance * distance) / (2 * distance);
            double height = Math.sqrt(Math.max(0, one.radius() * one.radius() - along * along));
            double[] base = add(one.center(), scale(between, along / distance));
            double[] across = scale(new double[]{-between[1], between[0]}, height / distance);
            double[] offset = between.length == 3 ? new double[]{across[0], across[1], 0} : across;
            return new SolvedPoint(which == 0 ? add(base, offset) : subtract(base, offset));
        }
        return new Undefined("this pair of elements has no intersection rule");
    }

    private static Solved lineCircle(SolvedLine line, SolvedCircle circle, int which) {
        double[] direction = line.direction();
        double[] toOrigin = subtract(line.origin(), circle.center());
        double a = dot(direction, direction);
        double b = 2 * dot(direction, toOrigin);
        double c = dot(toOrigin, toOrigin) - circle.radius() * circle.radius();
        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) {
            return new Undefined("the line misses the circle");
        }
        double root = Math.sqrt(discriminant);
        double t = which == 0 ? (-b - root) / (2 * a) : (-b + root) / (2 * a);
        return new SolvedPoint(add(line.origin(), scale(direction, t)));
    }

    private static Solved perpendicular(SolvedLine line, double[] through, int dimension) {
        double[] direction = line.direction();
        if (dimension == 2) {
            return new SolvedLine(through, new double[]{-direction[1], direction[0]}, "LINE");
        }
        double[] foot = projectOnLine(through, line);
        double[] toward = subtract(through, foot);
        if (norm(toward) < 1e-12) {
            return new Undefined("the point lies on the line; a perpendicular in space is not unique");
        }
        return new SolvedLine(foot, toward, "LINE");
    }

    private static Solved tangent(SolvedCircle circle, double[] near) {
        double[] radial = subtract(near, circle.center());
        double length = norm(radial);
        if (length < 1e-12) {
            return new Undefined("the point is the centre");
        }
        double[] touching = add(circle.center(), scale(radial, circle.radius() / length));
        double[] direction = radial.length == 3 ? cross(circle.normal(), radial) : new double[]{-radial[1], radial[0]};
        return new SolvedLine(touching, direction, "LINE");
    }

    private static Solved reflection(double[] point, SolvedLine line) {
        double[] foot = projectOnLine(point, line);
        return new SolvedPoint(subtract(scale(foot, 2), point));
    }

    private static double[] projectOnLine(double[] point, SolvedLine line) {
        double[] direction = line.direction();
        double t = dot(subtract(point, line.origin()), direction) / dot(direction, direction);
        return add(line.origin(), scale(direction, t));
    }

    private static Dataset.Feature feature(GeometricElement element, Solved result) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("id", element.id());
        facts.put("type", element.type().name());
        if (element.label() != null) {
            facts.put("label", element.label());
        }
        double[] position = null;
        switch (result) {
            case SolvedPoint point -> position = point.position();
            case SolvedLine line -> {
                position = line.origin();
                facts.put("direction", line.direction());
            }
            case SolvedCircle circle -> {
                position = circle.center();
                facts.put("radius", circle.radius());
            }
            case SolvedPlane plane -> {
                position = plane.point();
                facts.put("normal", plane.normal());
            }
            case SolvedSphere sphere -> {
                position = sphere.center();
                facts.put("radius", sphere.radius());
            }
            case SolvedPolygon polygon -> {
                position = polygon.corners().getFirst();
                facts.put("corners", polygon.corners().size());
            }
            case Undefined undefined -> facts.put("undefined", undefined.reason());
        }
        return new Dataset.Feature(position, null, result instanceof Undefined ? "UNDEFINED" : element.type().name(), facts);
    }

    private static void draw(GeometricElement element, Solved result, double extent, int dimension, List<Dataset.Polyline> lines) {
        switch (result) {
            case SolvedLine line -> {
                double[] direction = line.direction();
                double length = norm(direction);
                if (length < 1e-12) {
                    return;
                }
                double[] start;
                double[] end;
                switch (line.form()) {
                    case "SEGMENT", "VECTOR" -> {
                        start = line.origin();
                        end = add(line.origin(), direction);
                    }
                    case "RAY" -> {
                        start = line.origin();
                        end = add(line.origin(), scale(direction, extent * 2 / length));
                    }
                    default -> {
                        start = add(line.origin(), scale(direction, -extent * 2 / length));
                        end = add(line.origin(), scale(direction, extent * 2 / length));
                    }
                }
                lines.add(new Dataset.Polyline(flatten(List.of(start, end)), null, element.label() != null ? element.label() : element.id(), false));
                if (line.form().equals("VECTOR") && dimension == 2) {
                    lines.add(new Dataset.Polyline(flatten(arrowHead(end, direction, length)), null, element.label() != null ? element.label() : element.id(), false));
                }
            }
            case SolvedPolygon polygon -> {
                List<double[]> corners = new ArrayList<>(polygon.corners());
                corners.add(polygon.corners().getFirst());
                lines.add(new Dataset.Polyline(flatten(corners), null, element.label() != null ? element.label() : element.id(), true));
            }
            case SolvedCircle circle -> {
                List<double[]> points = new ArrayList<>();
                for (int index = 0; index <= 128; index++) {
                    double angle = 2 * Math.PI * index / 128;
                    double[] point = circle.center().clone();
                    point[0] += circle.radius() * Math.cos(angle);
                    point[1] += circle.radius() * Math.sin(angle);
                    points.add(point);
                }
                lines.add(new Dataset.Polyline(flatten(points), null, element.label() != null ? element.label() : element.id(), true));
            }
            default -> {
            }
        }
    }

    /** The head of a vector: two strokes back from its tip, a sixth of its length, never longer than 0.6 units. */
    private static List<double[]> arrowHead(double[] tip, double[] direction, double length) {
        double size = Math.min(0.6, length / 6);
        double backX = -direction[0] / length * size;
        double backY = -direction[1] / length * size;
        double cos = Math.cos(Math.PI / 7);
        double sin = Math.sin(Math.PI / 7);
        double[] left = {tip[0] + backX * cos - backY * sin, tip[1] + backX * sin + backY * cos};
        double[] right = {tip[0] + backX * cos + backY * sin, tip[1] - backX * sin + backY * cos};
        return List.of(left, tip, right);
    }

    private static float[] flatten(List<double[]> points) {
        int dimension = points.getFirst().length;
        float[] flat = new float[points.size() * dimension];
        for (int index = 0; index < points.size(); index++) {
            for (int axis = 0; axis < dimension; axis++) {
                flat[index * dimension + axis] = (float) points.get(index)[axis];
            }
        }
        return flat;
    }

    private static double[] planeNormal(int dimension) {
        return dimension == 3 ? new double[]{0, 0, 1} : null;
    }

    static double[] add(double[] first, double[] second) {
        double[] result = new double[first.length];
        for (int index = 0; index < first.length; index++) {
            result[index] = first[index] + second[index];
        }
        return result;
    }

    static double[] subtract(double[] first, double[] second) {
        double[] result = new double[first.length];
        for (int index = 0; index < first.length; index++) {
            result[index] = first[index] - second[index];
        }
        return result;
    }

    static double[] scale(double[] vector, double factor) {
        double[] result = new double[vector.length];
        for (int index = 0; index < vector.length; index++) {
            result[index] = vector[index] * factor;
        }
        return result;
    }

    static double dot(double[] first, double[] second) {
        double sum = 0;
        for (int index = 0; index < first.length; index++) {
            sum += first[index] * second[index];
        }
        return sum;
    }

    static double norm(double[] vector) {
        return Math.sqrt(dot(vector, vector));
    }

    static double[] cross(double[] first, double[] second) {
        if (first.length != 3) {
            throw new IllegalArgumentException("a plane needs three dimensions");
        }
        return new double[]{first[1] * second[2] - first[2] * second[1], first[2] * second[0] - first[0] * second[2],
                first[0] * second[1] - first[1] * second[0]};
    }
}
