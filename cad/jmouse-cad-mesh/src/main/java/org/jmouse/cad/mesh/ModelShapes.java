package org.jmouse.cad.mesh;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.WordNode;
import org.jmouse.mesh.Mesh;
import org.jmouse.mesh.MeshBuilder;
import org.jmouse.mesh.MeshMaterial;
import org.jmouse.mesh.Placement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 🧱 A model drawn here from shapes — boxes, cylinders, prisms and turned bodies, each of a material —, as a table of packages writes one:
 * read from the model document's shape lines ({@link ModelLinesParser}) and made a mesh in
 * millimetres with Z up, the frame a footprint sets its model in. A model of files has none.
 */
public final class ModelShapes {

    /** One shape: its material, where its centre stands, and how it is turned — degrees about X, then Y, then Z. */
    public sealed interface Shape {

        String material();

        List<Double> at();

        List<Double> rotate();
    }

    public record Box(String material, List<Double> size, List<Double> at, List<Double> rotate) implements Shape {
    }

    /** Along Z before it is turned. */
    public record Cylinder(String material, double radius, double length, List<Double> at, List<Double> rotate) implements Shape {
    }

    /** A body turned about Z: its profile, radius and z from the bottom up, swept from and to its angles, degrees. */
    public record Revolved(String material, List<double[]> profile, double from, double to, List<Double> at, List<Double> rotate) implements Shape {
    }

    /** A convex outline of {@code [x, y]} corners raised along Z by {@code height} from its base, where {@code at} sets it. */
    public record Prism(String material, List<double[]> outline, double height, List<Double> at, List<Double> rotate) implements Shape {
    }

    private static final List<Double> NONE = List.of(0.0, 0.0, 0.0);

    /** The lines that draw a shape: every other line of a model says something else of it. */
    private static final Set<LineGrammar> SHAPES = Set.of(ModelLinesParser.BOX, ModelLinesParser.CYLINDER, ModelLinesParser.PRISM,
            ModelLinesParser.REVOLVED);

    private ModelShapes() {
    }

    /** The shapes a model document draws, in their order; none for a model of files. */
    public static List<Shape> of(DocumentNode model) {
        return model.getExpressions().stream()
                .filter(statement -> statement instanceof ClauseLineNode line && SHAPES.contains(line.grammar()))
                .map(statement -> shapeOf((ClauseLineNode) statement))
                .toList();
    }

    /** The shapes as one body, each in its material. */
    public static Mesh mesh(List<Shape> shapes, ModelMaterials materials) {
        MeshBuilder builder = new MeshBuilder();
        for (Shape shape : shapes) {
            MeshMaterial material = materials.material(shape.material())
                    .orElseThrow(() -> new IllegalArgumentException("No material is called " + shape.material()));
            Placement placement = placementOf(shape);
            switch (shape) {
                case Box box -> builder.box(material, placement, box.size().get(0), box.size().get(1), box.size().get(2));
                case Cylinder cylinder -> builder.cylinder(material, placement, cylinder.radius(), cylinder.length(), materials.steps());
                case Prism prism -> builder.prism(material, placement, prism.outline(), prism.height());
                case Revolved revolved -> builder.revolved(material, placement, revolved.profile(), revolved.from(), revolved.to(), materials.steps());
            }
        }
        return builder.build();
    }

    /** Where a shape stands in its model: turned about X, then Y, then Z, about its own centre, then moved to its place. */
    public static Placement placementOf(Shape shape) {
        return Placement.translation(shape.at().get(0), shape.at().get(1), shape.at().get(2))
                .then(about(0, 0, 1, shape.rotate().get(2)))
                .then(about(0, 1, 0, shape.rotate().get(1)))
                .then(about(1, 0, 0, shape.rotate().get(0)));
    }

    private static Shape shapeOf(ClauseLineNode line) {
        String material = ((WordNode) line.slot("material").orElseThrow()).word();
        List<Double> at = line.has("at") ? line.quantities("at") : NONE;
        List<Double> rotate = line.has("rotate") ? line.quantities("rotate") : NONE;
        if (line.grammar() == ModelLinesParser.BOX) {
            return new Box(material, line.quantities("size"), at, rotate);
        }
        if (line.grammar() == ModelLinesParser.REVOLVED) {
            return new Revolved(material, pairs(line.quantities("profile")), line.quantity("from").orElse(0.0), line.quantity("to").orElse(360.0), at, rotate);
        }
        if (line.grammar() == ModelLinesParser.PRISM) {
            return new Prism(material, pairs(line.quantities("outline")), line.quantity("height").orElseThrow(), at, rotate);
        }
        return new Cylinder(material, line.quantity("radius").orElseThrow(), line.quantity("length").orElseThrow(), at, rotate);
    }

    /** Written numbers taken two at a time: an outline's x, y or a profile's radius, z. */
    private static List<double[]> pairs(List<Double> written) {
        List<double[]> pairs = new ArrayList<>();
        for (int index = 0; index + 1 < written.size(); index += 2) {
            pairs.add(new double[] {written.get(index), written.get(index + 1)});
        }
        return pairs;
    }

    private static Placement about(double x, double y, double z, double degrees) {
        return Placement.rotation(new double[] {x, y, z, Math.toRadians(degrees)});
    }
}
