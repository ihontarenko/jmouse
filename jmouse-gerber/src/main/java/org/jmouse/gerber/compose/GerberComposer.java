package org.jmouse.gerber.compose;

import org.jmouse.gerber.GerberFile;
import org.jmouse.gerber.GerberPoint;
import org.jmouse.gerber.GerberPosition;
import org.jmouse.gerber.GerberShape;
import org.jmouse.gerber.node.ApertureDefinition;
import org.jmouse.gerber.node.Attribute;
import org.jmouse.gerber.node.AttributeScope;
import org.jmouse.gerber.node.EndOfFile;
import org.jmouse.gerber.node.FormatSpecification;
import org.jmouse.gerber.node.GerberCommand;
import org.jmouse.gerber.node.GerberDocument;
import org.jmouse.gerber.node.Interpolation;
import org.jmouse.gerber.node.InterpolationMode;
import org.jmouse.gerber.node.LoadPolarity;
import org.jmouse.gerber.node.Operation;
import org.jmouse.gerber.node.OperationKind;
import org.jmouse.gerber.node.QuadrantMode;
import org.jmouse.gerber.node.Region;
import org.jmouse.gerber.node.SelectAperture;
import org.jmouse.gerber.node.UnitMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Composes a Gerber X2 file (The Gerber Layer Format Specification, Ucamco, revision 2024.05) as a board house reads
 * it — millimetres, six decimals, multi-quadrant arcs, the file's function and polarity as attributes, every aperture
 * once with what it is for — out of drawing operations: a flash, a line, an arc, a region. It builds the
 * {@link GerberDocument}, never text: the modal state a file carries — the aperture in hand, the polarity, where the
 * pen is — decides which commands the tree needs, and the text is the tree's {@code toSource()}.
 *
 * <p>The same file as {@code @jmouse/gerber}'s writer gives, command for command, so a board written by either side
 * is the same file.</p>
 */
public final class GerberComposer {

    private static final FormatSpecification FORMAT = FormatSpecification.NANOMETRES;

    private final GerberFile                     file;
    private final Map<String, ApertureDefinition> apertures   = new LinkedHashMap<>();
    private final List<GerberCommand>            definitions = new ArrayList<>();
    private final List<GerberCommand>            image       = new ArrayList<>();
    private       Integer                        current;
    private       boolean                        dark        = true;
    private       GerberPoint                    at;

    public GerberComposer(GerberFile file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    /**
     * The aperture of this shape for this purpose ({@code %TA.AperFunction}: {@code SMDPad,CuDef}, {@code Conductor},
     * {@code ViaPad} …), defined once; its D code.
     *
     * @param purpose the aperture function's values, comma-separated; null for none
     */
    public int aperture(GerberShape shape, String purpose) {
        String key = shape.template() + "|" + (purpose == null ? "" : purpose);
        ApertureDefinition known = apertures.get(key);
        if (known != null) {
            return known.code();
        }
        ApertureDefinition definition = new ApertureDefinition(ApertureDefinition.FIRST_CODE + apertures.size(), shape,
                GerberPosition.NONE);
        apertures.put(key, definition);
        if (purpose != null) {
            definitions.add(Attribute.of(AttributeScope.APERTURE, ".AperFunction", purpose.split(",")));
        }
        definitions.add(definition);
        if (purpose != null) {
            definitions.add(Attribute.deleteAll());
        }
        return definition.code();
    }

    /** What follows is drawn dark (material) or clear (taken away from what is under it). */
    public GerberComposer polarity(boolean dark) {
        if (dark != this.dark) {
            this.dark = dark;
            image.add(new LoadPolarity(dark, GerberPosition.NONE));
        }
        return this;
    }

    /** The aperture stamped once at a point: a pad, a via. */
    public GerberComposer flash(int aperture, GerberPoint point) {
        select(aperture);
        image.add(operation(point, OperationKind.FLASH));
        at = point;
        return this;
    }

    /** A line through the points, drawn with a circular aperture: a track, an outline. */
    public GerberComposer line(int aperture, List<GerberPoint> points) {
        if (points.size() < 2) {
            return this;
        }
        select(aperture);
        moveTo(points.getFirst(), image);
        for (GerberPoint point : points.subList(1, points.size())) {
            image.add(operation(point, OperationKind.DRAW));
        }
        at = points.getLast();
        return this;
    }

    /**
     * An arc from a point to a point about a centre, clockwise as the coordinates run or not, drawn with a circular
     * aperture. It returns to linear drawing itself, so every other command is drawn in G01.
     */
    public GerberComposer arc(int aperture, GerberPoint from, GerberPoint to, GerberPoint centre, boolean clockwise) {
        select(aperture);
        moveTo(from, image);
        image.add(new QuadrantMode(true, GerberPosition.NONE));
        image.add(new InterpolationMode(clockwise ? Interpolation.CLOCKWISE : Interpolation.COUNTERCLOCKWISE,
                GerberPosition.NONE));
        image.add(Operation.arcTo(FORMAT.coordinate(to.x()), FORMAT.coordinate(to.y()),
                FORMAT.coordinate(centre.x() - from.x()), FORMAT.coordinate(centre.y() - from.y())));
        image.add(new InterpolationMode(Interpolation.LINEAR, GerberPosition.NONE));
        at = to;
        return this;
    }

    /**
     * An area: each closed contour filled, in the polarity in hand. Contours of one region may not cross; a hole is a
     * contour of its own drawn clear after the area, as {@code polarity(false)} gives.
     */
    public GerberComposer region(List<List<GerberPoint>> contours) {
        List<List<GerberPoint>> closed = contours.stream().filter(contour -> contour.size() >= 3).toList();
        if (closed.isEmpty()) {
            return this;
        }
        List<GerberCommand> edges = new ArrayList<>();
        for (List<GerberPoint> contour : closed) {
            at = null;
            moveTo(contour.getFirst(), edges);
            for (GerberPoint point : contour.subList(1, contour.size())) {
                edges.add(operation(point, OperationKind.DRAW));
            }
            if (!contour.getFirst().equals(contour.getLast())) {
                edges.add(operation(contour.getFirst(), OperationKind.DRAW));
            }
        }
        image.add(Region.of(edges));
        at = null;
        return this;
    }

    /** The whole file: attributes, format, units, apertures, the image, the end. */
    public GerberDocument document() {
        List<GerberCommand> commands = new ArrayList<>();
        commands.add(Attribute.of(AttributeScope.FILE, ".FileFunction", file.function().split(",")));
        commands.add(Attribute.of(AttributeScope.FILE, ".FilePolarity", file.positive() ? "Positive" : "Negative"));
        if (!file.software().isEmpty()) {
            commands.add(Attribute.of(AttributeScope.FILE, ".GenerationSoftware", file.software().toArray(String[]::new)));
        }
        file.project().ifPresent(project -> commands.add(Attribute.of(AttributeScope.FILE, ".ProjectId", project, "", "")));
        commands.add(FORMAT);
        commands.add(new UnitMode(true, GerberPosition.NONE));
        commands.add(new LoadPolarity(true, GerberPosition.NONE));
        commands.add(new InterpolationMode(Interpolation.LINEAR, GerberPosition.NONE));
        commands.addAll(definitions);
        commands.addAll(image);
        commands.add(new EndOfFile(GerberPosition.NONE));
        return GerberDocument.of(commands);
    }

    private void select(int aperture) {
        if (current == null || current != aperture) {
            current = aperture;
            image.add(new SelectAperture(aperture, GerberPosition.NONE));
        }
    }

    private void moveTo(GerberPoint point, List<GerberCommand> into) {
        if (!point.equals(at)) {
            into.add(operation(point, OperationKind.MOVE));
        }
        at = point;
    }

    private static Operation operation(GerberPoint point, OperationKind kind) {
        return Operation.at(FORMAT.coordinate(point.x()), FORMAT.coordinate(point.y()), kind);
    }
}
