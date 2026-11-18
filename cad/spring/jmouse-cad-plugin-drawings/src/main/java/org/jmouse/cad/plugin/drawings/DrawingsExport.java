package org.jmouse.cad.plugin.drawings;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ExportPurpose;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.ExportedFile;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.exchange.pcb.PcbSolidLayers;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.strokefont.StrokeFont;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A pcb's documents, written from the pcb alone: one PDF of all its sheets, and a DXF of each that a factory draws from. */
public final class DrawingsExport implements DocumentExport {

    /** The format's word on the exchange routes. */
    static final String FORMAT = "drawings";

    private static final String PRODUCER = "jmCAD";

    /** The rule a pcb's thickness is in. */
    private static final String THICKNESS = "pcb-thickness";

    /** The sheets, the artwork and the layers, as the installation has them when a drawing is made. */
    private final CadSettings sections;
    private final StrokeFont  strokes;
    private final SheetFonts  fonts;

    /** The ways a pcb is made, read through the engine's tables. */
    private final CadStandards standards;

    DrawingsExport(CadSettings sections, StrokeFont strokes, SheetFonts fonts, CadStandards standards) {
        this.standards = standards;
        this.sections = sections;
        this.strokes = strokes;
        this.fonts = fonts;
    }

    /** How a sheet is drawn and worded. */
    private SheetSettings sheets() {
        return sections.read(DrawingsPlugin.SHEETS);
    }

    @Override
    public String id() {
        return FORMAT;
    }

    @Override
    public String title() {
        return "Factory drawing · PDF and DXF";
    }

    /** Drawings a person reads. */
    @Override
    public ExportPurpose purpose() {
        return ExportPurpose.DOCUMENTATION;
    }

    @Override
    public Set<DocumentKind> kinds() {
        return Set.of(PcbModule.PCB);
    }

    @Override
    public ExportedFiles write(DocumentNode document, CatalogueDocuments catalogue) {
        Designed designed = designed(document, catalogue);
        String name = document.slug();
        List<ExportedFile> files = new ArrayList<>();
        LocalDate today = LocalDate.now();
        SheetFacts facts = new SheetFacts(document.title(), "", designed.revision(), today.toString(), String.valueOf(today.getYear()), designed.sheets().facts());
        files.add(ExportedFile.binary(name + "-drawing.pdf", pdfOf(document.title(), facts, designed.sheets().pages())));
        // A DXF is what a factory draws from: the drawings alone, each numbered as a document of its own.
        List<Sheet> drawings = new SheetTitles(sheets(), document.title()).titled(designed.sheets().pages().stream().filter(page -> page.purpose() == SheetPurpose.DRAWING).toList());
        DxfSheets dxf = new DxfSheets(sheets());
        for (Sheet sheet : drawings) {
            files.add(new ExportedFile(name + "-" + sheet.name() + ".dxf", dxf.write(sheet)));
        }
        return new ExportedFiles(name + "-" + FORMAT, files, PcbFabrication.of(document, catalogue).notes());
    }

    /** A pcb's sheets, and the revision they are of. */
    private record Designed(FactorySheets sheets, String revision) {
    }

    private Designed designed(DocumentNode document, CatalogueDocuments catalogue) {
        PcbFabrication fabrication = PcbFabrication.of(document, catalogue);
        Map<String, Double> rules = standards.pcbRules().valuesOf(fabrication.pcb().rules());
        Double thickness = rules.get(THICKNESS);
        if (thickness == null) {
            throw new IllegalStateException("The pcb's rules hold no " + THICKNESS + ", which a drawing's stack needs.");
        }
        PcbArtwork drawn = PcbArtwork.of(fabrication, rules, sections.read(PcbArtwork.SECTION), strokes);
        String revision = catalogue.revisionOf(document).orElse(sheets().words().unsaved());
        return new Designed(new FactorySheets(sheets(), lettering(), drawn, sections.read(PcbSolidLayers.SECTION), thickness, document.title(), revision,
                slug -> catalogue.find(slug).map(DocumentNode::title).orElse(slug)), revision);
    }

    /** A pcb's sheets and what is told of it, for a document holding others beside them. */
    record PcbPart(List<SheetPage> pages, List<SheetFacts.Fact> facts, String revision) {
    }

    PcbPart pcbPart(DocumentNode document, CatalogueDocuments catalogue) {
        Designed designed = designed(document, catalogue);
        return new PcbPart(designed.sheets().pages(), designed.sheets().facts(), designed.revision());
    }

    /** A schematic's drawing as a page. */
    SheetPage schematicPage(SchematicPages.Drawn drawn) {
        return new SchematicPages(sheets(), lettering()).page(drawn);
    }

    /** Pages as one PDF, behind a cover and contents, numbered through. */
    byte[] pdfOf(String title, SheetFacts facts, List<SheetPage> body) {
        DocumentFront front = new DocumentFront(sheets(), lettering());
        List<SheetPage> all = new ArrayList<>();
        all.add(front.cover(facts));
        all.addAll(front.contents(body, facts.revision()));
        all.addAll(body);
        return new PdfSheets(sheets(), strokes, fonts.faces()).write(title, PRODUCER, new SheetTitles(sheets(), title).titled(all));
    }

    /** The lettering a document is set in: its layout measures words by the font they will be drawn in. */
    private SheetLettering lettering() {
        return fonts.faces().<SheetLettering>map(found -> (text, height) -> found.regular().widthOf(text, found.regular().sizeForCapHeight(height)))
                .orElseGet(() -> SheetLettering.strokes(strokes, sheets().curveSteps()));
    }
}
