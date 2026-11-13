package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.table.TableModule;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.TermDocumentWriter;
import org.jmouse.core.io.PortablePaths;
import org.jmouse.cad.pcb.FootprintLinesParser;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.cad.packages.footprint.FootprintGeneration;
import org.jmouse.cad.packages.model.PackageModels;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.cad.packages.footprint.LandPatternRules;
import org.jmouse.cad.packages.portrait.PackagePortraits;
import org.jmouse.cad.packages.portrait.PortraitRules;
import org.jmouse.cad.packages.symbol.ConnectorSymbols;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.sheet.Sheet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The library's standard documents, regenerated from its tables: every package table under {@value #TABLES} that
 * names a {@code form} — its footprints by the land pattern rules, its portraits by the portrait rules — and the
 * connector symbols of {@value #TABLES}, the 3D models {@value #PACKAGE_MODELS} draws for the footprints, each
 * written as terms into its folder — and the library's index
 * ({@value CadLibrary#INDEX}), every document file under it by its slug, which a jar cannot list. A generated document is
 * regenerated, never edited; {@code check} names every written one the tables no longer give, and an index that no
 * longer lists the files.
 *
 * <pre>
 * PackageRegeneration write|check &lt;the library's resource folder&gt;
 * </pre>
 */
public final class PackageRegeneration {

    /** Where the library's tables stand, each a table document: {@code tables/land-pattern-rules.table.jm}. */
    static final String TABLES         = "tables";
    static final String LAND_PATTERNS  = CadStandards.LAND_PATTERN_RULES;
    static final String PORTRAIT_RULES = "package-portrait-rules";
    static final String PACKAGE_MODELS = "package-models";
    static final String CONNECTORS     = "connector-symbols";

    private static final String TABLE_SUFFIX = TableModule.TABLE.fileSuffix();
    private static final DocumentLanguage TABLE_LANGUAGE = DocumentLanguage.of(new TableModule());

    /** A document's file: its slug, then its kind's word — `soic-8-footprint.footprint.jm`. */
    private static final Pattern DOCUMENT_FILE = Pattern.compile("(.+)\\.[a-z-]+\\.jm");

    /** What the index says of itself, above its lines. */
    private static final String INDEX_HEAD = "#The documents of the jmCAD library: slug = path, beside CadLibrary — written by PackageRegeneration";

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
                System.out.println("DIFFERS " + PortablePaths.below(library, file));
            }
        }
        String index = indexOf(library);
        Path indexFile = library.resolve(CadLibrary.INDEX);
        if (writing) {
            write(indexFile, index);
        } else if (!Files.exists(indexFile) || !index.equals(read(indexFile))) {
            differing += 1;
            System.out.println("DIFFERS " + CadLibrary.INDEX);
        }
        System.out.println(generated.size() + " documents " + (writing ? "written" : "generated, " + differing + " differ from the library"));
    }

    /**
     * The index: every document file under the library, written `slug=path`, in the order of the slugs. Two files of one slug
     * are refused, naming both: an index keeps one path a slug, and keeping either would lose the other without a word.
     */
    private static String indexOf(Path library) {
        Map<String, String> paths = new TreeMap<>();
        List<String> twice = new ArrayList<>();
        try (Stream<Path> files = Files.walk(library)) {
            files.filter(Files::isRegularFile).sorted().forEach(file -> {
                Matcher document = DOCUMENT_FILE.matcher(file.getFileName().toString());
                if (document.matches()) {
                    String path = PortablePaths.below(library, file);
                    String earlier = paths.putIfAbsent(document.group(1), path);
                    if (earlier != null) {
                        twice.add("'" + document.group(1) + "' in " + earlier + " and " + path);
                    }
                }
            });
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        if (!twice.isEmpty()) {
            throw new IllegalStateException("A slug names one document of the library; these name two: " + String.join("; ", twice));
        }
        StringBuilder index = new StringBuilder(INDEX_HEAD).append('\n');
        paths.forEach((slug, path) -> index.append(slug).append('=').append(path).append('\n'));
        return index.toString();
    }

    /** Every footprint, portrait and symbol the library's tables give, by the rules written beside them. */
    private static List<Generated> generate(Path library) {
        Path standards = library.resolve(TABLES);
        LandPatternRules landPatterns = LandPatternRules.of(table(standards.resolve(LAND_PATTERNS + TABLE_SUFFIX)));
        PortraitRules portraits = PortraitRules.of(table(standards.resolve(PORTRAIT_RULES + TABLE_SUFFIX)));
        List<Sheet> tables = packageTables(standards);
        List<Generated> generated = new ArrayList<>();
        List<FootprintGeneration.FormedFootprint> footprints = FootprintGeneration.generateFormed(landPatterns, tables);
        // Each footprint whose body the table of package models draws gets its model, and names it.
        Map<String, PackageModels.StandardModel> models = new LinkedHashMap<>();
        new PackageModels(table(standards.resolve(PACKAGE_MODELS + TABLE_SUFFIX))).generate(footprints)
                .forEach(model -> models.put(model.footprint(), model));
        for (FootprintGeneration.FormedFootprint formed : footprints) {
            DocumentNode footprint = formed.footprint().document();
            PackageModels.StandardModel model = models.get(formed.footprint().slug());
            if (model != null) {
                // Named by the model's code, as every reference names a document.
                footprint.addExpression(LineBuilder.line(FootprintLinesParser.MODEL).slot("reference", Nodes.text(model.document().referenceName())).build());
                generated.add(new Generated("models", model.folder(), ModelModule.MODEL, model.document()));
            }
            generated.add(new Generated("footprints", formed.footprint().folder(), PcbModule.FOOTPRINT, footprint));
        }
        PackagePortraits.generate(portraits, landPatterns.filing(), tables).forEach(portrait -> generated.add(new Generated(
                "portraits", portrait.folder(), SchematicModule.PORTRAIT, portrait.document())));
        ConnectorSymbols.generate(table(standards.resolve(CONNECTORS + TABLE_SUFFIX))).forEach(symbol -> generated.add(
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
        return TableModule.sheetOf(TABLE_LANGUAGE.readOne(read(file), file.getFileName().toString()));
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
