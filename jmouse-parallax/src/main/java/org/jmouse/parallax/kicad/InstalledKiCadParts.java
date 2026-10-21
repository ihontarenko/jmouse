package org.jmouse.parallax.kicad;

import org.jmouse.kicad.footprint.KiCadFootprintIndex;
import org.jmouse.kicad.parser.KiCadParseException;
import org.jmouse.kicad.parser.KiCadParser;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;
import org.jmouse.kicad.symbol.KiCadSymbolIndex;
import org.jmouse.kicad.symbol.KiCadSymbolLibrary;
import org.jmouse.parallax.part.FetchedPart;
import org.jmouse.parallax.part.PartCandidate;
import org.jmouse.parallax.part.PartQuery;
import org.jmouse.parallax.part.PartSearchProvider;
import org.jmouse.parallax.part.ProviderState;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.Meta;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
 * <h2>⚠️ Nothing of KiCad's drawing is converted</h2>
 *
 * <p>KiCad's libraries are licensed CC-BY-SA 4.0 with the KiCad libraries exception, and a converted symbol or
 * footprint would be an adaptation of them. So a fetched symbol becomes a component from its facts alone — pin
 * numbers, names, kinds — drawn as our box ({@code PinBox}); its footprint is named, not converted, and the part lands
 * on one of our footprints once one is chosen. A footprint is listed so it can be found, and is not fetched. The
 * component carries the library's licence and source, since its description and pin names are read from it.</p>
 */
public final class InstalledKiCadParts implements PartSearchProvider {

    /** The licence KiCad's own libraries come under, which every document converted from them keeps. */
    public static final String KICAD_LIBRARY_LICENSE = "CC-BY-SA-4.0 WITH KiCad-libraries-exception";

    private static final String SYMBOL_PREFIX = "symbol:";
    private static final String FOOTPRINT_PREFIX = "footprint:";
    private static final String SYMBOL_SUFFIX = ".kicad_sym";
    private static final String SOURCE_LINK = "https://gitlab.com/kicad/libraries";

    private final List<Path> symbolFolders;
    private final List<Path> footprintFolders;
    private final Executor indexing;
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
     * @param indexing         where the libraries are read the first time they are asked about
     */
    public InstalledKiCadParts(List<Path> symbolFolders, List<Path> footprintFolders, Executor indexing) {
        this.symbolFolders = List.copyOf(symbolFolders);
        this.footprintFolders = List.copyOf(footprintFolders);
        this.indexing = indexing;
    }

    @Override
    public String code() {
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
                    + "set jmouse.parallax.kicad.symbol-libraries and footprint-libraries.");
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
                + "Fetched parts stay under " + KICAD_LIBRARY_LICENSE + ".");
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
            return fetchFootprint(identifier.substring(FOOTPRINT_PREFIX.length()));
        }
        throw new IllegalArgumentException("A KiCad part is named 'symbol:LIBRARY:NAME' or 'footprint:LIBRARY:NAME'.");
    }

    private FetchedPart fetchSymbol(String libraryIdentifier) {
        KiCadSymbolDefinition symbol = readSymbol(libraryIdentifier);
        if (symbol.power()) {
            throw new IllegalArgumentException(libraryIdentifier + " is a power symbol — a net's name drawn, not a part.");
        }
        List<ExperienceDocument> documents = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        String written = symbol.property("Footprint");
        if (!written.isBlank()) {
            notes.add("KiCad names its footprint " + written + "; KiCad's footprints are not converted — choose one of ours for the part.");
        }
        ExperienceDocument component = licensed(KiCadComponentStub.documentOf(symbol, Optional.empty()),
                symbol.property("Datasheet"));
        documents.add(component);
        return new FetchedPart(documents, component.slug(), notes);
    }

    private FetchedPart fetchFootprint(String libraryIdentifier) {
        throw new IllegalArgumentException("KiCad's footprint " + libraryIdentifier + " is not converted into ours: its "
                + "drawing stays KiCad's. Choose the footprint of our library for that package.");
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
            KiCadSymbolLibrary read = KiCadSymbolLibrary.of(new KiCadParser().parse(library,
                    Files.readString(file, StandardCharsets.UTF_8)));
            return read.symbol(name).orElseThrow(() -> new IllegalArgumentException("The KiCad library " + library
                    + " holds no symbol " + name + "."));
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The KiCad library " + file + " cannot be read", unreadable);
        } catch (KiCadParseException unreadable) {
            throw new IllegalArgumentException("The KiCad library " + library + " does not read: " + unreadable.getMessage(),
                    unreadable);
        }
    }

    /** The document under KiCad's library licence, saying where it came from, its datasheet linked when it has one. */
    private static ExperienceDocument licensed(ExperienceDocument document, String datasheet) {
        Meta meta = document.meta();
        List<Meta.Link> links = new ArrayList<>(meta.links());
        if (datasheet != null && datasheet.startsWith("http")) {
            links.add(new Meta.Link("Datasheet", datasheet));
        }
        links.add(new Meta.Link("KiCad libraries", SOURCE_LINK));
        Meta licensed = new Meta(meta.summary(), meta.description(), meta.category(), meta.tags(), meta.source(),
                meta.authors(), KICAD_LIBRARY_LICENSE, links, meta.fields(), meta.labels());
        return new ExperienceDocument(document.kind(), document.title(), document.slug(), document.type(), licensed,
                document.base(), document.units(), document.uses(), document.blocks(), document.line());
    }

    private PartCandidate candidateOf(KiCadSymbolIndex.Entry symbol) {
        String datasheet = symbol.property("Datasheet");
        return new PartCandidate(code(), SYMBOL_PREFIX + symbol.identifier(), PartCandidate.Kind.COMPONENT,
                symbol.identifier(), null, null, symbol.description(),
                datasheet.startsWith("http") ? datasheet : null, null, KICAD_LIBRARY_LICENSE, null,
                !symbol.name().startsWith("#") && !symbol.property("Reference").startsWith("#"));
    }

    private PartCandidate candidateOf(KiCadFootprintIndex.Entry footprint) {
        return new PartCandidate(code(), FOOTPRINT_PREFIX + footprint.identifier(), PartCandidate.Kind.FOOTPRINT,
                footprint.identifier(), null, null, null, null, null, KICAD_LIBRARY_LICENSE, null, false);
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
