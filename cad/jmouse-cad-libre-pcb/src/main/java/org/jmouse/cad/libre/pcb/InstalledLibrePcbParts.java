package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.document.part.FetchedPart;
import org.jmouse.cad.document.part.IndexedPart;
import org.jmouse.cad.document.part.InstalledLibrary;
import org.jmouse.cad.document.part.PartCandidate;
import org.jmouse.libre.pcb.LibrePcbFile;
import org.jmouse.libre.pcb.LibrePcbLibrary;
import org.jmouse.libre.pcb.LibrePcbLibraryHeader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 🔎 The LibrePCB libraries installed beside the server — the {@code *.lplib} folders LibrePCB's library manager keeps in a
 * workspace's {@code data/libraries/remote} and {@code local} — as a place to find parts: a device offered as a component landing
 * on its package, a package as a footprint, and a component no installed device makes as itself.
 *
 * <h2>⚠️ The libraries are one library</h2>
 *
 * <p>An element names elements of other libraries — LibrePCB's Integrated Circuits holds components whose symbols stand in its
 * Base —, so every installed library is read together and a reference is resolved across all of them.</p>
 *
 * <h2>The terms are each library's</h2>
 *
 * <p>What is fetched carries the terms of the library it comes from, where the filing table names them by the library's UUID
 * ({@link LibrePcbFiling#licenceOf}); a library it does not name claims none.</p>
 */
public final class InstalledLibrePcbParts implements InstalledLibrary {

    /** What the installed libraries are as a place parts are looked for. */
    public static final String ID   = "libre-pcb";
    public static final String NAME = "LibrePCB libraries";

    /** What it finds. */
    private static final Set<PartCandidate.Kind> OFFERED = Set.of(PartCandidate.Kind.COMPONENT, PartCandidate.Kind.FOOTPRINT);

    private static final String DEVICE_PREFIX    = "device:";
    private static final String PACKAGE_PREFIX   = "package:";
    private static final String COMPONENT_PREFIX = "component:";

    /** Every element of the installed libraries, by its UUID, with the library it stands in. */
    private record Shelf(Map<String, Placed> elements) {

        Optional<Placed> find(LibrePcbFile.Element kind, String id) {
            return Optional.ofNullable(elements.get(id)).filter(placed -> placed.kind() == kind);
        }

        List<Placed> all(LibrePcbFile.Element kind) {
            return elements.values().stream().filter(placed -> placed.kind() == kind).toList();
        }
    }

    /** One element: what it is, its file's path inside its library's folder, the library, and its text. */
    private record Placed(LibrePcbFile.Element kind, String path, Installed library, String text) {
    }

    /** One installed library: its folder and what it says of itself. */
    private record Installed(Path folder, LibrePcbLibraryHeader header) {
    }

    private final List<Path> folders;
    private final LibrePcbFiling filing;

    /** @param folders the folders LibrePCB keeps its libraries in, each holding {@code *.lplib} folders */
    public InstalledLibrePcbParts(List<Path> folders, LibrePcbFiling filing) {
        this.folders = List.copyOf(folders);
        this.filing = filing;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return NAME;
    }

    /** A device or a lone component is offered as a component, a package as a footprint. */
    @Override
    public Set<PartCandidate.Kind> offers() {
        return OFFERED;
    }

    @Override
    public boolean present() {
        return folders.stream().anyMatch(Files::isDirectory);
    }

    @Override
    public String absence() {
        return "No LibrePCB library folder is configured, or none of them exists on the server: set the folders LibrePCB keeps its "
                + "libraries in — a workspace's data/libraries/remote and data/libraries/local —, and install libraries with "
                + "LibrePCB's library manager.";
    }

    /** None for all of them: each library's own, which its parts carry. */
    @Override
    public Optional<String> license() {
        return Optional.empty();
    }

    @Override
    public List<IndexedPart> read() {
        Shelf shelf = shelf();
        // Read once, every element a part is offered by: its devices, components and packages.
        LibrePcbLibrary all = libraryOf(shelf.elements().values().stream().filter(placed -> placed.kind() != LibrePcbFile.Element.SYMBOL).toList());
        List<IndexedPart> parts = new ArrayList<>();
        Set<String> madeByDevice = new HashSet<>();
        for (LibrePcbLibrary.Device device : all.devices().values()) {
            Optional<LibrePcbLibrary.Component> component = Optional.ofNullable(all.components().get(device.component()));
            madeByDevice.add(device.component());
            String keywords = String.join(", ", device.keywords(), component.map(LibrePcbLibrary.Component::keywords).orElse(""));
            String description = device.description().isBlank() ? component.map(LibrePcbLibrary.Component::description).orElse("") : device.description();
            parts.add(new IndexedPart(candidate(DEVICE_PREFIX + device.id(), PartCandidate.Kind.COMPONENT, device.name(), description,
                    shelf.elements().get(device.id()).library()), keywords, description));
        }
        for (LibrePcbLibrary.Package found : all.packages().values()) {
            parts.add(new IndexedPart(candidate(PACKAGE_PREFIX + found.id(), PartCandidate.Kind.FOOTPRINT, found.name(), found.description(),
                    shelf.elements().get(found.id()).library()), found.keywords(), found.description()));
        }
        // A component no installed device makes — its packages in a library not installed — is still a part to draw.
        for (LibrePcbLibrary.Component component : all.components().values()) {
            if (!component.schematicOnly() && !madeByDevice.contains(component.id())) {
                parts.add(new IndexedPart(candidate(COMPONENT_PREFIX + component.id(), PartCandidate.Kind.COMPONENT, component.name(),
                        component.description(), shelf.elements().get(component.id()).library()), component.keywords(), component.description()));
            }
        }
        return parts;
    }

    @Override
    public FetchedPart fetch(String identifier) {
        Shelf shelf = shelf();
        if (identifier != null && identifier.startsWith(DEVICE_PREFIX)) {
            return fetchDevice(shelf, identifier.substring(DEVICE_PREFIX.length()));
        }
        if (identifier != null && identifier.startsWith(PACKAGE_PREFIX)) {
            return fetchPackage(shelf, identifier.substring(PACKAGE_PREFIX.length()));
        }
        if (identifier != null && identifier.startsWith(COMPONENT_PREFIX)) {
            return fetchComponent(shelf, identifier.substring(COMPONENT_PREFIX.length()), Optional.empty(), null);
        }
        throw new IllegalArgumentException("A LibrePCB part is named 'device:UUID', 'package:UUID' or 'component:UUID'.");
    }

    private FetchedPart fetchDevice(Shelf shelf, String id) {
        Placed placed = shelf.find(LibrePcbFile.Element.DEVICE, id)
                .orElseThrow(() -> new IllegalArgumentException("No installed LibrePCB library holds the device " + id + "."));
        LibrePcbLibrary.Device device = LibrePcbLibrary.read(Map.of(placed.path(), placed.text())).devices().get(id);
        return fetchComponent(shelf, device.component(), Optional.of(device), placed);
    }

    /** A component, in its first variant, landing on its device's package where it has one; its terms its device's library's. */
    private FetchedPart fetchComponent(Shelf shelf, String componentId, Optional<LibrePcbLibrary.Device> device, Placed devicePlaced) {
        Placed component = shelf.find(LibrePcbFile.Element.COMPONENT, componentId).orElseThrow(() -> new IllegalArgumentException(
                "No installed LibrePCB library holds the component " + componentId + "; install the library it stands in."));
        Map<String, Placed> needed = new LinkedHashMap<>();
        needed.put(component.path(), component);
        LibrePcbLibrary.Component read = LibrePcbLibrary.read(Map.of(component.path(), component.text())).components().get(componentId);
        read.variants().forEach(variant -> variant.gates().forEach(gate -> {
            Placed symbol = shelf.find(LibrePcbFile.Element.SYMBOL, gate.symbol()).orElseThrow(() -> new IllegalArgumentException(
                    read.name() + " is drawn with the symbol " + gate.symbol() + ", which no installed LibrePCB library holds."));
            needed.putIfAbsent(symbol.path(), symbol);
        }));
        device.ifPresent(given -> {
            needed.put(devicePlaced.path(), devicePlaced);
            Placed found = shelf.find(LibrePcbFile.Element.PACKAGE, given.packageId()).orElseThrow(() -> new IllegalArgumentException(
                    given.name() + " comes in the package " + given.packageId() + ", which no installed LibrePCB library holds."));
            needed.put(found.path(), found);
        });
        LibrePcbLibrary library = libraryOf(needed.values());
        LibrePcbLibraryDocuments made = documentsOf(library, needed.values(), devicePlaced != null ? devicePlaced : component);
        LibrePcbLibrary.Component taken = library.components().get(componentId);
        if (taken.variants().isEmpty()) {
            throw new IllegalArgumentException(taken.name() + " has no variant to draw it with.");
        }
        LibrePcbLibrary.Variant variant = taken.variants().getFirst();
        LibrePcbLibraryDocuments.Part part = made.part(taken, variant, device.map(given -> library.devices().get(given.id())),
                LibrePcbLibraryDocuments.namedPinsOf(List.of(variant)));
        List<String> notes = new ArrayList<>();
        if (taken.variants().size() > 1) {
            notes.add(taken.name() + " has " + taken.variants().size() + " variants: the first, " + variant.id() + ", is taken.");
        }
        if (part.drawn() instanceof LibrePcbComponents.Drawn.AsBox) {
            notes.add(taken.name() + ": " + variant.gates().size() + " gates, drawn as our box.");
        }
        if (device.isEmpty()) {
            notes.add(taken.name() + " comes in no installed package: choose a footprint for the part.");
        }
        return new FetchedPart(made.documents(), made.files(), part.slug(), notes);
    }

    private FetchedPart fetchPackage(Shelf shelf, String id) {
        Placed placed = shelf.find(LibrePcbFile.Element.PACKAGE, id)
                .orElseThrow(() -> new IllegalArgumentException("No installed LibrePCB library holds the package " + id + "."));
        LibrePcbLibrary library = libraryOf(List.of(placed));
        LibrePcbLibrary.Package found = library.packages().get(id);
        if (found.footprints().isEmpty()) {
            throw new IllegalArgumentException(found.name() + " has no footprint.");
        }
        LibrePcbLibrary.Footprint footprint = found.footprints().getFirst();
        LibrePcbLibraryDocuments made = documentsOf(library, List.of(placed), placed);
        made.footprint(found, footprint);
        List<String> notes = found.footprints().size() > 1
                ? List.of(found.name() + " has " + found.footprints().size() + " footprints: the first, " + footprint.name() + ", is taken.")
                : List.of();
        return new FetchedPart(made.documents(), made.files(), LibrePcbNames.footprint(found, footprint), notes);
    }

    /** The elements as one library, their paths as inside a library's folder. */
    private static LibrePcbLibrary libraryOf(Iterable<Placed> elements) {
        Map<String, String> files = new LinkedHashMap<>();
        elements.forEach(placed -> files.put(placed.path(), placed.text()));
        return LibrePcbLibrary.read(files);
    }

    /** What the elements become, claimed under the terms of the library {@code owner} stands in; models read beside each package. */
    private LibrePcbLibraryDocuments documentsOf(LibrePcbLibrary library, Iterable<Placed> elements, Placed owner) {
        Map<String, Path> packageFolders = new LinkedHashMap<>();
        elements.forEach(placed -> {
            if (placed.kind() == LibrePcbFile.Element.PACKAGE) {
                packageFolders.put(placed.path(), placed.library().folder());
            }
        });
        LibrePcbFiling terms = filing.licenceOf(owner.library().header().id()).map(filing::claiming).orElse(filing);
        return new LibrePcbLibraryDocuments(library, new LibrePcbLibraryDocuments.PackageModels() {
            @Override
            public Optional<byte[]> read(LibrePcbLibrary.Package found, String modelId) {
                String path = LibrePcbFile.Element.PACKAGE.folder() + found.id() + "/" + modelId + LibrePcbFile.MODEL_SUFFIX;
                return packageFolders.values().stream().map(folder -> folder.resolve(path)).filter(Files::isRegularFile).findFirst()
                        .map(InstalledLibrePcbParts::bytesOf);
            }

            @Override
            public String absence() {
                return "is not in its library's folder: install the library again with LibrePCB's library manager.";
            }
        }, Optional.empty(), terms);
    }

    private PartCandidate candidate(String identifier, PartCandidate.Kind kind, String title, String description, Installed library) {
        return new PartCandidate(ID, identifier, kind, title, null, null, description.isBlank() ? null : description, null, null,
                filing.licenceOf(library.header().id()).orElse(null), true, null);
    }

    // ─── the folders ─────────────────────────────────────────────────────────────────────────────────────────────────

    /** Every element of every installed library, by its UUID: the libraries in their folders' order, the first holding one keeping it. */
    private Shelf shelf() {
        Map<String, Placed> elements = new LinkedHashMap<>();
        for (Installed library : installed()) {
            for (LibrePcbFile.Element kind : LibrePcbFile.Element.values()) {
                Path folder = library.folder().resolve(kind.folder());
                if (!Files.isDirectory(folder)) {
                    continue;
                }
                for (Path element : listed(folder)) {
                    Path file = element.resolve(kind.file());
                    String id = element.getFileName().toString();
                    if (Files.isRegularFile(file) && !elements.containsKey(id)) {
                        elements.put(id, new Placed(kind, kind.folder() + id + "/" + kind.file(), library, textOf(file)));
                    }
                }
            }
        }
        return new Shelf(elements);
    }

    /** The libraries installed in the folders: each {@code *.lplib} folder holding what it says of itself. */
    private List<Installed> installed() {
        List<Installed> libraries = new ArrayList<>();
        for (Path folder : folders) {
            if (!Files.isDirectory(folder)) {
                continue;
            }
            for (Path library : listed(folder)) {
                Path header = library.resolve(LibrePcbLibraryHeader.FILE);
                if (LibrePcbFile.isLibrary(library.getFileName().toString()) && Files.isRegularFile(header)) {
                    libraries.add(new Installed(library, LibrePcbLibraryHeader.read(header.toString(), textOf(header))));
                }
            }
        }
        return libraries;
    }

    private static List<Path> listed(Path folder) {
        try (Stream<Path> entries = Files.list(folder)) {
            return entries.filter(Files::isDirectory).sorted(Comparator.naturalOrder()).toList();
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The LibrePCB library folder " + folder + " cannot be read", unreadable);
        }
    }

    private static String textOf(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The LibrePCB file " + file + " cannot be read", unreadable);
        }
    }

    private static byte[] bytesOf(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The LibrePCB model " + file + " cannot be read", unreadable);
        }
    }
}
