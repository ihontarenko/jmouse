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
import org.jmouse.cad.exchange.pcb.PcbLayers;
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

    private final SheetSettings       settings;
    private final PcbArtwork.Settings artwork;
    private final PcbLayers.Settings  layers;
    private final StrokeFont          strokes;
    private final SheetFonts          fonts;

    DrawingsExport(SheetSettings settings, PcbArtwork.Settings artwork, PcbLayers.Settings layers, StrokeFont strokes, SheetFonts fonts) {
        this.settings = settings;
        this.artwork = artwork;
        this.layers = layers;
        this.strokes = strokes;
        this.fonts = fonts;
    }

    /** The settings shipped in the jar, as data. */
    static SheetSettings shippedSettings() {
        return CadSettings.read(DrawingsPlugin.SHEETS);
    }

    /** The export with the settings shipped in its jar and the engine's. */
    static DrawingsExport shipped(SheetSettings settings, SheetFonts fonts) {
        return new DrawingsExport(settings, CadSettings.read(PcbArtwork.SECTION),
                CadSettings.read(PcbLayers.SECTION), StrokeFont.newStroke(), fonts);
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
        List<Sheet> drawings = new SheetTitles(settings, document.title()).titled(designed.sheets().pages().stream().filter(page -> page.purpose() == SheetPurpose.DRAWING).toList());
        DxfSheets dxf = new DxfSheets(settings);
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
        Map<String, Double> rules = CadStandards.pcbRules().valuesOf(fabrication.pcb().rules());
        Double thickness = rules.get(THICKNESS);
        if (thickness == null) {
            throw new IllegalStateException("The pcb's rules hold no " + THICKNESS + ", which a drawing's stack needs.");
        }
        PcbArtwork drawn = PcbArtwork.of(fabrication, rules, artwork, strokes);
        String revision = catalogue.revisionOf(document).orElse(settings.words().unsaved());
        return new Designed(new FactorySheets(settings, lettering(), drawn, layers, thickness, document.title(), revision,
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
        return new SchematicPages(settings, lettering()).page(drawn);
    }

    /** Pages as one PDF, behind a cover and contents, numbered through. */
    byte[] pdfOf(String title, SheetFacts facts, List<SheetPage> body) {
        DocumentFront front = new DocumentFront(settings, lettering());
        List<SheetPage> all = new ArrayList<>();
        all.add(front.cover(facts));
        all.addAll(front.contents(body, facts.revision()));
        all.addAll(body);
        return new PdfSheets(settings, strokes, fonts.faces()).write(title, PRODUCER, new SheetTitles(settings, title).titled(all));
    }

    /** The lettering a document is set in: its layout measures words by the font they will be drawn in. */
    private SheetLettering lettering() {
        return fonts.faces().<SheetLettering>map(found -> (text, height) -> found.regular().widthOf(text, found.regular().sizeForCapHeight(height)))
                .orElseGet(() -> SheetLettering.strokes(strokes, settings.curveSteps()));
    }
}
