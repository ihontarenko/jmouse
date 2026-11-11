package org.jmouse.cad.management;

import org.jmouse.cad.store.catalogue.WrittenOwners;
import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.packages.board.BoardRules;
import org.jmouse.cad.packages.footprint.LandPatternRules;
import org.jmouse.cad.store.library.PartLibrary;
import org.jmouse.cad.store.library.PartLibrary.ComponentReference;
import org.jmouse.cad.store.library.PartLibrary.ComponentView;
import org.jmouse.cad.store.library.PartLibrary.DrawnDocument;
import org.jmouse.cad.store.library.PartLibrary.FootprintDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 🧰 The library a sheet and a board are built from — {@code JMOUSE_CAD_API.md} §2.3: components, symbols, portraits,
 * frames, footprints and the standards they are checked against.
 *
 * <p>Every route decides inside its service: a list holds the catalogue's documents the reader may see, and an owner's
 * own only when {@code owner} names one the reader may read ({@code TYPE:id}).</p>
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class LibraryController {

    private final PartLibrary library;

    public LibraryController(PartLibrary library) {
        this.library = library;
    }

    @GetMapping("/components")
    @PublicEndpoint("the catalogue's components the reader may see, and an owner's only where the reader may read")
    public List<ComponentReference> components(@RequestParam(required = false) String owner) {
        return library.components(WrittenOwners.optional(owner));
    }

    @GetMapping("/components/{reference}")
    @PublicEndpoint("a component is answered only when the reader may see it; a hidden one is not found")
    public ComponentView component(@PathVariable String reference, @RequestParam(required = false) String owner) {
        return library.component(reference, WrittenOwners.optional(owner));
    }

    @GetMapping("/symbols")
    @PublicEndpoint("the catalogue's symbols the reader may see, and an owner's only where the reader may read")
    public List<DrawnDocument> symbols(@RequestParam(required = false) String owner) {
        return library.symbols(WrittenOwners.optional(owner));
    }

    @GetMapping("/portraits")
    @PublicEndpoint("the catalogue's portraits the reader may see, and an owner's only where the reader may read")
    public List<DrawnDocument> portraits(@RequestParam(required = false) String owner) {
        return library.portraits(WrittenOwners.optional(owner));
    }

    @GetMapping("/frames")
    @PublicEndpoint("the catalogue's frames the reader may see, and an owner's only where the reader may read")
    public List<DrawnDocument> frames(@RequestParam(required = false) String owner) {
        return library.frames(WrittenOwners.optional(owner));
    }

    @GetMapping("/footprints")
    @PublicEndpoint("the catalogue's footprints the reader may see, and an owner's only where the reader may read")
    public List<DrawnDocument> footprints(@RequestParam(required = false) String owner) {
        return library.footprints(WrittenOwners.optional(owner));
    }

    @GetMapping("/footprints/{slug}")
    @PublicEndpoint("a footprint the reader may see, and only the components on it the reader may see")
    public FootprintDetail footprint(@PathVariable String slug) {
        return library.footprint(slug);
    }

    @GetMapping("/footprint-rules")
    @PublicEndpoint("the land pattern rules the library's footprints are generated with — the library's own table")
    public LandPatternRules footprintRules() {
        return library.footprintRules();
    }

    @GetMapping("/board-rules")
    @PublicEndpoint("the ways a printed circuit board can be made — the library's own table")
    public BoardRules boardRules() {
        return library.boardRules();
    }
}
