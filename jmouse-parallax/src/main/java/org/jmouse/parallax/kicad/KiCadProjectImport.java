package org.jmouse.parallax.kicad;

import org.jmouse.kicad.KiCadProject;
import org.jmouse.kicad.KiCadProject.SheetInstance;
import org.jmouse.parallax.script.engine.DefinedObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A KiCad design as jMX documents: a circuit for each sheet file — the root's places the others as blocks, as KiCad's
 * hierarchy does —, and a symbol for each KiCad symbol a sheet draws with KiCad's own drawing, once however many
 * sheets use it.
 *
 * @param circuits every sheet file's circuit, the root's first
 * @param symbols  the symbols the circuits draw with, each unit once; the catalogue keeps one it already has
 * @param notes    what the import could not carry, by part: a component the library map does not name, a value that is
 *                 not a number, a unit drawn by another's place
 */
public record KiCadProjectImport(List<DefinedObject> circuits, List<DefinedObject> symbols, List<String> notes) {

    /**
     * @param name what the design is called: its project's name, {@code LINEAR_500W_PS-1}
     */
    public static KiCadProjectImport of(KiCadProject project, String name, KiCadLibraryMap library,
                                        KiCadCatalogue catalogue) {
        String root = slugOf(name);
        Map<String, String> slugs = new LinkedHashMap<>();
        for (SheetInstance sheet : project.sheets()) {
            String file = sheet.schematic().name();
            slugs.putIfAbsent(file, sheet.root() ? root : root + "-" + slugOf(file.replaceFirst("\\.kicad_sch$", "")));
        }
        List<DefinedObject> circuits = new ArrayList<>();
        Set<KiCadSheetImport.SymbolUse> uses = new LinkedHashSet<>();
        List<String> notes = new ArrayList<>();
        Set<String> imported = new LinkedHashSet<>();
        for (SheetInstance sheet : project.sheets()) {
            String file = sheet.schematic().name();
            if (!imported.add(file)) {
                continue;
            }
            KiCadSheetImport.Sheet read = KiCadSheetImport.of(sheet.schematic(), sheet.path(), library, catalogue,
                    placed -> slugs.getOrDefault(placed, slugOf(placed)));
            String title = sheet.root() ? name : name + " — " + sheet.name();
            circuits.add(new DefinedObject(slugs.get(file), title, "Imported from KiCad: " + file, null, "circuits",
                    List.of("electronics", "kicad"), false, null, read.circuit(), List.of(), List.of(), List.of(),
                    List.of(), List.of()));
            uses.addAll(read.symbols());
            read.notes().forEach(note -> notes.add(file + ": " + note));
        }
        List<DefinedObject> symbols = new ArrayList<>();
        Set<String> drawn = new LinkedHashSet<>();
        for (KiCadSheetImport.SymbolUse use : uses) {
            DefinedObject symbol = KiCadSymbolDrawing.object(use.symbol(), use.unit(), use.pinNames());
            if (drawn.add(symbol.slug())) {
                symbols.add(symbol);
            }
        }
        return new KiCadProjectImport(List.copyOf(circuits), List.copyOf(symbols), List.copyOf(notes));
    }

    private static String slugOf(String name) {
        String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "kicad-design" : slug;
    }
}
