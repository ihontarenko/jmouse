package org.jmouse.cad.kicad;

import org.jmouse.cad.exchange.imported.SheetMarks;

import org.jmouse.cad.exchange.imported.ImportedCircuit;
import org.jmouse.cad.exchange.imported.ImportedCircuitDocument;
import org.jmouse.kicad.KiCadProject;
import org.jmouse.kicad.connectivity.KiCadConnectivity;
import org.jmouse.kicad.pcb.KiCadPcb;
import org.jmouse.kicad.pcb.KiCadPcbFootprint;
import org.jmouse.kicad.KiCadProject.SheetInstance;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.kicad.schematic.KiCadSymbolInstance;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;

import org.jmouse.cad.document.DocumentParser;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.pcb.FootprintLines;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A KiCad design as jMX documents: a circuit for each sheet file — the root's places the others as blocks, as KiCad's
 * hierarchy does —, a symbol for each KiCad symbol a sheet draws with KiCad's own drawing, and a footprint for each
 * footprint its parts name that the libraries hold, each once however many sheets use it.
 *
 * @param circuits every sheet file's circuit, the root's first
 * @param symbols    the symbols the circuits draw with, each unit once; the catalogue keeps one it already has
 * @param components the components made from KiCad symbols the library map names none for, each once; the catalogue
 *                   keeps one it already has — a model written for it since, above all. Each lands on the footprint
 *                   its parts name when every pin number of it is a pad there
 * @param footprints the footprints the parts name, each once, as the footprint libraries hold them; the catalogue keeps
 *                   one it already has
 * @param libraries  the KiCad library each symbol, component and footprint made from KiCad comes from, by its slug —
 *                   {@code Device} for {@code kicad-device-r-shunt} —, so a host can file them as KiCad does
 * @param notes    what the import could not carry, by part: a component the library map does not name, a value that is
 *                 not a number, a unit drawn by another's place, a footprint no library holds
 * @param ground   the KiCad net that became node 0, the design's reference ground: {@code GND}, else its first ground
 */
public record KiCadProjectImport(List<DocumentNode> circuits, List<DocumentNode> symbols, List<DocumentNode> components,
                                 List<DocumentNode> footprints, Map<String, String> libraries, List<String> notes,
                                 Map<String, String> identifiers, String ground) {

    /** The property a KiCad part names its footprint by. */
    private static final String FOOTPRINT = "Footprint";

    /**
     * @param name       what the design is called: its project's name, {@code LINEAR_500W_PS-1}
     * @param footprints where the footprints its parts name are looked up: the project's own libraries, then KiCad's
     * @param installed  KiCad's installed libraries: a symbol or a footprint the design keeps a copy of is KiCad's when the
     *                   installed library of its name holds it, else the design's own
     * @param filing     whose KiCad's own libraries are, and where what is made of them is filed
     */
    public static KiCadProjectImport of(KiCadProject project, String name, KiCadLibraryMap library,
                                        KiCadCatalogue catalogue, KiCadFootprintLibraries footprints, InstalledKiCadLibraries installed,
                                        KiCadLibraries filing) {
        return of(project, name, library, catalogue, footprints, installed, Optional.empty(), filing);
    }

    /**
     * @param board the design's board, when it is read with it: each part then lands on the footprint that stands on the
     *              board, as the board holds it. It is what was laid out, whatever a library has become since.
     */
    public static KiCadProjectImport of(KiCadProject project, String name, KiCadLibraryMap library,
                                        KiCadCatalogue catalogue, KiCadFootprintLibraries footprints, InstalledKiCadLibraries installed,
                                        Optional<KiCadPcb> board, KiCadLibraries filing) {
        String root = DocumentParser.slugOf(name);
        Map<String, String> slugs = new LinkedHashMap<>();
        for (SheetInstance sheet : project.sheets()) {
            String file = sheet.schematic().name();
            slugs.putIfAbsent(file, sheet.root() ? root : root + "-" + DocumentParser.slugOf(file.replaceFirst("\\.kicad_sch$", "")));
        }
        List<DocumentNode> circuits = new ArrayList<>();
        Set<ImportedSheet.SymbolUse> uses = new LinkedHashSet<>();
        Map<String, DocumentNode> components = new LinkedHashMap<>();
        Map<String, String> libraries = new LinkedHashMap<>();
        List<String> notes = new ArrayList<>();
        String ground = referenceGround(project);
        if (!ground.equals(KiCadNetNames.KICAD_GROUND)) {
            notes.add("The design has no GND: " + ground + " is node 0, the ground everything is measured from; its other "
                    + "grounds stay nets of their own.");
        }
        Map<String, String> landedOnBoard = new LinkedHashMap<>();
        Map<String, String> relinked = board.map(read -> relinkedSymbols(read, project, notes)).orElse(Map.of());
        Map<String, KiCadFootprint> onBoard = board.map(read -> footprintsOnBoard(read, relinked, landedOnBoard, notes)).orElse(Map.of());
        Map<String, String> paired = pairedFootprints(project, library, catalogue);
        // The board's own copy of a paired footprint is the board's copper; ours lends it only its body.
        Map<String, String> pairedOnBoard = new LinkedHashMap<>(paired);
        pairedOnBoard.keySet().retainAll(onBoard.keySet());
        paired.keySet().removeAll(onBoard.keySet());
        Map<String, KiCadFootprint> found = footprintsNamed(project, footprints, paired, onBoard, landedOnBoard, notes);
        Map<String, String> footprintOfSymbol = footprintOfSymbol(project);
        Set<String> imported = new LinkedHashSet<>();
        Map<String, String> identifiers = new LinkedHashMap<>();
        SheetMarks marks = SheetMarks.of(catalogue::document, filing.markSymbols());
        // Each sheet is read alone, told what the whole design makes of the names that join across it.
        KiCadDesignNets designNets = new KiCadDesignNets(ground, KiCadConnectivity.of(project).globals(), project.busAliases());
        for (SheetInstance sheet : project.sheets()) {
            String file = sheet.schematic().name();
            if (!imported.add(file)) {
                continue;
            }
            ImportedSheet read = KiCadSheetImport.of(sheet.schematic(), sheet.path(), designNets, library, filing.ownLibrary(),
                    catalogue, placed -> slugs.getOrDefault(placed, DocumentParser.slugOf(placed)), placed -> labelledPorts(project, placed, designNets), marks);
            // A sheet is called what KiCad calls it; the project it belongs to is said by where it is filed.
            String title = sheet.root() ? name : sheet.name();
            identifiers.putAll(read.identifiers());
            ImportedSheet sheetOnFootprints = onTheirFootprints(read, found, paired, partsOnBoard(read, landedOnBoard), catalogue, notes);
            circuits.add(ImportedCircuitDocument.of(slugs.get(file), title, "Imported from KiCad: " + file,
                    sheetOnFootprints.circuit(), filing));
            uses.addAll(read.symbols());
            for (KiCadSymbolDefinition definition : read.components()) {
                String component = KiCadSymbolDrawing.componentSlugOf(definition);
                if (components.containsKey(component)) {
                    continue;
                }
                Optional<String> footprint = landing(definition, footprintOfSymbol.get(definition.name()), found, paired, notes);
                components.put(component, KiCadComponentStub.documentOf(definition, footprint,
                        KiCadComponentStub.drawnWith(definition, library, catalogue),
                        KiCadComponentStub.boxed(definition, library), filing, installed.holdsSymbol(definition.name())));
                libraries.putIfAbsent(component, KiCadSymbolDrawing.libraryOf(definition));
            }
            read.circuit().notes().forEach(note -> notes.add(file + ": " + note));
        }
        // A board's old identity of a symbol names the part that symbol became.
        relinked.forEach((old, current) -> Optional.ofNullable(identifiers.get(current)).ifPresent(id -> identifiers.put(old, id)));
        List<DocumentNode> symbols = new ArrayList<>();
        Set<String> drawn = new LinkedHashSet<>();
        for (ImportedSheet.SymbolUse use : uses) {
            DocumentNode symbol = KiCadSymbolDrawing.documentOf(use.symbol(), use.unit(), use.pinNames(), filing,
                    installed.holdsSymbol(use.symbol().name()));
            if (drawn.add(symbol.slug())) {
                symbols.add(symbol);
                libraries.putIfAbsent(symbol.slug(), KiCadSymbolDrawing.libraryOf(use.symbol()));
            }
        }
        List<DocumentNode> footprintDocuments = new ArrayList<>();
        found.forEach((libraryId, footprint) -> {
            Optional<List<FootprintLines.Model>> bodies = Optional.ofNullable(pairedOnBoard.get(libraryId)).flatMap(catalogue::footprintDrawing)
                    .flatMap(ours -> OwnBodies.of(KiCadFootprintDrawing.padsOf(footprint), ours));
            footprintDocuments.add(KiCadFootprintDrawing.documentOf(libraryId, footprint,
                    installed.holdsFootprint(libraryId) ? KiCadFootprintLibraries.Origin.INSTALLED : KiCadFootprintLibraries.Origin.PROJECT,
                    filing, bodies));
            libraries.putIfAbsent(KiCadFootprintDrawing.slugOf(libraryId), KiCadFootprintDrawing.libraryOf(libraryId));
        });
        return new KiCadProjectImport(List.copyOf(circuits), List.copyOf(symbols), List.copyOf(components.values()),
                List.copyOf(footprintDocuments), Map.copyOf(libraries), List.copyOf(notes), Map.copyOf(identifiers), ground);
    }


    /**
     * Our footprint each KiCad footprint the design names stands for, by its library id: where the library map pairs the
     * id with a code and the catalogue holds a footprint of that code. A pair whose code the catalogue does not hold is
     * not one — that footprint is converted from KiCad's as any other.
     */
    private static Map<String, String> pairedFootprints(KiCadProject project, KiCadLibraryMap library,
                                                        KiCadCatalogue catalogue) {
        Map<String, String> paired = new LinkedHashMap<>();
        for (SheetInstance sheet : project.sheets()) {
            for (KiCadSymbolInstance symbol : sheet.schematic().symbols()) {
                String libraryId = symbol.property(FOOTPRINT).strip();
                if (!libraryId.isEmpty() && !paired.containsKey(libraryId)) {
                    // Paired by the code the map gives, and named by it: the catalogue holding a footprint of it is the pair.
                    library.footprintCodeOf(libraryId).filter(code -> catalogue.footprintCoded(code).isPresent())
                            .ifPresent(code -> paired.put(libraryId, code));
                }
            }
        }
        return paired;
    }
    /** What a box placing a sheet file can wire a pin to: the port each of its hierarchical labels is. */
    private static Map<String, String> labelledPorts(KiCadProject project, String file, KiCadDesignNets designNets) {
        return project.sheets().stream().filter(sheet -> sheet.schematic().name().equals(file)).findFirst()
                .map(sheet -> KiCadSheetImport.portsOf(sheet.schematic(), sheet.path(), designNets)).orElse(Map.of());
    }

    /**
     * The footprints standing on a board, by library id, each as the first of its kind stands there; and the library id each
     * symbol landed on, by the symbol's uuid. A kind whose copies differ — one edited on the board — is said.
     */
    private static Map<String, KiCadFootprint> footprintsOnBoard(KiCadPcb board, Map<String, String> relinked, Map<String, String> landed,
                                                                 List<String> notes) {
        Map<String, KiCadFootprint> onBoard = new LinkedHashMap<>();
        Set<String> differing = new TreeSet<>();
        for (KiCadPcbFootprint placed : board.footprints()) {
            KiCadFootprint first = onBoard.putIfAbsent(placed.libraryId(), placed.footprint());
            if (first != null && !samePads(first, placed.footprint())) {
                differing.add(placed.libraryId());
            }
            placed.path().map(KiCadProjectImport::symbolOf).ifPresent(symbol -> landed.put(relinked.getOrDefault(symbol, symbol), placed.libraryId()));
        }
        if (!differing.isEmpty()) {
            notes.add("Copies of " + String.join(", ", differing) + " on the board differ from each other: every part of them "
                    + "stands on the first copy's pads.");
        }
        return onBoard;
    }

    private static String symbolOf(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * The symbols a board knows under an older identity, by that identity → the one they have now. A symbol placed anew keeps
     * its reference and loses its uuid, and KiCad itself relinks such a one by the reference. That can be trusted only of a
     * board that otherwise belongs to its schematic: of one drawn before the schematic was drawn anew, a reference is a
     * reused word and would seat another part.
     */
    private static Map<String, String> relinkedSymbols(KiCadPcb board, KiCadProject project, List<String> notes) {
        Map<String, String> uuidOfReference = new LinkedHashMap<>();
        Set<String> uuids = new LinkedHashSet<>();
        Set<String> ambiguous = new LinkedHashSet<>();
        for (SheetInstance sheet : project.sheets()) {
            for (KiCadSymbolInstance symbol : sheet.schematic().symbols()) {
                uuids.add(symbol.uuid());
                String known = uuidOfReference.putIfAbsent(symbol.reference(sheet.path()), symbol.uuid());
                if (known != null && !known.equals(symbol.uuid()) && symbol.unit() <= 1) {
                    ambiguous.add(symbol.reference(sheet.path()));
                }
            }
        }
        List<KiCadPcbFootprint> ofSchematic = board.footprints().stream().filter(KiCadPcbFootprint::ofSchematic).toList();
        long found = ofSchematic.stream().filter(placed -> uuids.contains(symbolOf(placed.path().orElseThrow()))).count();
        if (found * 2 <= ofSchematic.size()) {
            return Map.of();
        }
        Map<String, String> relinked = new LinkedHashMap<>();
        Set<String> names = new TreeSet<>();
        for (KiCadPcbFootprint placed : ofSchematic) {
            String symbol = symbolOf(placed.path().orElseThrow());
            String current = uuidOfReference.get(placed.reference());
            if (!uuids.contains(symbol) && current != null && !ambiguous.contains(placed.reference())) {
                relinked.put(symbol, current);
                names.add(placed.reference());
            }
        }
        if (!names.isEmpty()) {
            notes.add(String.join(", ", names) + ": placed anew on the schematic since the board was laid out, so found by their reference.");
        }
        return relinked;
    }

    private static boolean samePads(KiCadFootprint first, KiCadFootprint second) {
        return padsOf(first).equals(padsOf(second));
    }

    /** A footprint's pads to a thousandth of a millimetre, the least a copy of it differs by in anything but rounding. */
    private static List<String> padsOf(KiCadFootprint footprint) {
        return footprint.pads().stream().map(pad -> String.format(Locale.ROOT, "%s %.3f %.3f %.3f %.3f", pad.number(), pad.at().x(),
                pad.at().y(), pad.width(), pad.height())).toList();
    }

    /** The library id each part of a sheet stands on on the board, by the part's id. */
    private static Map<String, String> partsOnBoard(ImportedSheet read, Map<String, String> landed) {
        Map<String, String> parts = new LinkedHashMap<>();
        read.identifiers().forEach((uuid, id) -> {
            String libraryId = landed.get(uuid);
            if (libraryId != null) {
                parts.put(id, libraryId);
            }
        });
        return parts;
    }

    /**
     * Every footprint a part of the design names, by its library id, each once; one the libraries do not hold is said
     * in a note with the parts that name it — the design still imports. One paired with ours is not among them. What
     * stands on the board comes first, and answers for its name.
     */
    private static Map<String, KiCadFootprint> footprintsNamed(KiCadProject project, KiCadFootprintLibraries footprints,
                                                               Map<String, String> paired, Map<String, KiCadFootprint> onBoard,
                                                               Map<String, String> landedOnBoard, List<String> notes) {
        Map<String, KiCadFootprint> found = new LinkedHashMap<>(onBoard);
        Map<String, Set<String>> missing = new TreeMap<>();
        for (SheetInstance sheet : project.sheets()) {
            for (KiCadSymbolInstance symbol : sheet.schematic().symbols()) {
                String libraryId = symbol.property(FOOTPRINT).strip();
                // A footprint the map pairs with ours is not read: the part lands on ours, and KiCad need not be installed.
                if (libraryId.isEmpty() || found.containsKey(libraryId) || paired.containsKey(libraryId)
                        || landedOnBoard.containsKey(symbol.uuid())) {
                    continue;
                }
                Optional<KiCadFootprint> read;
                try {
                    read = footprints.find(libraryId);
                } catch (IllegalArgumentException unreadable) {
                    notes.add("The footprint " + libraryId + " cannot be read: " + unreadable.getMessage());
                    read = Optional.empty();
                }
                read.ifPresentOrElse(footprint -> found.put(libraryId, footprint), () -> missing
                        .computeIfAbsent(libraryId, ignored -> new TreeSet<>()).add(symbol.reference(sheet.path())));
            }
        }
        missing.forEach((libraryId, parts) -> notes.add("The footprint " + libraryId + " is in no library the import "
                + "reads (the project's folder, KiCad's own): " + String.join(", ", parts) + " land on nothing."));
        return found;
    }

    /** The footprint the parts of each KiCad symbol name, by the symbol's library id: the one most of them name. */
    private static Map<String, String> footprintOfSymbol(KiCadProject project) {
        Map<String, Map<String, Integer>> counted = new LinkedHashMap<>();
        for (SheetInstance sheet : project.sheets()) {
            for (KiCadSymbolInstance symbol : sheet.schematic().symbols()) {
                String libraryId = symbol.property(FOOTPRINT).strip();
                if (!libraryId.isEmpty()) {
                    counted.computeIfAbsent(symbol.libraryId(), ignored -> new LinkedHashMap<>())
                            .merge(libraryId, 1, Integer::sum);
                }
            }
        }
        Map<String, String> chosen = new LinkedHashMap<>();
        counted.forEach((symbol, named) -> named.entrySet().stream().max(Map.Entry.comparingByValue())
                .ifPresent(most -> chosen.put(symbol, most.getKey())));
        return chosen;
    }

    /**
     * A part of a catalogue component whose KiCad part sits on another package than the component's own footprint —
     * an NE5532 drawn as DIP-8 where the catalogue's is SOIC-8 — gets that footprint on its own line: ours, when the
     * library map pairs the KiCad footprint with one; else the one converted, when it was found and has a pad for every
     * pin number of the component.
     */
    private static ImportedSheet onTheirFootprints(ImportedSheet read, Map<String, KiCadFootprint> found,
                                                   Map<String, String> paired, Map<String, String> onBoard,
                                                   KiCadCatalogue catalogue, List<String> notes) {
        Set<String> made = new LinkedHashSet<>();
        read.components().forEach(definition -> made.add(KiCadSymbolDrawing.componentSlugOf(definition)));
        List<ImportedCircuit.Part> parts = new ArrayList<>();
        for (ImportedCircuit.Part part : read.circuit().parts()) {
            String boardFootprint = onBoard.get(part.id());
            // What stands on the board is what was laid out: the part stands on it whatever the schematic's field says.
            Map<String, String> kicadPads = read.pads().getOrDefault(part.id(), Map.of());
            if (boardFootprint != null && made.contains(part.component())) {
                parts.add(part.onFootprint(KiCadFootprintDrawing.codeOf(boardFootprint)));
                continue;
            }
            String libraryId = boardFootprint != null ? boardFootprint : read.footprints().get(part.id());
            Optional<String> ours = libraryId == null ? Optional.empty() : Optional.ofNullable(paired.get(libraryId));
            if (libraryId == null || (ours.isEmpty() && !found.containsKey(libraryId)) || made.contains(part.component())) {
                parts.add(part);
                continue;
            }
            String slug = ours.orElseGet(() -> KiCadFootprintDrawing.codeOf(libraryId));
            if (catalogue.footprintOf(part.component()).map(slug::equals).orElse(false)) {
                parts.add(part);
                continue;
            }
            // On KiCad's own footprint a pad is its symbol's pin number, which need not be the component's: a KiCad diode's
            // pad 1 is its cathode. Ours is numbered as our component is.
            if (ours.isEmpty() && !kicadPads.equals(catalogue.pinsByNumber(part.component()))) {
                parts.add(part.onFootprint(slug, kicadPads));
                continue;
            }
            parts.add(part.onFootprint(slug));
        }
        return new ImportedSheet(read.circuit().withParts(parts), read.symbols(), read.components(), read.footprints(), read.identifiers(), read.pads());
    }

    /**
     * The footprint a component made from a KiCad symbol lands on: ours, when the library map pairs the one its parts
     * name with it; else that one, when the libraries hold it and every pin number of the symbol is a pad of it;
     * otherwise none, and a note says why.
     */
    private static Optional<String> landing(KiCadSymbolDefinition definition, String libraryId,
                                            Map<String, KiCadFootprint> found, Map<String, String> paired,
                                            List<String> notes) {
        if (libraryId == null) {
            return Optional.empty();
        }
        if (paired.containsKey(libraryId)) {
            return Optional.of(paired.get(libraryId));
        }
        if (!found.containsKey(libraryId)) {
            return Optional.empty();
        }
        Set<String> pads = found.get(libraryId).padNumbers();
        List<String> missing = KiCadComponentStub.pinNumbers(definition).stream()
                .filter(number -> !pads.contains(number)).toList();
        if (!missing.isEmpty()) {
            notes.add(KiCadSymbolDrawing.componentSlugOf(definition) + " does not land on " + libraryId + ": its pins "
                    + String.join(", ", missing) + " are no pad there (pads " + String.join(", ", pads) + ").");
            return Optional.empty();
        }
        return Optional.of(KiCadFootprintDrawing.codeOf(libraryId));
    }

    /**
     * The ground the design is measured from: KiCad's {@code GND} where a power symbol names it; else, in a design of
     * isolated grounds ({@code GND1}, {@code GNDD}), the first of them by name, so a solver has its node 0.
     */
    private static String referenceGround(KiCadProject project) {
        TreeSet<String> grounds = new TreeSet<>();
        for (SheetInstance sheet : project.sheets()) {
            for (KiCadSymbolInstance symbol : sheet.schematic().symbols()) {
                boolean power = sheet.schematic().definitionOf(symbol).map(KiCadSymbolDefinition::power).orElse(false);
                if (power && symbol.value().toUpperCase(Locale.ROOT).startsWith(KiCadNetNames.KICAD_GROUND)) {
                    grounds.add(symbol.value());
                }
            }
        }
        return grounds.contains(KiCadNetNames.KICAD_GROUND) || grounds.isEmpty() ? KiCadNetNames.KICAD_GROUND
                : grounds.first();
    }

}
