package org.jmouse.kicad;

import org.jmouse.kicad.parser.KiCadParser;
import org.jmouse.kicad.schematic.KiCadSchematic;
import org.jmouse.kicad.schematic.KiCadSheet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A design: its root schematic and every sheet it places, each instance of a sheet once — a sheet placed twice is two
 * instances of one file, with a path of its own: {@code /root-uuid/sheet-uuid}, the path KiCad keeps references under.
 */
public final class KiCadProject {

    /** One instance of a sheet in the design. */
    public record SheetInstance(String path, String name, KiCadSchematic schematic, SheetInstance parent, KiCadSheet placedBy) {

        /** Whether it is the root's instance. */
        public boolean root() {
            return parent == null;
        }
    }

    private final List<SheetInstance> sheets;

    private KiCadProject(List<SheetInstance> sheets) {
        this.sheets = List.copyOf(sheets);
    }

    /**
     * The design whose root schematic is that file; its sheets are read beside it, by the file each sheet box names.
     *
     * @throws UncheckedIOException when a file cannot be read
     */
    public static KiCadProject load(Path rootSchematic) {
        Path folder = rootSchematic.toAbsolutePath().getParent();
        KiCadParser parser = new KiCadParser();
        Map<String, KiCadSchematic> read = new HashMap<>();
        return of(rootSchematic.getFileName().toString(), file -> read.computeIfAbsent(file, name -> {
            try {
                return KiCadSchematic.of(parser.parse(name, Files.readString(folder.resolve(name), StandardCharsets.UTF_8)));
            } catch (IOException unreadable) {
                throw new UncheckedIOException("A sheet's file " + name + " cannot be read", unreadable);
            }
        }));
    }

    /**
     * The design whose root is the file of that name, every file given by {@code files} — a folder, an archive, a test.
     *
     * @throws IllegalArgumentException when a sheet places itself, through any number of sheets
     */
    public static KiCadProject of(String rootFile, Function<String, KiCadSchematic> files) {
        KiCadSchematic root = files.apply(rootFile);
        List<SheetInstance> sheets = new ArrayList<>();
        place(new SheetInstance("/" + root.uuid(), rootFile, root, null, null), files, sheets);
        return new KiCadProject(sheets);
    }

    private static void place(SheetInstance instance, Function<String, KiCadSchematic> files, List<SheetInstance> into) {
        into.add(instance);
        for (KiCadSheet sheet : instance.schematic().sheets()) {
            for (SheetInstance above = instance; above != null; above = above.parent()) {
                if (above.schematic().name().equals(sheet.file())) {
                    throw new IllegalArgumentException(instance.schematic().name() + " places " + sheet.file()
                            + ", which places it in turn: a sheet cannot hold itself.");
                }
            }
            place(new SheetInstance(instance.path() + "/" + sheet.uuid(), sheet.name(), files.apply(sheet.file()), instance, sheet),
                    files, into);
        }
    }

    /** Every sheet instance, the root first and each sheet before the sheets it places. */
    public List<SheetInstance> sheets() {
        return sheets;
    }

    public SheetInstance root() {
        return sheets.getFirst();
    }
}
