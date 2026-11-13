package org.jmouse.cad.kicad;

import org.jmouse.cad.table.ReadTable;
import org.jmouse.cad.table.TableSource;
import org.jmouse.cad.table.ShippedTables;
import org.jmouse.cad.exchange.imported.FilingTable;
import org.jmouse.cad.exchange.imported.ImportFiling;
import org.jmouse.cad.exchange.imported.SheetFiling;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;

import org.jmouse.kicad.KiCadLayer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * What a drawing converted from KiCad's own libraries says of itself, as {@code kicad-libraries.sht} beside this class
 * writes it: who holds KiCad's libraries and under what terms, and which of our layers each KiCad layer is.
 *
 * @param holder  who holds the rights to KiCad's own libraries, named on every document converted from them
 * @param license their licence, as an SPDX expression
 * @param home    where they are kept, which a part fetched from them links to
 * @param sheet         what it says as every product's table does: category, tags, transient, mark symbols
 * @param modelTags     the tags a 3D model taken from KiCad's model libraries carries
 * @param modelScales   the millimetres a unit of KiCad's own model files is, by format, where it is not the format's standard
 * @param modelUps      the axis KiCad's own model files stand up along, by format, where it is not the format's standard
 * @param ownLibrary    the KiCad library a component with no KiCad counterpart is written under
 * @param ownLibraryDescription what a project's library tables say of that library, carried beside the project
 * @param layers        our layer each KiCad layer is, by KiCad's name: {@code F.SilkS} is {@code silkscreen}; the first of
 *                      a side is what ours is written as
 * @param areaStep      how far an area written as points may stray from the arcs and curves it steps, in millimetres: as
 *                      far as KiCad's own steps stray when it fills one
 * @param modelFile     the file a board's footprint names a model of KiCad's model libraries by, {@code %s} its place there
 * @param edgeWidth     the width a board's edge is drawn with on {@code Edge.Cuts}, which cuts along its middle whatever it is
 * @param holeFootprint what the footprint of a hole the board drills of its own is named in the own library, {@code %s} its drill
 * @param provenanceFields the field of a KiCad footprint each part of a provenance is written as — {@code source},
 *                         {@code authors}, {@code license} —, so whose a footprint is travels into KiCad and back
 */
public record KiCadLibraries(String holder, String license, String home, SheetFiling sheet, List<String> modelTags,
                             Map<String, Double> modelScales, Map<String, String> modelUps, String ownLibrary, String ownLibraryDescription,
                             Map<String, String> layers, String pcbRules, Map<String, String> pcbRuleNames, double textLineSpacing,
                             String pcbOwnPart, String pcbClassOpening, double areaStep, String modelFile,
                             double edgeWidth, String holeFootprint, Map<String, String> provenanceFields)
        implements ImportFiling {

    /** The table's address in the catalogue. */
    public static final String TABLE = "kicad-libraries";

    /** Where its module ships it. */
    public static final ShippedTables SHIPPED = ShippedTables.in(KiCadLibraries.class, "jmouse-cad-kicad", "org/jmouse/cad/kicad/tables");

    /** The parts of a provenance, as {@code provenance-fields} names them: the words of the document's {@code meta}. */
    private static final String SOURCE = MetaNode.SOURCE;
    private static final String AUTHORS = MetaNode.AUTHORS;
    private static final String LICENSE = MetaNode.LICENSE;

    public KiCadLibraries {
        modelTags = List.copyOf(modelTags);
        modelScales = Map.copyOf(modelScales);
        modelUps = Map.copyOf(modelUps);
        layers = Collections.unmodifiableMap(new LinkedHashMap<>(layers));
        provenanceFields = Collections.unmodifiableMap(new LinkedHashMap<>(provenanceFields));
    }

    /** The table this library ships. */
    public static KiCadLibraries shipped() {
        return readFrom(SHIPPED).current();
    }

    /**
     * As its source answers at each use — the engine's catalogue table, its curator's revision where one is kept —, read again
     * only when another revision answers.
     */
    public static ReadTable<KiCadLibraries> readFrom(TableSource tables) {
        return new ReadTable<>(tables, TABLE, sheet -> of(FilingTable.of(TABLE, sheet)));
    }

    public static KiCadLibraries of(FilingTable table) {
        return new KiCadLibraries(table.text("holder"), table.text("license"), table.text("home"), SheetFiling.of(table),
                table.words("model-tags"), table.assignments("model-scales").entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, scale -> Double.parseDouble(scale.getValue()))),
                table.assignments("model-ups"), table.text("own-library"), table.text("own-library-description"), table.layers("kicad"),
                table.text("pcb-rules"), table.assignments("pcb-rule-names"), Double.parseDouble(table.text("text-line-spacing")),
                table.text("pcb-own-part"), table.text("pcb-class-opening"), table.millimetres("area-step"), table.text("model-file"),
                table.millimetres("edge-width"), table.text("hole-footprint"), table.assignments("provenance-fields"));
    }

    /**
     * Whose a drawing converted from KiCad is: one of KiCad's installed libraries is the KiCad Libraries Team's, under
     * their licence; one of a design's own claims neither — its holder and terms are the design's.
     *
     * @param source    where it was taken, as {@link KiCadSources} spells it
     * @param installed whether it comes from KiCad's installed libraries
     */
    @Override
    public Provenance provenance(String source, boolean installed) {
        return installed ? new Provenance(source, holder, license) : new Provenance(source, null, null);
    }

    public Map<String, String> propertiesOf(Provenance provenance) {
        Map<String, String> properties = new LinkedHashMap<>();
        stated(SOURCE, provenance.source(), properties);
        stated(AUTHORS, provenance.authors(), properties);
        stated(LICENSE, provenance.license(), properties);
        return properties;
    }

    /**
     * Whose a footprint is, as the fields KiCad keeps on it say: theirs where they name a holder or a licence — written by this
     * program on the way out, or by anyone who keeps the same fields —; empty where they name neither.
     */
    public Optional<Provenance> provenanceOf(Map<String, String> properties) {
        Optional<String> authors = field(AUTHORS, properties);
        Optional<String> license = field(LICENSE, properties);
        if (authors.isEmpty() && license.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Provenance(field(SOURCE, properties).orElse(null), authors.orElse(null), license.orElse(null)));
    }

    private void stated(String part, String value, Map<String, String> properties) {
        String field = provenanceFields.get(part);
        if (field != null && value != null && !value.isBlank()) {
            properties.put(field, value);
        }
    }

    private Optional<String> field(String part, Map<String, String> properties) {
        return Optional.ofNullable(provenanceFields.get(part)).map(properties::get).filter(value -> !value.isBlank());
    }

    /**
     * The KiCad layer one of ours is written as: on a side, that side's — {@code silkscreen} on the back is {@code B.SilkS};
     * one of no side, its first.
     */
    public Optional<String> kicadLayerOf(String purpose, boolean back) {
        List<String> named = layers.entrySet().stream().filter(entry -> entry.getValue().equals(purpose)).map(Map.Entry::getKey).toList();
        return named.stream().filter(layer -> back ? KiCadLayer.isBack(layer) : KiCadLayer.isFront(layer)).findFirst()
                .or(() -> named.stream().filter(layer -> !KiCadLayer.isFront(layer) && !KiCadLayer.isBack(layer)).findFirst());
    }

    /** The KiCad library a drawing comes from, as its source says it ({@link KiCadSources#libraryOf}). */
    @Override
    public Optional<String> libraryOf(Provenance provenance) {
        return KiCadSources.libraryOf(provenance.source());
    }

    /** The code KiCad's import names a document by, from the KiCad id or the model's path its source keeps. */
    @Override
    public Optional<String> codeOfImported(DocumentKind kind, String slug, Provenance provenance) {
        String source = provenance.source();
        if (kind.equals(PcbModule.FOOTPRINT)) {
            return KiCadSources.footprintIdOf(source).map(KiCadFootprintDrawing::codeOf);
        }
        if (kind.equals(ModelModule.MODEL)) {
            return KiCadSources.modelPathOf(source).map(KiCadFootprintDrawing::modelCodeOf);
        }
        if (kind.equals(SchematicModule.SYMBOL)) {
            return KiCadSources.symbolIdOf(source).map(id -> KiCadSymbolDrawing.codeOf(id, KiCadSymbolDrawing.unitOfSlug(slug)));
        }
        return Optional.empty();
    }

    /** Our layer a KiCad layer is; empty for one that is not a part's. */
    @Override
    public Optional<String> layerOf(String kicad) {
        return Optional.ofNullable(layers.get(kicad));
    }
}
