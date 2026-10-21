package org.jmouse.parallax.kicad;

import org.jmouse.kicad.KiCadProject;
import org.jmouse.kicad.KiCadProject.SheetInstance;
import org.jmouse.kicad.schematic.KiCadSymbolInstance;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.engine.DefinedObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
public record KiCadProjectImport(List<DefinedObject> circuits, List<ExperienceDocument> symbols, List<String> notes) {

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
        String ground = referenceGround(project);
        if (!ground.equals(KiCadNetNames.KICAD_GROUND)) {
            notes.add("The design has no GND: " + ground + " is node 0, the ground everything is measured from; its other "
                    + "grounds stay nets of their own.");
        }
        Set<String> imported = new LinkedHashSet<>();
        for (SheetInstance sheet : project.sheets()) {
            String file = sheet.schematic().name();
            if (!imported.add(file)) {
                continue;
            }
            KiCadSheetImport.Sheet read = KiCadSheetImport.of(sheet.schematic(), sheet.path(), ground, library, catalogue,
                    placed -> slugs.getOrDefault(placed, slugOf(placed)));
            String title = sheet.root() ? name : name + " — " + sheet.name();
            circuits.add(new DefinedObject(slugs.get(file), title, "Imported from KiCad: " + file, null, "circuits",
                    List.of("electronics", "kicad"), false, null, read.circuit(), List.of(), List.of(), List.of(),
                    List.of(), List.of()));
            uses.addAll(read.symbols());
            read.notes().forEach(note -> notes.add(file + ": " + note));
        }
        List<ExperienceDocument> symbols = new ArrayList<>();
        Set<String> drawn = new LinkedHashSet<>();
        for (KiCadSheetImport.SymbolUse use : uses) {
            ExperienceDocument symbol = KiCadSymbolDrawing.documentOf(use.symbol(), use.unit(), use.pinNames());
            if (drawn.add(symbol.slug())) {
                symbols.add(symbol);
            }
        }
        return new KiCadProjectImport(List.copyOf(circuits), List.copyOf(symbols), List.copyOf(notes));
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
