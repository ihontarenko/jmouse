package org.jmouse.cad.kicad;

import org.jmouse.kicad.KiCadProject;
import org.jmouse.kicad.KiCadProject.SheetInstance;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.kicad.schematic.KiCadSymbolInstance;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;

import org.jmouse.cad.document.DocumentNode;

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
 */
public record KiCadProjectImport(List<DocumentNode> circuits, List<DocumentNode> symbols, List<DocumentNode> components,
                                 List<DocumentNode> footprints, Map<String, String> libraries, List<String> notes) {

    /** The property a KiCad part names its footprint by. */
    private static final String FOOTPRINT = "Footprint";

    /**
     * @param name       what the design is called: its project's name, {@code LINEAR_500W_PS-1}
     * @param footprints where the footprints its parts name are looked up: the project's own libraries, then KiCad's
     * @param filing     whose KiCad's own libraries are, and where what is made of them is filed
     */
    public static KiCadProjectImport of(KiCadProject project, String name, KiCadLibraryMap library,
                                        KiCadCatalogue catalogue, KiCadFootprintLibraries footprints,
                                        KiCadLibraries filing) {
        String root = slugOf(name);
        Map<String, String> slugs = new LinkedHashMap<>();
        for (SheetInstance sheet : project.sheets()) {
            String file = sheet.schematic().name();
            slugs.putIfAbsent(file, sheet.root() ? root : root + "-" + slugOf(file.replaceFirst("\\.kicad_sch$", "")));
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
        Map<String, String> paired = pairedFootprints(project, library, catalogue);
        Map<String, KiCadFootprint> found = footprintsNamed(project, footprints, paired, notes);
        Map<String, String> footprintOfSymbol = footprintOfSymbol(project);
        Set<String> imported = new LinkedHashSet<>();
        for (SheetInstance sheet : project.sheets()) {
            String file = sheet.schematic().name();
            if (!imported.add(file)) {
                continue;
            }
            ImportedSheet read = KiCadSheetImport.of(sheet.schematic(), sheet.path(), ground, library, filing.ownLibrary(),
                    catalogue, placed -> slugs.getOrDefault(placed, slugOf(placed)));
            // A sheet is called what KiCad calls it; the project it belongs to is said by where it is filed.
            String title = sheet.root() ? name : sheet.name();
            ImportedSheet sheetOnFootprints = onTheirFootprints(read, found, paired, catalogue, notes);
            circuits.add(KiCadCircuitDocument.of(slugs.get(file), title, "Imported from KiCad: " + file, sheetOnFootprints,
                    filing));
            uses.addAll(read.symbols());
            for (KiCadSymbolDefinition definition : read.components()) {
                String component = KiCadSymbolDrawing.componentSlugOf(definition);
                if (components.containsKey(component)) {
                    continue;
                }
                Optional<String> footprint = landing(definition, footprintOfSymbol.get(definition.name()), found, paired, notes);
                components.put(component, KiCadComponentStub.documentOf(definition, footprint,
                        KiCadComponentStub.drawnWith(definition, library, catalogue),
                        KiCadComponentStub.boxed(definition, library), filing));
                libraries.putIfAbsent(component, KiCadSymbolDrawing.libraryOf(definition));
            }
            read.notes().forEach(note -> notes.add(file + ": " + note));
        }
        List<DocumentNode> symbols = new ArrayList<>();
        Set<String> drawn = new LinkedHashSet<>();
        for (ImportedSheet.SymbolUse use : uses) {
            DocumentNode symbol = KiCadSymbolDrawing.documentOf(use.symbol(), use.unit(), use.pinNames(), filing);
            if (drawn.add(symbol.slug())) {
                symbols.add(symbol);
                libraries.putIfAbsent(symbol.slug(), KiCadSymbolDrawing.libraryOf(use.symbol()));
            }
        }
        List<DocumentNode> footprintDocuments = new ArrayList<>();
        found.forEach((libraryId, footprint) -> {
            footprintDocuments.add(KiCadFootprintDrawing.documentOf(libraryId, footprint,
                    footprints.originOf(libraryId).orElse(KiCadFootprintLibraries.Origin.PROJECT), filing));
            libraries.putIfAbsent(KiCadFootprintDrawing.slugOf(libraryId), KiCadFootprintDrawing.libraryOf(libraryId));
        });
        return new KiCadProjectImport(List.copyOf(circuits), List.copyOf(symbols), List.copyOf(components.values()),
                List.copyOf(footprintDocuments), Map.copyOf(libraries), List.copyOf(notes));
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
                    library.footprintCodeOf(libraryId).flatMap(catalogue::footprintCoded)
                            .ifPresent(slug -> paired.put(libraryId, slug));
                }
            }
        }
        return paired;
    }
    /**
     * Every footprint a part of the design names, by its library id, each once; one the libraries do not hold is said
     * in a note with the parts that name it — the design still imports. One paired with ours is not among them.
     */
    private static Map<String, KiCadFootprint> footprintsNamed(KiCadProject project, KiCadFootprintLibraries footprints,
                                                               Map<String, String> paired, List<String> notes) {
        Map<String, KiCadFootprint> found = new LinkedHashMap<>();
        Map<String, Set<String>> missing = new TreeMap<>();
        for (SheetInstance sheet : project.sheets()) {
            for (KiCadSymbolInstance symbol : sheet.schematic().symbols()) {
                String libraryId = symbol.property(FOOTPRINT).strip();
                // A footprint the map pairs with ours is not read: the part lands on ours, and KiCad need not be installed.
                if (libraryId.isEmpty() || found.containsKey(libraryId) || paired.containsKey(libraryId)) {
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
                                                   Map<String, String> paired, KiCadCatalogue catalogue, List<String> notes) {
        Set<String> made = new LinkedHashSet<>();
        read.components().forEach(definition -> made.add(KiCadSymbolDrawing.componentSlugOf(definition)));
        List<ImportedSheet.Part> parts = new ArrayList<>();
        for (ImportedSheet.Part part : read.parts()) {
            String libraryId = read.footprints().get(part.id());
            Optional<String> ours = libraryId == null ? Optional.empty() : Optional.ofNullable(paired.get(libraryId));
            if (libraryId == null || (ours.isEmpty() && !found.containsKey(libraryId)) || made.contains(part.component())) {
                parts.add(part);
                continue;
            }
            String slug = ours.orElseGet(() -> KiCadFootprintDrawing.slugOf(libraryId));
            if (catalogue.footprintOf(part.component()).map(slug::equals).orElse(false)) {
                parts.add(part);
                continue;
            }
            if (ours.isEmpty()) {
                Set<String> pads = found.get(libraryId).padNumbers();
                List<String> missing = catalogue.pinsByNumber(part.component()).keySet().stream()
                        .filter(number -> !pads.contains(number)).toList();
                if (!missing.isEmpty()) {
                    notes.add(part.id() + " keeps " + part.component() + "'s footprint: " + libraryId
                            + " has no pad " + String.join(", ", missing));
                    parts.add(part);
                    continue;
                }
            }
            parts.add(part.onFootprint(slug));
        }
        return new ImportedSheet(parts, read.items(), read.wires(), read.probes(), read.ports(), read.globals(), read.symbols(),
                read.components(), read.notes(), read.footprints());
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
        return Optional.of(KiCadFootprintDrawing.slugOf(libraryId));
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

    private static String slugOf(String name) {
        String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "kicad-design" : slug;
    }
}
