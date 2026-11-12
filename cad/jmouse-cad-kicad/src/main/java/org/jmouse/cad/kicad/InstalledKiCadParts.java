package org.jmouse.cad.kicad;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.part.FetchedPart;
import org.jmouse.cad.document.part.PartCandidate;
import org.jmouse.cad.document.part.PartQuery;
import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.document.part.ProviderState;
import org.jmouse.el.grammar.ListNode;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintIndex;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.sexpression.SExpressionParseException;
import org.jmouse.sexpression.SExpressionParser;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;
import org.jmouse.kicad.symbol.KiCadSymbolIndex;
import org.jmouse.kicad.symbol.KiCadSymbolLibrary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * 🔎 The KiCad libraries installed beside the server — {@code share/kicad/symbols} and {@code share/kicad/footprints} —
 * as a place to find parts: a symbol is offered as a component, a footprint as itself.
 *
 * <p>The symbols are indexed once, in the background, on the first question: some two hundred megabytes, walked as
 * tokens ({@link KiCadSymbolIndex}) rather than built. Until the walk ends the provider is
 * {@link ProviderState.Status#INDEXING}. The footprints are listed by file name and read only when one is fetched.</p>
 *
 * <h2>⚠️ What is converted stays KiCad's, and says so</h2>
 *
 * <p>A fetched symbol becomes a component with KiCad's drawing — a symbol of several units is our box, as an import draws
 * it — landing on the footprint KiCad names for it, converted too, when every pin number is a pad there. A footprint is
 * fetched alone as well. Each document is an adaptation of KiCad's libraries, so each carries their holder, their
 * licence — CC-BY-SA 4.0 with the KiCad libraries exception — and where it was taken ({@link KiCadLibraries#provenance});
 * a design that uses them is not bound by the share-alike, the libraries' exception says so.</p>
 */
public final class InstalledKiCadParts implements PartSearchProvider {

    private static final String SYMBOL_PREFIX = "symbol:";
    private static final String FOOTPRINT_PREFIX = "footprint:";
    private static final String SYMBOL_SUFFIX = ".kicad_sym";
    private static final String WEB = "http";

    private final List<Path> symbolFolders;
    private final List<Path> footprintFolders;
    private final KiCadFootprintLibraries footprints;
    private final Executor indexing;
    private final KiCadLibraries libraries;
    private final AtomicBoolean started = new AtomicBoolean();
    private volatile Index index;
    private volatile String failure;

    private record Index(List<KiCadSymbolIndex.Entry> symbols, List<KiCadFootprintIndex.Entry> footprints) {
    }

    private record Scored(PartCandidate candidate, int score) {
    }

    /**
     * @param symbolFolders    folders of {@code *.kicad_sym} libraries
     * @param footprintFolders folders of {@code *.pretty} libraries
     * @param footprints       the footprints in those folders, read when one is fetched
     * @param indexing         where the libraries are read the first time they are asked about
     */
    public InstalledKiCadParts(List<Path> symbolFolders, List<Path> footprintFolders, KiCadFootprintLibraries footprints,
                               Executor indexing, KiCadLibraries libraries) {
        this.symbolFolders = List.copyOf(symbolFolders);
        this.footprintFolders = List.copyOf(footprintFolders);
        this.footprints = footprints;
        this.indexing = indexing;
        this.libraries = libraries;
    }

    @Override
    public String id() {
        return "kicad";
    }

    @Override
    public String name() {
        return "KiCad libraries";
    }

    @Override
    public ProviderState state() {
        if (symbolFolders.stream().noneMatch(Files::isDirectory) && footprintFolders.stream().noneMatch(Files::isDirectory)) {
            return ProviderState.notConfigured("No KiCad library folder is configured, or none of them exists on the server: "
                    + "set the KiCad symbol and footprint library folders.");
        }
        if (failure != null) {
            return ProviderState.failed(failure);
        }
        Index read = index;
        if (read == null) {
            startIndexing();
            return ProviderState.indexing("Reading the installed KiCad libraries — the first time takes a little while.");
        }
        return ProviderState.ready(read.symbols().size() + " symbols, " + read.footprints().size() + " footprints. "
                + "Fetched parts stay under " + libraries.license() + ".");
    }

    @Override
    public List<PartCandidate> search(String query, int limit) {
        PartQuery words = PartQuery.of(query);
        Index read = index;
        if (read == null || words.isEmpty()) {
            startIndexing();
            return List.of();
        }
        List<Scored> found = new ArrayList<>();
        for (KiCadSymbolIndex.Entry symbol : read.symbols()) {
            int score = words.score(symbol.identifier(), symbol.property("ki_keywords"), symbol.description());
            if (score > 0) {
                found.add(new Scored(candidateOf(symbol), score));
            }
        }
        for (KiCadFootprintIndex.Entry footprint : read.footprints()) {
            int score = words.score(footprint.identifier(), "", "");
            if (score > 0) {
                found.add(new Scored(candidateOf(footprint), score));
            }
        }
        return found.stream().sorted(Comparator.comparingInt(Scored::score).reversed()
                        .thenComparing(scored -> scored.candidate().identifier()))
                .limit(limit).map(Scored::candidate).toList();
    }

    @Override
    public FetchedPart fetch(String identifier) {
        if (identifier != null && identifier.startsWith(SYMBOL_PREFIX)) {
            return fetchSymbol(identifier.substring(SYMBOL_PREFIX.length()));
        }
        if (identifier != null && identifier.startsWith(FOOTPRINT_PREFIX)) {
            String libraryId = identifier.substring(FOOTPRINT_PREFIX.length());
            DocumentNode footprint = footprintDocument(libraryId).orElseThrow(() -> new IllegalArgumentException(
                    "No installed KiCad library holds the footprint " + libraryId + "."));
            return new FetchedPart(libraries.withLibraryCategories(List.of(footprint)), footprint.slug(), List.of());
        }
        throw new IllegalArgumentException("A KiCad part is named 'symbol:LIBRARY:NAME' or 'footprint:LIBRARY:NAME'.");
    }

    private FetchedPart fetchSymbol(String libraryIdentifier) {
        KiCadSymbolDefinition symbol = readSymbol(libraryIdentifier);
        if (symbol.power()) {
            throw new IllegalArgumentException(libraryIdentifier + " is a power symbol — a net's name drawn, not a part.");
        }
        List<String> notes = new ArrayList<>();
        // A footprint before the symbol, the symbol before the component that names both.
        List<DocumentNode> documents = new ArrayList<>();
        Optional<DocumentNode> footprint = landing(symbol, notes);
        footprint.ifPresent(documents::add);
        List<Integer> units = KiCadSymbolDrawing.units(symbol);
        if (units.size() == 1) {
            documents.add(KiCadSymbolDrawing.documentOf(symbol, units.getFirst(), Map.of(), libraries, true));
        }
        DocumentNode component = KiCadComponentStub.documentOf(symbol, footprint.map(DocumentNode::slug), Optional.empty(),
                false, libraries, true);
        linked(component, symbol.property("Datasheet"));
        documents.add(component);
        return new FetchedPart(libraries.withLibraryCategories(documents), component.slug(), notes);
    }

    /**
     * The footprint KiCad names for the symbol, converted, when an installed library holds it and every pin number of
     * the symbol is a pad there; otherwise none, and a note says why.
     */
    private Optional<DocumentNode> landing(KiCadSymbolDefinition symbol, List<String> notes) {
        String libraryId = symbol.property("Footprint").strip();
        if (libraryId.isEmpty()) {
            notes.add("KiCad names no footprint for " + symbol.name() + ": choose one for the part.");
            return Optional.empty();
        }
        Optional<KiCadFootprint> found = footprints.find(libraryId);
        if (found.isEmpty()) {
            notes.add("KiCad names the footprint " + libraryId + ", which no installed library holds: choose one for the part.");
            return Optional.empty();
        }
        Set<String> pads = found.get().padNumbers();
        List<String> missing = KiCadComponentStub.pinNumbers(symbol).stream().filter(number -> !pads.contains(number)).toList();
        if (!missing.isEmpty()) {
            notes.add("The part does not land on " + libraryId + ": its pins " + String.join(", ", missing) + " are no pad there.");
            return Optional.empty();
        }
        return footprintDocument(libraryId);
    }

    /** One of KiCad's installed footprints, converted; empty when none of the libraries holds it. */
    private Optional<DocumentNode> footprintDocument(String libraryId) {
        return footprints.find(libraryId).map(found -> KiCadFootprintDrawing.documentOf(libraryId, found,
                KiCadFootprintLibraries.Origin.INSTALLED, libraries));
    }

    private KiCadSymbolDefinition readSymbol(String libraryIdentifier) {
        int colon = libraryIdentifier.indexOf(':');
        if (colon <= 0 || colon == libraryIdentifier.length() - 1) {
            throw new IllegalArgumentException("A KiCad symbol is named 'LIBRARY:NAME', not '" + libraryIdentifier + "'.");
        }
        String library = libraryIdentifier.substring(0, colon);
        String name = libraryIdentifier.substring(colon + 1);
        Path file = symbolFolders.stream().map(folder -> folder.resolve(library + SYMBOL_SUFFIX))
                .filter(Files::isRegularFile).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No installed KiCad library is called " + library + "."));
        try {
            KiCadSymbolLibrary read = KiCadSymbolLibrary.of(new SExpressionParser().parse(library,
                    Files.readString(file, StandardCharsets.UTF_8)));
            return read.symbol(name).map(found -> found.inLibrary(library)).orElseThrow(() -> new IllegalArgumentException("The KiCad library " + library
                    + " holds no symbol " + name + "."));
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The KiCad library " + file + " cannot be read", unreadable);
        } catch (SExpressionParseException unreadable) {
            throw new IllegalArgumentException("The KiCad library " + library + " does not read: " + unreadable.getMessage(),
                    unreadable);
        }
    }

    /** The component linked to its datasheet, when it has one on the web, and to KiCad's libraries it was taken from. */
    private void linked(DocumentNode document, String datasheet) {
        MetaNode meta = document.block(MetaNode.class).orElseThrow();
        List<ListNode> links = new ArrayList<>();
        if (datasheet != null && datasheet.startsWith(WEB)) {
            links.add(link("Datasheet", datasheet));
        }
        links.add(link("KiCad libraries", libraries.home()));
        meta.addExpression(MetaLineNode.group(MetaNode.LINKS, links));
    }

    /** {@code "label" "https://…"}: a line of a links group. */
    private static ListNode link(String label, String address) {
        return new ListNode(List.of(Nodes.text(label), Nodes.text(address)), false);
    }

    private PartCandidate candidateOf(KiCadSymbolIndex.Entry symbol) {
        String datasheet = symbol.property("Datasheet");
        return new PartCandidate(id(), SYMBOL_PREFIX + symbol.identifier(), PartCandidate.Kind.COMPONENT,
                symbol.identifier(), null, null, symbol.description(),
                datasheet.startsWith(WEB) ? datasheet : null, null, libraries.license(), null,
                !symbol.name().startsWith("#") && !symbol.property("Reference").startsWith("#"), null);
    }

    private PartCandidate candidateOf(KiCadFootprintIndex.Entry footprint) {
        return new PartCandidate(id(), FOOTPRINT_PREFIX + footprint.identifier(), PartCandidate.Kind.FOOTPRINT,
                footprint.identifier(), null, null, null, null, null, libraries.license(), null, true, null);
    }

    private void startIndexing() {
        if (index != null || !started.compareAndSet(false, true)) {
            return;
        }
        indexing.execute(() -> {
            try {
                index = new Index(readSymbols(), KiCadFootprintIndex.list(footprintFolders));
            } catch (RuntimeException unreadable) {
                failure = "The installed KiCad libraries could not be read: " + unreadable.getMessage();
            }
        });
    }

    private List<KiCadSymbolIndex.Entry> readSymbols() {
        List<KiCadSymbolIndex.Entry> symbols = new ArrayList<>();
        for (Path folder : symbolFolders) {
            if (!Files.isDirectory(folder)) {
                continue;
            }
            try (Stream<Path> files = Files.list(folder)) {
                for (Path file : files.filter(path -> path.getFileName().toString().endsWith(SYMBOL_SUFFIX)).sorted().toList()) {
                    String fileName = file.getFileName().toString();
                    symbols.addAll(KiCadSymbolIndex.read(fileName.substring(0, fileName.length() - SYMBOL_SUFFIX.length()),
                            Files.readString(file, StandardCharsets.UTF_8)));
                }
            } catch (IOException unreadable) {
                throw new UncheckedIOException("The KiCad symbol libraries in " + folder + " cannot be read", unreadable);
            }
        }
        return List.copyOf(symbols);
    }
}
