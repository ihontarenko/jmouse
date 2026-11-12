package org.jmouse.cad.mesh;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.mesh.MeshFormats;
import org.jmouse.mesh.Placement;

import java.util.List;
import java.util.Optional;

/**
 * Where a part's 3D model stands over a pcb, in millimetres with Z up and the pcb's Y — which grows downwards on
 * the drawing — turned upwards: the frame a pcb is modelled in, for a viewer and for an exchange file alike.
 */
public final class ModelPlacement {

    private ModelPlacement() {
    }

    /**
     * A model file in millimetres with Z up: scaled by the millimetres one of its units is, and stood up from Y when it
     * stands up along Y — its line's own, else its format's standard.
     */
    public static Placement ofFile(ModelFile file, MeshFormats.MeshFormat format) {
        double unit = Optional.ofNullable(file.scale()).orElse(format.unit());
        MeshFormats.Axis up = Optional.ofNullable(file.up()).orElse(format.up());
        Placement scaled = Placement.scale(unit, unit, unit);
        return up == MeshFormats.Axis.Y ? new Placement(new double[] {1, 0, 0, 0, 0, 0, -1, 0, 0, 1, 0, 0}).then(scaled) : scaled;
    }

    /**
     * A model, already in millimetres with Z up, where its part stands: set on the footprint as the model line says —
     * scaled, turned about X, then Y, then Z, and moved, in the footprint's frame —, turned over through the pcb for a
     * part on the bottom, then turned and moved with the part.
     *
     * @param surface the height its side's face is at: the pcb's top for a part on the top, its bottom else
     */
    public static Placement onBoard(PcbDrawing.Placement part, FootprintDrawing.Model model, double surface) {
        Placement set = Placement.translation(model.offset().get(0), model.offset().get(1), model.offset().get(2))
                .then(about(0, 0, 1, model.rotation().get(2)))
                .then(about(0, 1, 0, model.rotation().get(1)))
                .then(about(1, 0, 0, model.rotation().get(0)))
                .then(Placement.scale(model.scale().get(0), model.scale().get(1), model.scale().get(2)));
        Placement sided = part.onBottom() ? Placement.scale(-1, 1, -1).then(set) : set;
        return Placement.translation(part.x(), -part.y(), surface).then(about(0, 0, 1, part.rotation())).then(sided);
    }

    private static Placement about(double x, double y, double z, double degrees) {
        return Placement.rotation(new double[] {x, y, z, Math.toRadians(degrees)});
    }

    /** The models a footprint sets on its part that are shown: a hidden one is kept for its line alone. */
    public static List<FootprintDrawing.Model> shownOf(FootprintDrawing footprint) {
        return footprint.models().stream().filter(model -> !model.hidden()).toList();
    }
}
