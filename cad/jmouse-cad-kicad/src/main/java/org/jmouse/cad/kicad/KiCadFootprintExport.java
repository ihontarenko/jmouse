package org.jmouse.cad.kicad;

import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.kicad.KiCadLayer;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.KiCadProperty;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintField;
import org.jmouse.kicad.footprint.KiCadFootprintGraphic;
import org.jmouse.kicad.footprint.KiCadModel;
import org.jmouse.kicad.footprint.KiCadPad;
import org.jmouse.kicad.footprint.KiCadPadShape;
import org.jmouse.kicad.footprint.KiCadPadType;
import org.jmouse.kicad.pcb.KiCadPcb;
import org.jmouse.kicad.pcb.KiCadSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A footprint of ours as a KiCad footprint, as a library holds it — standing on the front, unturned —: the way back from
 * {@link KiCadFootprintDrawing}. Its pads as they are, a custom pad's outline as its primitives; each outline on the
 * front's KiCad layer of what it is for; the models KiCad's own libraries hold, by the file KiCad names them with. Both
 * are in millimetres with Y downwards, so nothing is scaled or turned.
 */
final class KiCadFootprintExport {

    /** KiCad's word for a pad of each of our types, and for each of our shapes. */
    private static final Map<String, KiCadPadType> TYPES = Map.of(PcbVocabulary.SURFACE, KiCadPadType.SURFACE_MOUNT,
            PcbVocabulary.THROUGH_HOLE, KiCadPadType.THROUGH_HOLE, PcbVocabulary.CONNECTOR, KiCadPadType.CONNECTOR,
            PcbVocabulary.UNPLATED_HOLE, KiCadPadType.UNPLATED_HOLE);
    private static final Map<String, KiCadPadShape> SHAPES = Map.of(PcbVocabulary.CIRCLE, KiCadPadShape.CIRCLE,
            PcbVocabulary.RECTANGLE, KiCadPadShape.RECTANGLE, PcbVocabulary.OVAL, KiCadPadShape.OVAL, "trapezoid", KiCadPadShape.TRAPEZOID,
            PcbVocabulary.ROUNDED_RECTANGLE, KiCadPadShape.ROUNDED_RECTANGLE, PcbVocabulary.CUSTOM, KiCadPadShape.CUSTOM);

    private final KiCadLibraries libraries;
    private final KiCadPathGraphics graphics;

    KiCadFootprintExport(KiCadLibraries libraries) {
        this.libraries = libraries;
        this.graphics = new KiCadPathGraphics(libraries.areaStep());
    }

    /**
     * @param models     the KiCad models that show its body, in its order: those of KiCad's own libraries
     * @param provenance whose it is, written into it as the fields KiCad keeps ({@link KiCadLibraries#propertiesOf}), so its
     *                   holder and licence travel with it
     */
    KiCadFootprint footprintOf(String name, String description, FootprintDrawing drawing, List<KiCadModel> models, Provenance provenance) {
        List<KiCadPad> pads = drawing.pads().stream().map(this::pad).toList();
        List<KiCadFootprintGraphic> drawn = new ArrayList<>();
        for (FootprintDrawing.Outline outline : drawing.outlines()) {
            Optional<String> layer = libraries.kicadLayerOf(outline.layer(), false);
            if (layer.isEmpty()) {
                continue;
            }
            drawn.addAll(outline.filled() ? graphics.filled(outline.path(), layer.get())
                    : graphics.stroked(outline.path(), layer.get(), outline.width()));
        }
        return new KiCadFootprint(name, description, List.of(), libraries.propertiesOf(provenance), pads, drawn, fieldsOf(drawing), models);
    }

    /** Where the footprint prints its part's reference and value, on KiCad's layer for what the field is printed on. */
    private List<KiCadFootprintField> fieldsOf(FootprintDrawing drawing) {
        List<KiCadFootprintField> fields = new ArrayList<>();
        for (FootprintDrawing.Field field : drawing.fields()) {
            String purpose = field.layer() == null ? (PcbVocabulary.FIELD_REFERENCE.equals(field.subject()) ? PcbVocabulary.SILKSCREEN : PcbVocabulary.FABRICATION) : field.layer();
            libraries.kicadLayerOf(purpose, false).ifPresent(layer -> fields.add(new KiCadFootprintField(
                    PcbVocabulary.FIELD_REFERENCE.equals(field.subject()) ? KiCadProperty.REFERENCE : KiCadProperty.VALUE,
                    new KiCadPoint(field.x(), field.y()), field.rotation(), layer, field.height(), false,
                    KiCadFootprintDrawing.alongOf(field.anchor()), KiCadPcb.Across.MIDDLE)));
        }
        return fields;
    }

    /** Where a field of the footprint prints, on the front's KiCad layer of its own. */
    String fieldLayer(String layer) {
        return libraries.kicadLayerOf(layer, false).orElseGet(() -> libraries.kicadLayerOf(PcbVocabulary.SILKSCREEN, false).orElseThrow());
    }

    private KiCadPad pad(FootprintDrawing.Pad pad) {
        KiCadPadType type = TYPES.getOrDefault(pad.type(), KiCadPadType.SURFACE_MOUNT);
        KiCadPadShape shape = SHAPES.getOrDefault(pad.shape(), KiCadPadShape.RECTANGLE);
        Optional<double[]> drill = pad.drillWidth() > 0 ? Optional.of(new double[] {pad.drillWidth(), pad.drillHeight() > 0
                ? pad.drillHeight() : pad.drillWidth()}) : Optional.empty();
        List<KiCadFootprintGraphic> primitives = shape == KiCadPadShape.CUSTOM && pad.path() != null && !pad.path().isBlank()
                ? graphics.filled(pad.path(), "") : List.of();
        return new KiCadPad(pad.number(), type, shape, new KiCadPoint(pad.x(), pad.y()), pad.rotation(), pad.width(), pad.height(),
                drill, pad.roundness(), layersOf(pad, type), Optional.ofNullable(pad.maskMargin()), Optional.ofNullable(pad.pasteMargin()),
                primitives);
    }

    /**
     * The layers a pad is on: through the board, every copper layer and the mask on both sides where it opens it; on the
     * surface, its face's copper with the mask and the paste it opens.
     */
    private static List<String> layersOf(FootprintDrawing.Pad pad, KiCadPadType type) {
        List<String> layers = new ArrayList<>();
        if (type == KiCadPadType.THROUGH_HOLE || type == KiCadPadType.UNPLATED_HOLE) {
            layers.add(KiCadLayer.ALL_COPPER);
            if (pad.opensMask()) {
                layers.add(KiCadLayer.ALL_MASK);
            }
            return layers;
        }
        KiCadSide side = PcbVocabulary.FACE_FAR.equals(pad.face()) ? KiCadSide.BACK : KiCadSide.FRONT;
        layers.add(side.copper());
        if (pad.opensMask()) {
            layers.add(side.layer(KiCadLayer.MASK_SUFFIX));
        }
        if (pad.opensPaste()) {
            layers.add(side.layer(KiCadLayer.PASTE_SUFFIX));
        }
        return layers;
    }
}
