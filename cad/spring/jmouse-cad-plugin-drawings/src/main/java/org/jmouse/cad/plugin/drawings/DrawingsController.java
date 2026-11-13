package org.jmouse.cad.plugin.drawings;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.management.CadManagementRoutes;
import org.jmouse.cad.store.catalogue.WrittenOwners;
import org.jmouse.cad.store.exchange.DocumentExchange;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** One document to read and print, of a design's schematics, its pcb's sheets, or both, behind a cover, numbered together. */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX + "/plugins/" + DrawingsPlugin.ID)
public class DrawingsController {

    private final DrawingsExport   drawings;
    private final DocumentExchange exchange;
    private final CadSettings      sections;

    public DrawingsController(DrawingsExport drawings, DocumentExchange exchange, CadSettings sections) {
        this.drawings = drawings;
        this.exchange = exchange;
        this.sections = sections;
    }

    /**
     * @param name      what the file is called, without its ending
     * @param title     what the document is headed with
     * @param author    who it is by, for its cover; blank for not said
     * @param revision  the revision of a document with no pcb to say it
     * @param schematic the schematics, drawn; empty for none
     * @param pcb       the pcb's text; null for none
     * @param owner     whose it is: what the pcb names is found there
     */
    public record DocumentRequest(String name, String title, String author, String revision, List<SchematicPages.Drawn> schematic, String pcb, String owner) {
    }

    /** @param text the DXF file's text */
    public record OutlineRequest(String text) {
    }

    /** A board's edge read from a DXF: nothing stored, the path handed back for the editor to take as one step. */
    @PostMapping("/outline")
    @PublicEndpoint("what the caller sent, read and handed back; nothing stored is touched")
    public OutlineFromDxf.Answer outline(@RequestBody OutlineRequest request) {
        return OutlineFromDxf.of(request.text());
    }

    @PostMapping("/document")
    @PublicEndpoint("what the caller sent; components are read as the reader may see them")
    public ExportedFiles document(@RequestBody DocumentRequest request) {
        List<SchematicPages.Drawn> schematic = request.schematic() == null ? List.of() : request.schematic();
        List<SheetPage> pages = new ArrayList<>();
        schematic.forEach(drawn -> pages.add(drawings.schematicPage(drawn)));
        List<SheetFacts.Fact> facts = new ArrayList<>();
        if (!schematic.isEmpty()) {
            facts.add(new SheetFacts.Fact(sections.read(DrawingsPlugin.SHEETS).words().factSheets(), String.valueOf(schematic.size())));
        }
        String revision = request.revision() == null ? sections.read(DrawingsPlugin.SHEETS).words().unsaved() : request.revision();
        if (request.pcb() != null && !request.pcb().isBlank()) {
            DrawingsExport.PcbPart pcb = exchange.written(request.pcb(), WrittenOwners.optional(request.owner()), drawings::pcbPart);
            pages.addAll(pcb.pages());
            facts.addAll(pcb.facts());
            revision = pcb.revision();
        }
        if (pages.isEmpty()) {
            throw new IllegalArgumentException("A document holds a schematic or a pcb.");
        }
        LocalDate today = LocalDate.now();
        SheetFacts told = new SheetFacts(request.title(), request.author() == null ? "" : request.author(), revision, today.toString(), String.valueOf(today.getYear()), facts);
        return ExportedFiles.binary(request.name() + ".pdf", drawings.pdfOf(request.title(), told, pages), List.of());
    }
}
