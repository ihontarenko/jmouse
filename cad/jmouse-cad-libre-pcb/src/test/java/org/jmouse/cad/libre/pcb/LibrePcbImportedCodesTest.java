package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.libre.pcb.LibrePcbLibrary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A LibrePCB element brought before imports named by codes is given the code the import gives it now. */
class LibrePcbImportedCodesTest {

    private static final LibrePcbLibrary.Footprint FOOTPRINT = new LibrePcbLibrary.Footprint("d61e4907-8f2b-4c1a-9e7d-0a1b2c3d4e5f", "default",
            List.of(), List.of(), List.of(), List.of(), new LibrePcbLibrary.Axes(0, 0, 0), new LibrePcbLibrary.Axes(0, 0, 0));
    private static final LibrePcbLibrary.Package PACKAGE = new LibrePcbLibrary.Package("4b616942-1111-2222-3333-444455556666", "CAPC1608 (0603)",
            "", "", Map.of(), Map.of(), List.of(FOOTPRINT));

    @Test
    void aFootprintIsCodedByItsOwnIdWhichItsSlugKeeps() {
        Optional<String> recovered = LibrePcbFiling.shipped().codeOfImported(PcbModule.FOOTPRINT, LibrePcbNames.footprint(PACKAGE, FOOTPRINT),
                new Provenance(LibrePcbNames.source("package", PACKAGE.name(), PACKAGE.id()), null, null));
        assertEquals(Optional.of(LibrePcbNames.footprintCode(PACKAGE, FOOTPRINT)), recovered);
    }

    @Test
    void aModelIsCodedByItsNameAndIdAsItsSourceSaysThem() {
        String modelId = "a1b2c3d4-0000-0000-0000-000000000000";
        Optional<String> recovered = LibrePcbFiling.shipped().codeOfImported(ModelModule.MODEL, LibrePcbNames.model(PACKAGE, modelId),
                new Provenance(LibrePcbNames.source("model", PACKAGE.name(), modelId), null, null));
        assertEquals(Optional.of(LibrePcbNames.modelCode(PACKAGE, modelId)), recovered);
    }
}
