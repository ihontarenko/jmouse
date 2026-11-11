package org.jmouse.cad.library;

import org.jmouse.cad.board.BoardModule;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.TermDocumentWriter;
import org.jmouse.cad.packages.footprint.FootprintGeneration;
import org.jmouse.cad.packages.footprint.LandPatternRules;
import org.jmouse.cad.packages.portrait.PackagePortraits;
import org.jmouse.cad.packages.portrait.PortraitRules;
import org.jmouse.cad.packages.symbol.ConnectorSymbols;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The library's standard documents, regenerated from its tables: every package table under {@value #STANDARDS} that
 * names a {@code form} — its footprints by the land pattern rules, its portraits by the portrait rules — and the
 * connector symbols of {@value #SYMBOL_STANDARDS}, each written as terms into its folder. A generated document is regenerated, never edited; {@code check} names every written one the
 * tables no longer give.
 *
 * <pre>
 * PackageRegeneration write|check &lt;the library's resource folder&gt;
 * </pre>
 */
public final class PackageRegeneration {

    static final String STANDARDS      = "standards/footprint-standards";
    static final String LAND_PATTERNS  = "land-pattern-rules.sht";
    static final String PORTRAIT_RULES = "package-portrait-rules.sht";
    static final String SYMBOL_STANDARDS = "standards/symbol-standards";
    static final String CONNECTORS     = "connector-symbols.sht";

    private static final String TABLE_SUFFIX = ".sht";

    /** A generated document, the folder of the library it is filed in, and its kind. */
    private record Generated(String root, String folder, DocumentKind kind, DocumentNode document) {
    }

    private PackageRegeneration() {
    }

    public static void main(String[] arguments) {
        if (arguments.length != 2 || !List.of("write", "check").contains(arguments[0])) {
            throw new IllegalArgumentException("PackageRegeneration write|check <the library's resource folder>");
        }
        boolean writing = arguments[0].equals("write");
        Path library = Path.of(arguments[1]);
        TermDocumentWriter terms = new TermDocumentWriter(true);
        int differing = 0;
        List<Generated> generated = generate(library);
        for (Generated document : generated) {
            Path file = library.resolve(document.root()).resolve(document.folder())
                    .resolve(document.document().slug() + document.kind().fileSuffix());
            String text = terms.write(document.document());
            if (writing) {
                write(file, text);
            } else if (!Files.exists(file) || !text.equals(read(file))) {
                differing += 1;
                System.out.println("DIFFERS " + library.relativize(file).toString().replace('\\', '/'));
            }
        }
        System.out.println(generated.size() + " documents " + (writing ? "written" : "generated, " + differing + " differ from the library"));
    }

    /** Every footprint, portrait and symbol the library's tables give, by the rules written beside them. */
    private static List<Generated> generate(Path library) {
        Path standards = library.resolve(STANDARDS);
        LandPatternRules landPatterns = LandPatternRules.of(table(standards.resolve(LAND_PATTERNS)));
        PortraitRules portraits = PortraitRules.of(table(standards.resolve(PORTRAIT_RULES)));
        List<Sheet> tables = packageTables(standards);
        List<Generated> generated = new ArrayList<>();
        FootprintGeneration.generate(landPatterns, tables).forEach(footprint -> generated.add(new Generated(
                "footprints", footprint.folder(), BoardModule.FOOTPRINT, footprint.document())));
        PackagePortraits.generate(portraits, landPatterns.filing(), tables).forEach(portrait -> generated.add(new Generated(
                "portraits", portrait.folder(), SchematicModule.PORTRAIT, portrait.document())));
        ConnectorSymbols.generate(table(library.resolve(SYMBOL_STANDARDS).resolve(CONNECTORS))).forEach(symbol -> generated.add(
                new Generated("symbols", symbol.folder(), SchematicModule.SYMBOL, symbol.document())));
        return generated;
    }

    /** The tables that name a form, in the order of their names; the rules name none. */
    private static List<Sheet> packageTables(Path standards) {
        try (Stream<Path> files = Files.list(standards)) {
            return files.filter(file -> file.toString().endsWith(TABLE_SUFFIX)).sorted().map(PackageRegeneration::table)
                    .filter(table -> FootprintGeneration.form(table).isPresent()).toList();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static Sheet table(Path file) {
        return SheetReader.read(file.getFileName().toString(), read(file));
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static void write(Path file, String document) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, document, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException("The document " + file + " cannot be written", failure);
        }
    }
}
