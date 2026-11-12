package org.jmouse.kicad.footprint;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadKeyword;
import org.jmouse.sexpression.node.SExpressionAtom;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.sexpression.node.SExpressionNode;
import org.jmouse.sexpression.node.SExpressionString;

import java.util.ArrayList;
import java.util.List;

import java.util.Optional;

/**
 * A footprint's 3D model — {@code (model "${KICAD8_3DMODEL_DIR}/Package_SO.3dshapes/SOIC-8_3.9x4.9mm_P1.27mm.wrl" …)} —:
 * which file shows the part's body, and how it is moved, scaled and turned onto the footprint.
 *
 * @param path     the file as KiCad writes it, its path variable unresolved
 * @param offset   millimetres from the footprint's origin, Z upwards
 * @param scale    each axis' factor
 * @param rotation degrees about each axis
 * @param hidden   whether KiCad shows it
 */
public record KiCadModel(String path, Axes offset, Axes scale, Axes rotation, boolean hidden) {

    /** Millimetres in KiCad 5's inch offset {@code (at (xyz …))}. */
    private static final double MILLIMETRES_PER_INCH = 25.4;

    /** Three values, one for each axis. */
    public record Axes(double x, double y, double z) {

        public static final Axes ZERO = new Axes(0, 0, 0);
        public static final Axes ONE = new Axes(1, 1, 1);

        public Axes times(double factor) {
            return new Axes(x * factor, y * factor, z * factor);
        }
    }

    /** A {@code (model "PATH" (offset (xyz …)) (scale (xyz …)) (rotate (xyz …)) [(hide yes)])} list. */
    /** The model as a footprint writes it, millimetres in {@code offset}: the list {@link #of} reads back. */
    public SExpressionList written() {
        List<SExpressionNode> items = new ArrayList<>();
        items.add(SExpressionString.of(path));
        if (hidden) {
            items.add(SExpressionAtom.of(HIDE.word()));
        }
        items.add(axesWritten(OFFSET, offset));
        items.add(axesWritten(SCALE, scale));
        items.add(axesWritten(ROTATE, rotation));
        return SExpressionList.of(MODEL, items.toArray(SExpressionNode[]::new));
    }

    private static SExpressionList axesWritten(KiCadKeyword name, Axes axes) {
        return SExpressionList.of(name, SExpressionList.of(XYZ, SExpressionAtom.of(axes.x()), SExpressionAtom.of(axes.y()),
                SExpressionAtom.of(axes.z())));
    }

    public static KiCadModel of(SExpressionList model) {
        Axes offset = axes(model, OFFSET)
                .or(() -> axes(model, AT).map(inches -> inches.times(MILLIMETRES_PER_INCH)))
                .orElse(Axes.ZERO);
        return new KiCadModel(model.word(1), offset, axes(model, SCALE).orElse(Axes.ONE),
                axes(model, ROTATE).orElse(Axes.ZERO), model.says(HIDE));
    }

    /**
     * The library file it names, its path variable and extension dropped — {@code Package_SO.3dshapes/SOIC-8_3.9x4.9mm_P1.27mm}
     * —: one model, whichever of its formats is read.
     */
    public String libraryPath() {
        String written = path.replace('\\', '/');
        if (written.startsWith("${")) {
            int close = written.indexOf('}');
            written = close < 0 ? written : written.substring(close + 1);
        }
        while (written.startsWith("/")) {
            written = written.substring(1);
        }
        int slash = written.lastIndexOf('/');
        int dot = written.lastIndexOf('.');
        return dot > slash ? written.substring(0, dot) : written;
    }

    private static Optional<Axes> axes(SExpressionList model, KiCadKeyword name) {
        return model.list(name).flatMap(list -> list.list(XYZ))
                .map(xyz -> new Axes(xyz.number(1), xyz.number(2), xyz.number(3)));
    }
}
