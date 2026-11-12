package org.jmouse.cad.management;

import org.jmouse.cad.store.catalogue.WrittenOwners;
import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.packages.pcb.PcbRules;
import org.jmouse.cad.packages.footprint.LandPatternRules;
import org.jmouse.cad.store.library.PartLibrary;
import org.jmouse.cad.store.library.PartLibrary.ComponentReference;
import org.jmouse.cad.store.library.PartLibrary.ComponentView;
import org.jmouse.cad.store.library.PartLibrary.DrawnDocument;
import org.jmouse.cad.store.library.PartLibrary.FootprintDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.jmouse.cad.store.library.PartLibrary.ForeignParts;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 🧰 The library a sheet and a pcb are built from — {@code JMOUSE_CAD_API.md} §2.3: components, symbols, portraits,
 * frames, footprints and the standards they are checked against.
 *
 * <p>Every route decides inside its service: a list holds the catalogue's documents the reader may see, and those of
 * the owner's place — a project's own and its workspace's — only when {@code owner} names one the reader may read
 * ({@code TYPE:id}).</p>
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

    /** The documents of a file read in place, as jM. */
    public record ForeignDocumentsRequest(List<String> documents) {
    }

    /**
     * The components of a file read in place — a KiCad design's —, compiled so its circuits draw: what they name is found
     * among the file's own documents first, then in the catalogue as the reader sees it. Nothing is stored.
     */
    @PostMapping("/components/foreign")
    @PublicEndpoint("compiles texts the reader sends; what they name from the catalogue is only what the reader may see")
    public ForeignParts foreignParts(@RequestBody ForeignDocumentsRequest request) {
        return library.foreignParts(request.documents() == null ? List.of() : request.documents());
    }

    /**
     * A shelf of the library: the documents of one kind — a symbol, a footprint, a model a plugin brings —, the catalogue's
     * the reader may see and an owner's where the reader may read, each with its model and where it is held.
     */
    @GetMapping("/shelves/{kind}")
    @PublicEndpoint("the catalogue's documents the reader may see, and an owner's only where the reader may read")
    public List<DrawnDocument> shelf(@PathVariable String kind, @RequestParam(required = false) String owner) {
        return library.shelf(kind, WrittenOwners.optional(owner));
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

    @GetMapping("/pcb-rules")
    @PublicEndpoint("the ways a printed circuit board can be made — the library's own table")
    public PcbRules pcbRules() {
        return library.pcbRules();
    }
}
