package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.part.FetchedFile;
import org.jmouse.libre.pcb.LibrePcbBoard;
import org.jmouse.libre.pcb.LibrePcbLibrary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What LibrePCB library elements become, each made once however many times it is asked for: a part's symbol, the component landing
 * on its package's footprint, that footprint and its 3D models — for a project being imported and for a part fetched from an
 * installed library alike. Each is kept before what names it: a model before its footprint, a footprint before its component.
 */
final class LibrePcbLibraryDocuments {

    /** Where a package's 3D models are read from: a project's files, an installed library's folder. */
    interface PackageModels {

        /** A model's geometry; empty when it is not there. */
        Optional<byte[]> read(LibrePcbLibrary.Package found, String modelId);

        /** What a reader is told of a model a package names and this source lacks, and what to do: {@code is not among the project's files: …}. */
        String absence();
    }

    /**
     * A part made: the component's slug, how it is drawn, and the code of the footprint it lands on — its package's first —;
     * null when it has no package.
     */
    record Part(String slug, LibrePcbComponents.Drawn drawn, String footprint) {
    }

    private final LibrePcbLibrary library;
    private final PackageModels models;
    private final Optional<LibrePcbBoard.DesignRules> rules;
    private final LibrePcbFiling filing;
    private final Map<String, DocumentNode> modelDocuments = new LinkedHashMap<>();
    private final List<FetchedFile> modelFiles = new ArrayList<>();
    private final Map<String, DocumentNode> footprints = new LinkedHashMap<>();
    private final Map<String, DocumentNode> symbols = new LinkedHashMap<>();
    private final Map<String, DocumentNode> components = new LinkedHashMap<>();

    /** @param rules the board's design rules, which a pad left to them is made with; empty where there is no board */
    LibrePcbLibraryDocuments(LibrePcbLibrary library, PackageModels models, Optional<LibrePcbBoard.DesignRules> rules, LibrePcbFiling filing) {
        this.library = library;
        this.models = models;
        this.rules = rules;
        // Within the library: each element filed by what it says it is.
        this.filing = filing.within(library);
    }

    /** The pins each symbol names, by symbol: what every gate of these variants draws with a name. */
    static Map<String, Set<String>> namedPinsOf(Collection<LibrePcbLibrary.Variant> variants) {
        Map<String, Set<String>> named = new HashMap<>();
        variants.forEach(variant -> variant.gates().forEach(gate -> named.computeIfAbsent(gate.symbol(), ignored -> new LinkedHashSet<>())
                .addAll(gate.namedPins())));
        return named;
    }

    /**
     * A part of the library in one of its variants and, where it has one, a device: its symbol when it is drawn by its gate, the
     * footprint its package offers first, and the component landing on it.
     *
     * @param namedPins the pins each symbol names, by symbol ({@link #namedPinsOf})
     */
    Part part(LibrePcbLibrary.Component component, LibrePcbLibrary.Variant variant, Optional<LibrePcbLibrary.Device> device,
              Map<String, Set<String>> namedPins) {
        LibrePcbComponents.Drawn drawn = LibrePcbComponents.drawnOf(library, variant);
        if (drawn instanceof LibrePcbComponents.Drawn.ByGate byGate) {
            symbols.computeIfAbsent(LibrePcbNames.symbol(byGate.symbol()), ignored -> LibrePcbSymbolDrawing.documentOf(byGate.symbol(),
                    namedPins.getOrDefault(byGate.symbol().id(), Set.of()), filing));
        }
        Optional<LibrePcbLibrary.Package> found = device.map(LibrePcbLibrary.Device::packageId).map(library.packages()::get);
        String usual = found.flatMap(given -> given.footprints().stream().findFirst().map(footprint -> footprint(given, footprint))).orElse(null);
        String slug = device.map(given -> LibrePcbNames.component(given.name(), given.id()))
                .orElseGet(() -> LibrePcbNames.component(component.name(), variant.id()));
        components.computeIfAbsent(slug, ignored -> LibrePcbComponents.documentOf(library, component, drawn, device, usual, slug, filing));
        return new Part(slug, drawn, usual);
    }

    /** A footprint of a package, made once with its models; the code every reference to it writes. */
    String footprint(LibrePcbLibrary.Package found, LibrePcbLibrary.Footprint footprint) {
        String slug = LibrePcbNames.footprint(found, footprint);
        footprints.computeIfAbsent(slug, ignored -> {
            footprint.models().forEach(model -> model(found, model));
            return LibrePcbFootprintDrawing.documentOf(found, footprint, rules, filing);
        });
        return LibrePcbNames.footprintCode(found, footprint);
    }

    /** Everything made, each before what names it: models, footprints, symbols, components. */
    List<DocumentNode> documents() {
        List<DocumentNode> made = new ArrayList<>(modelDocuments.values());
        made.addAll(footprints.values());
        made.addAll(symbols.values());
        made.addAll(components.values());
        return made;
    }

    /** The models' geometry, each filed against its document. */
    List<FetchedFile> files() {
        return List.copyOf(modelFiles);
    }

    private void model(LibrePcbLibrary.Package found, String modelId) {
        String slug = LibrePcbNames.model(found, modelId);
        if (modelDocuments.containsKey(slug)) {
            return;
        }
        byte[] bytes = models.read(found, modelId).orElseThrow(() -> new IllegalArgumentException("The package " + found.name()
                + " names the model " + modelId + ", which " + models.absence()));
        LibrePcbModelDrawing.Made made = LibrePcbModelDrawing.of(found, modelId, bytes, filing);
        modelDocuments.put(slug, made.document());
        modelFiles.add(made.file());
    }
}
