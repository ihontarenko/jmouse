package org.jmouse.cad.exchange.pcb;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The parts' bodies as their footprints know them — what a part is shown as where no 3D model shows it: the widest
 * closed outline its footprint draws on the fabrication layer, not the small marks beside it, raised to the height the
 * footprint gives its package. A footprint that gives no height, or draws no closed outline, has no body.
 */
public final class PartBodies {

    /** How near a run's end must come to its start to close it, in millimetres. */
    private static final double CLOSED = 1e-3;

    private static final PathParser PATHS = new PathParser();

    /**
     * A part's body where the part stands.
     *
     * @param outline the body's outline on the pcb, in the pcb's frame
     * @param height  how high it stands over the face its part is on; empty where its footprint gives no height, when it is
     *                its outline alone
     */
    public record PartBody(String designator, String side, List<PcbDrawing.Point> outline, OptionalDouble height) {

        /** Whether it stands up from its face: whether its footprint gives its height. */
        public boolean raised() {
            return height.isPresent();
        }
    }

    private final int curveSteps;

    /** @param curveSteps how many straight pieces a curve of an outline is drawn with */
    public PartBodies(int curveSteps) {
        this.curveSteps = curveSteps;
    }

    /**
     * The part's body: the widest closed outline its footprint draws on the fabrication layer, raised to the height the footprint
     * gives; empty where it draws none.
     */
    public Optional<PartBody> of(PcbFabrication.SeatedPart part) {
        Optional<FootprintDrawing> drawing = part.drawing();
        if (drawing.isEmpty()) {
            return Optional.empty();
        }
        List<double[]> widest = null;
        for (FootprintDrawing.Outline outline : drawing.get().outlines()) {
            if (!"fabrication".equals(outline.layer()) || outline.path() == null || outline.path().isBlank()) {
                continue;
            }
            for (List<double[]> run : PathPolylines.of(PATHS.parse(outline.path()), curveSteps)) {
                if (closed(run) && (widest == null || enclosed(run) > enclosed(widest))) {
                    widest = run;
                }
            }
        }
        if (widest == null) {
            return Optional.empty();
        }
        List<PcbDrawing.Point> placed = new ArrayList<>();
        for (double[] point : widest) {
            placed.add(part.placement().placed(point[0], point[1]));
        }
        OptionalDouble height = drawing.get().body() == null ? OptionalDouble.empty() : OptionalDouble.of(drawing.get().body().height());
        return Optional.of(new PartBody(part.designator(), part.placement().side(), placed, height));
    }

    private static boolean closed(List<double[]> run) {
        return run.size() > 3 && Math.hypot(run.getFirst()[0] - run.getLast()[0], run.getFirst()[1] - run.getLast()[1]) < CLOSED;
    }

    /** The area a run encloses, whichever way it turns. */
    private static double enclosed(List<double[]> run) {
        double sum = 0;
        for (int index = 0; index < run.size(); index++) {
            double[] point = run.get(index);
            double[] next = run.get((index + 1) % run.size());
            sum += point[0] * next[1] - next[0] * point[1];
        }
        return Math.abs(sum) / 2;
    }
}
