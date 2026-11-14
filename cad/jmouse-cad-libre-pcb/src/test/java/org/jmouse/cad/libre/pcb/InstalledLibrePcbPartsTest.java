package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.document.part.FetchedPart;
import org.jmouse.cad.document.part.IndexedPart;
import org.jmouse.cad.document.part.InstalledLibraryParts;
import org.jmouse.cad.document.part.PartCandidate;
import org.jmouse.cad.document.part.ProviderState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LibrePCB's installed libraries as a place parts are found: a device offered as a component landing on its package, the package
 * as a footprint, and a fetch making them — the symbol, the footprint with its 3D model, the component — under the terms of the
 * library they come from. The library is one resistor of LibrePCB's own Base, CC0, as LibrePCB installs it.
 */
class InstalledLibrePcbPartsTest {

    private static final String DEVICE  = "device:078650d3-483c-4b9e-a848-b14f1aad2edc";
    private static final String PACKAGE = "package:2d00d07c-bfc1-4a96-a1cb-195c5ff93db9";

    private static Path installed() throws URISyntaxException {
        return Path.of(InstalledLibrePcbPartsTest.class.getResource("installed").toURI());
    }

    @Test
    void aDeviceIsOfferedAsAComponentAndItsPackageAsAFootprintUnderTheLibrarysTerms() throws URISyntaxException {
        InstalledLibrePcbParts library = new InstalledLibrePcbParts(List.of(installed()), LibrePcbFiling.shipped());

        Map<String, PartCandidate> offered = library.read().stream().map(IndexedPart::candidate)
                .collect(Collectors.toMap(PartCandidate::identifier, Function.identity()));

        // The component is the device's: it is not offered again on its own.
        assertEquals(2, offered.size());
        assertEquals(PartCandidate.Kind.COMPONENT, offered.get(DEVICE).kind());
        assertEquals("Resistor 2012 (0805)", offered.get(DEVICE).title());
        assertEquals("CC0-1.0", offered.get(DEVICE).license());
        assertEquals(PartCandidate.Kind.FOOTPRINT, offered.get(PACKAGE).kind());
    }

    @Test
    void aSearchFindsTheDeviceByWhatItAndItsComponentAreLookedForBy() throws URISyntaxException {
        InstalledLibraryParts parts = new InstalledLibraryParts(new InstalledLibrePcbParts(List.of(installed()), LibrePcbFiling.shipped()),
                Runnable::run);
        parts.prepare();

        assertEquals(ProviderState.Status.READY, parts.state().status());
        assertEquals(DEVICE, parts.search("resistor 0805", Set.of(PartCandidate.Kind.COMPONENT), 5).getFirst().identifier());
    }

    @Test
    void aFetchedDeviceIsItsSymbolItsFootprintWithItsModelAndTheComponentLandingOnIt() throws URISyntaxException {
        FetchedPart fetched = new InstalledLibrePcbParts(List.of(installed()), LibrePcbFiling.shipped()).fetch(DEVICE);

        Map<DocumentKind, Long> kinds = fetched.documents().stream().collect(Collectors.groupingBy(DocumentNode::kind, Collectors.counting()));
        assertEquals(1, fetched.files().size());
        assertTrue(kinds.keySet().size() >= 4, "a model, a footprint, a symbol and a component: " + kinds);
        DocumentNode component = fetched.documents().getLast();
        assertEquals(fetched.open(), component.slug());
        // Each is filed by what LibrePCB says it is: its package a chip resistor, its component a resistor.
        Map<String, String> filed = fetched.documents().stream().collect(Collectors.toMap(document -> document.kind().word(),
                document -> document.block(MetaNode.class).flatMap(meta -> meta.text(MetaNode.CATEGORY)).orElse(""), (first, second) -> first));
        assertEquals("resistors-surface", filed.get("footprint"));
        assertEquals("resistors", filed.get("component"));
        // Each document carries the terms of the library it comes from.
        fetched.documents().forEach(document -> assertEquals("CC0-1.0", Provenance.of(document).license(), document.slug()));
    }

    @Test
    void aFolderWithNoLibraryIsSaidMissing(@TempDir Path empty) {
        InstalledLibrePcbParts nowhere = new InstalledLibrePcbParts(List.of(empty.resolve("absent")), LibrePcbFiling.shipped());

        assertFalse(nowhere.present());
    }
}
