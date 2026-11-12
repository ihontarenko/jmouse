package org.jmouse.cad.kicad;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.library.KiCadProjectLibraries;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ExportedFile;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.kicad.KiCadProjectSettings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A pcb as a KiCad project, the way back from {@link KiCadProjectImport}: its board, the schematic of the circuit it lays
 * out, and the project file that holds what KiCad keeps there — the net classes and which nets are in them, the design
 * rules, the track and via sizes it offers. All three are named after the circuit, as KiCad names a project's files after
 * the project; each footprint names the symbol it stands for, so KiCad's "Update PCB from Schematic" finds the board
 * already agreeing with its schematic.
 */
public final class KiCadProjectExport {

    private static final String SCHEMATIC_NOTE = "Schematic: ";
    private static final String BOARD_NOTE = "Board: ";
    /** What a design written as several files is called after its root: {@code amplifier-kicad}. */
    public static final String SET_ENDING = "-kicad";
    private static final String PROJECT_EXTENSION = ".kicad_pro";

    private KiCadProjectExport() {
    }

    /**
     * @param catalogue the documents the pcb names: its circuit, the components, the symbols, the footprints, the models
     * @param rules     the values of the rules the pcb is made by
     * @param artwork   how every format draws what no document says
     * @throws IllegalArgumentException when the pcb's circuit cannot be read
     */
    public static ExportedFiles of(DocumentNode pcbDocument, CatalogueDocuments catalogue, KiCadLibraryMap map, KiCadLibraries libraries,
                                   Map<String, Double> rules, PcbArtwork.Settings artwork) {
        PcbFabrication fabrication = PcbFabrication.of(pcbDocument, catalogue);
        PcbDrawing pcb = fabrication.pcb();
        DocumentNode circuitDocument = catalogue.find(pcb.circuit()).filter(found -> found.kind().equals(SimulationModule.CIRCUIT))
                .orElseThrow(() -> new IllegalArgumentException("The pcb lays out '" + pcb.circuit() + "', which is no circuit the reader may see."));
        Circuit circuit = SimulationReading.circuit(circuitDocument);
        String project = circuitDocument.slug();
        String title = circuitDocument.title() == null || circuitDocument.title().isBlank() ? project : circuitDocument.title();
        List<ExportedFile> files = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        Map<String, KiCadSchematicExport.Symbol> symbols = Map.of();
        Map<String, String> nets = Map.of();
        Map<String, SExpressionList> librarySymbols = Map.of();
        Map<String, String> references = referencesOf(fabrication);
        if (KiCadSchematicExport.drawn(circuit)) {
            KiCadSchematicExport.Exported schematic = KiCadSchematicExport.of(project, title, circuit, catalogue, map, libraries,
                    designator -> references.getOrDefault(designator, designator));
            files.add(new ExportedFile(schematic.fileName(), schematic.text()));
            schematic.sheets().forEach(sheet -> files.add(new ExportedFile(sheet.fileName(), sheet.text())));
            schematic.notes().forEach(note -> notes.add(SCHEMATIC_NOTE + note));
            symbols = schematic.symbols();
            nets = schematic.nets();
            librarySymbols = schematic.librarySymbols();
        } else {
            notes.add(SCHEMATIC_NOTE + "the circuit has no drawing, so the project holds no schematic and the board's footprints are "
                    + "tied to no symbol. Open the circuit on the bench and save it, so its layout is written.");
        }
        KiCadPcbExport.Exported board = KiCadPcbExport.of(fabrication, project, title, references, symbols, nets, catalogue, map, libraries,
                rules, artwork);
        files.add(new ExportedFile(board.fileName(), board.text()));
        board.notes().forEach(note -> notes.add(BOARD_NOTE + note));
        files.addAll(ownLibraries(libraries.ownLibrary(), librarySymbols, board.footprints(), libraries));
        files.addFirst(new ExportedFile(project + PROJECT_EXTENSION, settingsOf(pcb, rules, libraries, nets).json(project)));
        return new ExportedFiles(project + SET_ENDING, files, notes);
    }

    /**
     * The library the project carries of its own: every footprint and symbol it names under this program's own library —
     * those no KiCad library holds —, as KiCad keeps a library beside a project, and the project's tables naming it. So the
     * project opens whole wherever it goes, and its parts are found, edited and updated in KiCad as KiCad's own are. None
     * where it names nothing of its own.
     *
     * @param nickname the program's own library, as the project's footprints and symbols name it
     */
    private static List<ExportedFile> ownLibraries(String nickname, Map<String, SExpressionList> symbols, Map<String, KiCadFootprint> footprints,
                                                   KiCadLibraries libraries) {
        String prefix = nickname + ":";
        List<ExportedFile> files = new ArrayList<>();
        String referenceLayer = libraries.kicadLayerOf(PcbVocabulary.SILKSCREEN, false).orElseThrow();
        String valueLayer = libraries.kicadLayerOf(PcbVocabulary.FABRICATION, false).orElseThrow();
        footprints.forEach((libraryId, footprint) -> {
            if (libraryId.startsWith(prefix)) {
                files.add(new ExportedFile(KiCadProjectLibraries.footprintFile(nickname, libraryId.substring(prefix.length())),
                        KiCadProjectLibraries.footprint(footprint, referenceLayer, valueLayer)));
            }
        });
        if (!files.isEmpty()) {
            files.add(new ExportedFile(KiCadProjectLibraries.FOOTPRINT_TABLE,
                    KiCadProjectLibraries.footprintTable(nickname, libraries.ownLibraryDescription())));
        }
        Map<String, SExpressionList> own = new LinkedHashMap<>();
        symbols.forEach((name, symbol) -> {
            if (name.startsWith(prefix)) {
                own.put(name, symbol);
            }
        });
        if (!own.isEmpty()) {
            files.add(new ExportedFile(KiCadProjectLibraries.symbolFile(nickname), KiCadProjectLibraries.symbols(nickname, own)));
            files.add(new ExportedFile(KiCadProjectLibraries.SYMBOL_TABLE,
                    KiCadProjectLibraries.symbolTable(nickname, libraries.ownLibraryDescription())));
        }
        return files;
    }

    /**
     * What KiCad calls each part, by its name in the unfolded design: what the board prints on it — a part of a placed sheet
     * reads {@code R1}, not {@code S1_R1} —, but its full name where two print alike, since KiCad keeps every reference apart.
     */
    private static Map<String, String> referencesOf(PcbFabrication fabrication) {
        Map<String, Long> printed = new LinkedHashMap<>();
        fabrication.parts().forEach(part -> printed.merge(part.printed(), 1L, Long::sum));
        Map<String, String> references = new LinkedHashMap<>();
        fabrication.parts().forEach(part -> references.put(part.designator(),
                printed.get(part.printed()) > 1 ? part.designator() : part.printed()));
        return references;
    }

    /**
     * What the project file says of how the board is made: the pcb's usual track, gap and via as KiCad's Default class, each
     * of its classes with the nets in it, the least values a house makes under KiCad's names for them — where two of ours are
     * one of KiCad's, the least of them —, and the sizes it offers.
     */
    private static KiCadProjectSettings settingsOf(PcbDrawing pcb, Map<String, Double> rules, KiCadLibraries libraries,
                                                   Map<String, String> kicadNets) {
        KiCadProjectSettings.NetClass usual = new KiCadProjectSettings.NetClass("", rules.get(PcbDrawing.RULE_TRACK_WIDTH),
                rules.get(PcbDrawing.RULE_CLEARANCE), rules.get(PcbDrawing.RULE_VIA_DRILL), rules.get(PcbDrawing.RULE_VIA_DIAMETER));
        List<KiCadProjectSettings.NetClass> classes = new ArrayList<>();
        Map<String, String> classOf = new LinkedHashMap<>();
        for (PcbDrawing.NetClass netClass : pcb.netClasses()) {
            classes.add(new KiCadProjectSettings.NetClass(netClass.name(), netClass.width(), netClass.clearance(), netClass.drill(),
                    netClass.diameter()));
            netClass.nets().forEach(net -> classOf.put(kicadNets.getOrDefault(net, net), netClass.name()));
        }
        Map<String, Double> kicadRules = new LinkedHashMap<>();
        libraries.pcbRuleNames().forEach((ours, theirs) -> Optional.ofNullable(rules.get(ours))
                .ifPresent(value -> kicadRules.merge(theirs, value, Math::min)));
        return new KiCadProjectSettings(Optional.of(usual), classes, classOf, List.of(), kicadRules, pcb.sizes().tracks(),
                pcb.sizes().vias().stream().map(size -> new KiCadProjectSettings.ViaSize(size.drill(), size.diameter())).toList());
    }
}
