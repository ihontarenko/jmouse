package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.script.Block;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A footprint as data a picture is painted from: its pads and its outlines, in millimetres from its origin, Y downwards —
 * what a footprint definition carries, and what a library browser or a board preview reads.
 */
public record FootprintDrawing(List<Pad> pads, List<Outline> outlines) {

    /** The version of the JSON shape; a drawing of another version is refused, not guessed at. */
    private static final int VERSION = 1;

    public FootprintDrawing {
        pads = List.copyOf(pads);
        outlines = List.copyOf(outlines);
    }

    /**
     * @param type  {@code through-hole}, {@code surface}, {@code connector}, {@code unplated-hole}
     * @param shape {@code circle}, {@code rectangle}, {@code oval}, {@code trapezoid}, {@code rounded-rectangle}, {@code custom}
     */
    public record Pad(String number, String type, String shape, double x, double y, double rotation, double width,
                      double height, double drillWidth, double drillHeight, double roundness) {
    }

    /** @param layer {@code silkscreen}, {@code fabrication}, {@code courtyard}, {@code copper} */
    public record Outline(String layer, double width, boolean filled, String path) {
    }

    /** The drawing a footprint document's lines make. */
    static FootprintDrawing of(Block.Footprint footprint) {
        return new FootprintDrawing(footprint.pads().stream().map(pad -> new Pad(pad.number(), pad.type().word(),
                pad.shape().word(), pad.x(), pad.y(), pad.rotation(), pad.width(), pad.height(), pad.drillWidth(),
                pad.drillHeight(), pad.roundness())).toList(),
                footprint.outlines().stream().map(outline -> new Outline(outline.layer(), outline.width(), outline.filled(),
                        outline.path())).toList());
    }

    /** The lines that write it back. */
    Block.Footprint block() {
        return new Block.Footprint(pads.stream().map(pad -> new Block.FootprintPad(pad.number(),
                Block.PadType.of(pad.type()).orElseThrow(), Block.PadShape.of(pad.shape()).orElseThrow(), pad.x(), pad.y(),
                pad.rotation(), pad.width(), pad.height(), pad.drillWidth(), pad.drillHeight(), pad.roundness())).toList(),
                outlines.stream().map(outline -> new Block.FootprintOutline(outline.layer(), outline.width(), outline.filled(),
                        outline.path())).toList());
    }

    /** The numbers its copper pads carry, each once — what a component's package pin numbers must be among. */
    public Set<String> padNumbers() {
        Set<String> numbers = new LinkedHashSet<>();
        pads.stream().filter(pad -> !pad.type().equals(Block.PadType.UNPLATED_HOLE.word()) && !pad.number().isEmpty())
                .forEach(pad -> numbers.add(pad.number()));
        return numbers;
    }

    // ─── JSON ───────────────────────────────────────────────────────────────────────────────────────

    /** The canonical JSON of this drawing. */
    public String toJson() {
        Map<String, Object> drawing = new LinkedHashMap<>();
        drawing.put("version", VERSION);
        drawing.put("pads", pads.stream().map(FootprintDrawing::padTree).toList());
        drawing.put("outlines", outlines.stream().map(FootprintDrawing::outlineTree).toList());
        return JsonText.write(drawing);
    }

    /** The drawing a footprint definition holds. */
    public static FootprintDrawing fromJson(String json) {
        if (!(JsonText.read(json) instanceof Map<?, ?> drawing) || !(drawing.get("version") instanceof Double version)
                || version != VERSION) {
            throw new IllegalArgumentException("A footprint drawing is a JSON object of version " + VERSION + ".");
        }
        return new FootprintDrawing(listOf(drawing.get("pads")).stream().map(FootprintDrawing::pad).toList(),
                listOf(drawing.get("outlines")).stream().map(FootprintDrawing::outline).toList());
    }

    private static Map<String, Object> padTree(Pad pad) {
        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("number", pad.number());
        tree.put("type", pad.type());
        tree.put("shape", pad.shape());
        tree.put("x", pad.x());
        tree.put("y", pad.y());
        tree.put("rotation", pad.rotation());
        tree.put("width", pad.width());
        tree.put("height", pad.height());
        tree.put("drillWidth", pad.drillWidth());
        tree.put("drillHeight", pad.drillHeight());
        tree.put("roundness", pad.roundness());
        return tree;
    }

    private static Map<String, Object> outlineTree(Outline outline) {
        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("layer", outline.layer());
        tree.put("width", outline.width());
        tree.put("filled", outline.filled());
        tree.put("path", outline.path());
        return tree;
    }

    private static Pad pad(Map<?, ?> tree) {
        return new Pad(String.valueOf(tree.get("number")), String.valueOf(tree.get("type")), String.valueOf(tree.get("shape")),
                number(tree, "x"), number(tree, "y"), number(tree, "rotation"), number(tree, "width"), number(tree, "height"),
                number(tree, "drillWidth"), number(tree, "drillHeight"), number(tree, "roundness"));
    }

    private static Outline outline(Map<?, ?> tree) {
        return new Outline(String.valueOf(tree.get("layer")), number(tree, "width"), Boolean.TRUE.equals(tree.get("filled")),
                String.valueOf(tree.get("path")));
    }

    private static double number(Map<?, ?> tree, String key) {
        return tree.get(key) instanceof Double value ? value : 0;
    }

    private static List<Map<?, ?>> listOf(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Map.class::isInstance).<Map<?, ?>>map(Map.class::cast).toList();
    }
}
