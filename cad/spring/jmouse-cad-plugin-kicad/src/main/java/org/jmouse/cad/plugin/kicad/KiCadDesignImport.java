package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.table.ReadTable;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.document.part.FetchedFile;
import org.jmouse.cad.mesh.ModelSource;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;
import org.jmouse.kicad.symbol.KiCadSymbolLibrary;
import org.jmouse.cad.kicad.InstalledKiCadLibraries;
import org.jmouse.cad.kicad.KiCadComponentStub;
import org.jmouse.cad.kicad.KiCadSymbolDrawing;
import org.jmouse.cad.kicad.KiCadDesign;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.cad.kicad.KiCadLibraryMap;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.kicad.KiCadFootprintDrawing;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.sexpression.SExpressionParseException;
import org.jmouse.sexpression.SExpressionParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.function.Supplier;

/**
 * KiCad's files read as jM documents, by what the main file is ({@link KiCadFile}). A design — from its root schematic,
 * or from its project file through the schematic beside it — is its footprints, symbols and components, then a circuit
 * for each sheet; the other files are the sheets it places and the footprints of the project's own libraries
 * ({@code LIBRARY.pretty/NAME.kicad_mod}), and a footprint only KiCad holds is looked up in its installed libraries. The 3D
 * models a design's footprints name come with it from KiCad's installed model libraries, each once, and none the place
 * already holds. A footprint on its own is its one document, of the library its folder names.
 */
final class KiCadDesignImport implements DocumentImport {

    private static final String LIBRARY_SUFFIX = ".pretty";

    private final Supplier<InstalledKiCadLibraries> installedLibraries;
    private final Supplier<ModelSource>             installedModels;
    /** The map of KiCad's symbols to ours and KiCad's filings.current(), as the engine's tables say them at each use. */
    private final ReadTable<KiCadLibraryMap>        maps;
    private final ReadTable<KiCadLibraries>         filings;

    /**
     * @param installedLibraries the footprint and symbol libraries installed on the server, as they stand when a design is read
     * @param installedModels    the 3D model libraries installed on the server, as they stand when a design is read
     */
    KiCadDesignImport(Supplier<InstalledKiCadLibraries> installedLibraries, Supplier<ModelSource> installedModels,
                      ReadTable<KiCadLibraryMap> maps, ReadTable<KiCadLibraries> filings) {
        this.maps = maps;
        this.filings = filings;
        this.installedLibraries = installedLibraries;
        this.installedModels = installedModels;
    }

    @Override
    public String id() {
        return KiCadPlugin.FORMAT;
    }

    @Override
    public String title() {
        return KiCadPlugin.TITLE;
    }

    @Override
    public List<String> fileNames() {
        return KiCadFile.fileNames();
    }

    @Override
    public List<String> fileNamesReadAlone() {
        return KiCadFile.fileNamesReadAlone();
    }

    /** A footprint and a symbol library are somebody's work: whose, the reader says. */
    @Override
    public List<String> libraryFileNames() {
        return KiCadFile.libraryFileNames();
    }

    @Override
    public ImportedDocuments read(ExchangeFiles files, CatalogueDocuments catalogue) {
        String main = files.main();
        KiCadFile kind = KiCadFile.of(main).orElseThrow(() -> new IllegalArgumentException(
                "KiCad reads a schematic, a project, a board, a footprint or a symbol library (" + String.join(", ", KiCadFile.fileNames()) + "); '"
                + main + "' is none of them."));
        ImportedDocuments read = switch (kind) {
            case SCHEMATIC -> design(main, KiCadFile.SCHEMATIC.stemOf(main), List.of(), files, catalogue);
            case PROJECT -> designOpenedBy(main, KiCadFile.PROJECT, files, catalogue);
            case PCB -> designOpenedBy(main, KiCadFile.PCB, files, catalogue);
            // A library file is somebody's: whose it is, as the reader says it, where its own text says nothing.
            case FOOTPRINT -> footprint(main, files.mainText()).whoseAsStated(files.stated(), main);
            case SYMBOL_LIBRARY -> symbols(main, files.mainText()).whoseAsStated(files.stated(), main);
        };
        // Every KiCad library a drawing comes from is a category of its own, under KiCad's.
        return read.filedBy(filings.current());
    }

    /**
     * The design a project or a board opens: from the root schematic beside it of the same name, which KiCad itself opens it
     * by; a project copied under a new name keeps its old schematic, so the one schematic beside it answers when there is one.
     */
    private ImportedDocuments designOpenedBy(String main, KiCadFile kind, ExchangeFiles files, CatalogueDocuments catalogue) {
        String stem = kind.stemOf(main);
        String named = stem + KiCadFile.SCHEMATIC.suffix();
        if (files.files().containsKey(named)) {
            return design(named, stem, List.of(), files, catalogue);
        }
        List<String> beside = files.files().keySet().stream().filter(file -> file.endsWith(KiCadFile.SCHEMATIC.suffix()))
                .filter(file -> folderOf(file).equals(folderOf(main))).toList();
        if (beside.size() != 1) {
            throw new IllegalArgumentException(main + " opens its root schematic " + named + ", which is not beside it"
                    + (beside.isEmpty() ? "." : "; beside it are " + String.join(", ", beside) + ": open the root one."));
        }
        return design(beside.getFirst(), stem, List.of(main + " has no schematic of its name, so it opens the one beside it, "
                + beside.getFirst() + "."), files, catalogue);
    }

    /** The folder a file stands in: {@code a/b} of {@code a/b/c.kicad_sch}; empty for one at the top. */
    private static String folderOf(String file) {
        String normalized = file.replace('\\', '/');
        return normalized.contains("/") ? normalized.substring(0, normalized.lastIndexOf('/')) : "";
    }

    /** A file's own name, without its folders: {@code c.kicad_sch} of {@code a/b/c.kicad_sch}. */
    private static String fileNameOf(String file) {
        String normalized = file.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    /** One footprint, as its library's: {@code LIBRARY:NAME} where its folder is {@code LIBRARY.pretty}, else its name alone. */
    private ImportedDocuments footprint(String main, String text) {
        String name = KiCadFile.FOOTPRINT.stemOf(fileNameOf(main));
        String folder = fileNameOf(folderOf(main));
        String libraryId = folder.endsWith(LIBRARY_SUFFIX) ? folder.substring(0, folder.length() - LIBRARY_SUFFIX.length()) + ":" + name : name;
        KiCadFootprint footprint;
        try {
            footprint = KiCadFootprint.of(new SExpressionParser().parse(main, text));
        } catch (SExpressionParseException unreadable) {
            throw new IllegalArgumentException(unreadable.getMessage(), unreadable);
        }
        DocumentNode document = KiCadFootprintDrawing.documentOf(libraryId, footprint, KiCadFootprintLibraries.Origin.PROJECT, filings.current());
        return new ImportedDocuments(List.of(document), List.of(), List.of());
    }

    /** A symbol library: each symbol its drawing, one for each unit, and a component with its pins, to be given a footprint and a model. */
    private ImportedDocuments symbols(String main, String text) {
        KiCadSymbolLibrary library;
        try {
            library = KiCadSymbolLibrary.of(new SExpressionParser().parse(main, text));
        } catch (SExpressionParseException unreadable) {
            throw new IllegalArgumentException(unreadable.getMessage(), unreadable);
        }
        List<DocumentNode> documents = new ArrayList<>();
        // Each symbol named as KiCad names it outside the file — Device:R —, so it is the same document a design brings.
        String libraryName = KiCadFile.SYMBOL_LIBRARY.stemOf(fileNameOf(main));
        for (KiCadSymbolDefinition symbol : library.symbols().stream().map(found -> found.inLibrary(libraryName)).toList()) {
            KiCadSymbolDrawing.units(symbol).forEach(unit -> documents.add(KiCadSymbolDrawing.documentOf(symbol, unit, Map.of(), filings.current(), false)));
            documents.add(KiCadComponentStub.documentOf(symbol, Optional.empty(), Optional.empty(), false, filings.current(), false));
        }
        if (documents.isEmpty()) {
            throw new IllegalArgumentException(main + " holds no symbol.");
        }
        return new ImportedDocuments(documents, List.of(), List.of());
    }

    /**
     * @param stem   what the design's files are called: its board and its project are the files of that name, when they were sent
     * @param opened what opening it had to say
     */
    private ImportedDocuments design(String root, String stem, List<String> opened, ExchangeFiles files, CatalogueDocuments catalogue) {
        String title = files.title() == null || files.title().isBlank() ? KiCadFile.SCHEMATIC.stemOf(root) : files.title();
        return withModels(KiCadDesign.read(new KiCadDesign.Sources(files.files(), root, title, stem + KiCadFile.PCB.suffix(),
                stem + KiCadFile.PROJECT.suffix(), opened), catalogue, installedLibraries.get(), maps.current(), filings.current()), catalogue);
    }

    /** A design with the models its footprints name, each before the footprints, as what is named comes first; one the place holds is left. */
    private ImportedDocuments withModels(ImportedDocuments design, CatalogueDocuments catalogue) {
        ModelSource models = installedModels.get();
        Map<String, ModelSource.FetchedModel> brought = new LinkedHashMap<>();
        for (DocumentNode document : design.library()) {
            if (!document.kind().equals(PcbModule.FOOTPRINT)) {
                continue;
            }
            Set<String> named = FootprintDrawing.of(document).models().stream().map(FootprintDrawing.Model::reference).collect(Collectors.toSet());
            for (ModelSource.FetchedModel model : models.modelsOf(document)) {
                // A footprint names its model by the model's code: the name matched, and the one the place is asked for.
                String name = model.document().referenceName();
                if (named.contains(name) && !brought.containsKey(name) && catalogue.find(name).isEmpty()) {
                    brought.put(name, model);
                }
            }
        }
        if (brought.isEmpty()) {
            return design;
        }
        List<DocumentNode> library = new ArrayList<>(brought.values().stream().map(ModelSource.FetchedModel::document).toList());
        library.addAll(design.library());
        List<FetchedFile> files = new ArrayList<>(design.files());
        brought.values().forEach(model -> files.addAll(model.files()));
        return new ImportedDocuments(library, design.designs(), design.notes(), files);
    }
}
