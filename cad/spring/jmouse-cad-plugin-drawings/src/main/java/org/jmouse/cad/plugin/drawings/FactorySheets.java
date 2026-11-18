package org.jmouse.cad.plugin.drawings;

import org.jmouse.cad.pcb.PcbLayers;
import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Rules;
import org.jmouse.cad.pcb.drawing.Dimension;
import org.jmouse.cad.pcb.drawing.Lettering;
import org.jmouse.cad.pcb.drawing.Shape;
import org.jmouse.cad.exchange.pcb.ArtworkAreas;
import org.jmouse.cad.exchange.pcb.ArtworkFeature;
import org.jmouse.cad.exchange.pcb.BillOfMaterials;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbSolidLayers;
import org.jmouse.cad.exchange.pcb.PcbProfile;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.PcbDrills;
import org.jmouse.strokefont.StrokeText;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Everything a reader and a factory read of a pcb, from nothing but the pcb: the board whole, each copper layer with its tracks
 * and pours, where its parts stand on each side, the outline with its dimensions and every hole marked by its tool, and, on
 * sheets of their own so the drawings keep the paper, the tables of holes and stack with the notes, and the bill of materials.
 */
final class FactorySheets {

    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    /** How many decimals a length on a sheet keeps. */
    private static final int LENGTH_DECIMALS = 3;

    private static final int LETTERS = 26;

    /** The words on paper white, for a hole through what is drawn. */
    private static final String PAPER = "#ffffff";

    private final SheetSettings         settings;
    private final SheetSettings.Words   words;
    private final SheetSettings.Ink     ink;
    private final SheetLettering        lettering;
    private final PcbArtwork            drawn;
    private final PcbSolidLayers.Settings    layers;
    private final double                thickness;
    private final String                title;
    private final String                revision;
    private final ArtworkAreas          areas;
    private final PcbProfile            profile;
    private final BoardView.Bounds      bounds;
    private final UnaryOperator<String> footprintTitles;

    /**
     * @param thickness       the pcb's finished thickness, in millimetres
     * @param title           what the drawing is headed with
     * @param revision        the revision it is of
     * @param footprintTitles what a person calls a footprint, by its slug
     */
    FactorySheets(SheetSettings settings, SheetLettering lettering, PcbArtwork drawn, PcbSolidLayers.Settings layers, double thickness, String title, String revision,
                  UnaryOperator<String> footprintTitles) {
        this.settings = settings;
        this.words = settings.words();
        this.ink = settings.ink();
        this.lettering = lettering;
        this.drawn = drawn;
        this.layers = layers;
        this.thickness = thickness;
        this.title = title;
        this.revision = revision;
        this.footprintTitles = footprintTitles;
        this.areas = new ArtworkAreas(drawn.outlines(), drawn.settings().curveSteps());
        this.profile = PcbProfile.of(drawn);
        this.bounds = BoardView.Bounds.of(profile);
    }

    /** One tool's holes: all of one width and one length, of one kind of wall. */
    record HoleTool(String mark, boolean plated, double width, double slot, List<PcbDrills.Drill> drills) {
    }

    /** Every sheet, numbered and with its title block, as a document of its own. */
    List<Sheet> sheets() {
        return new SheetTitles(settings, title).titled(pages());
    }

    /** What the pcb is, for a document's cover. */
    List<SheetFacts.Fact> facts() {
        PcbDrawing pcb = drawn.fabrication().pcb();
        long parts = drawn.fabrication().parts().stream().filter(part -> !part.nothingToMount()).count();
        return List.of(new SheetFacts.Fact(words.factSize(), length(bounds.width()) + " × " + length(bounds.height()) + " mm"),
                new SheetFacts.Fact(words.factLayers(), String.valueOf(pcb.copperLayers())),
                new SheetFacts.Fact(words.factThickness(), length(thickness) + " mm"),
                new SheetFacts.Fact(words.factParts(), String.valueOf(parts)),
                new SheetFacts.Fact(words.factHoles(), String.valueOf(PcbDrills.of(pcb, drawn.fabrication().footprints()).size())));
    }

    /** Every page, for a document to number with others. */
    List<SheetPage> pages() {
        List<SheetPage> pages = new ArrayList<>();
        overview(pages);
        copper(pages);
        assembly(pages, PcbVocabulary.TOP, "assembly-top", words.assemblyTopSheet(), false);
        assembly(pages, PcbVocabulary.BOTTOM, "assembly-bottom", words.assemblyBottomSheet(), true);
        List<HoleTool> tools = toolsOf(PcbDrills.of(drawn.fabrication().pcb(), drawn.fabrication().footprints()));
        fabrication(pages, tools);
        tables(pages, tools);
        materials(pages);
        return pages;
    }

    /** The board whole: both faces' copper under the top's print, as it is held. */
    private void overview(List<SheetPage> pages) {
        Fitted fitted = fitted();
        SheetPage page = newPage(pages, "overview", words.overviewSheet(), fitted, SheetPurpose.DOCUMENT);
        BoardView view = viewOn(fitted, false);
        substrate(page, view);
        layerNames().reversed().forEach(layer -> drawInk(page, view, drawn.copper(layer), inkOf(layer)));
        drawInk(page, view, drawn.silkscreen(PcbVocabulary.TOP), ink.silkscreen());
        drills(page, view);
        outline(page, view);
    }

    /** Each copper layer on a sheet of its own: its tracks, its pours, its pads. */
    private void copper(List<SheetPage> pages) {
        Fitted fitted = fitted();
        List<String> names = layerNames();
        for (int index = 0; index < names.size(); index++) {
            String layer = names.get(index);
            SheetPage page = newPage(pages, "copper-" + (index + 1), words.copperSheet() + " " + (index + 1) + " · " + layer, fitted, SheetPurpose.DOCUMENT);
            BoardView view = viewOn(fitted, false);
            drawInk(page, view, drawn.copper(layer), inkOf(layer));
            drills(page, view);
            outline(page, view);
        }
    }

    private List<String> layerNames() {
        int count = drawn.fabrication().pcb().copperLayers();
        List<String> names = new ArrayList<>();
        names.add(PcbVocabulary.TOP);
        for (int inner = 1; inner < Math.max(count, 2) - 1; inner++) {
            names.add(PcbLayers.innerCopper(inner));
        }
        names.add(PcbVocabulary.BOTTOM);
        return names;
    }

    private String inkOf(String layer) {
        return PcbVocabulary.TOP.equals(layer) ? ink.copperTop() : PcbVocabulary.BOTTOM.equals(layer) ? ink.copperBottom() : ink.copperInner();
    }

    private void fabrication(List<SheetPage> pages, List<HoleTool> tools) {
        Fitted fitted = fitted();
        SheetPage page = newPage(pages, "fabrication", words.fabricationSheet(), fitted, SheetPurpose.DRAWING);
        BoardView view = viewOn(fitted, false);
        outline(page, view);
        tools.forEach(tool -> holes(page, view, tool));
        documentation(page, view);
        overallDimensions(page, view);
        for (Dimension dimension : drawn.fabrication().pcb().dimensions()) {
            ownDimension(page, view, dimension);
        }
    }

    private void assembly(List<SheetPage> pages, String side, String name, String sheetTitle, boolean mirrored) {
        boolean any = drawn.fabrication().parts().stream().anyMatch(part -> part.placement().side().equals(side) && !part.nothingToMount());
        if (!any) {
            return;
        }
        Fitted fitted = fitted();
        SheetPage page = newPage(pages, name, sheetTitle, fitted, SheetPurpose.DRAWING);
        BoardView view = viewOn(fitted, mirrored);
        outline(page, view);
        for (ArtworkFeature feature : drawn.assembly(side)) {
            feature(page, view, feature);
        }
    }

    /** What is told in tables and words, on sheets of their own. */
    private void tables(List<SheetPage> pages, List<HoleTool> tools) {
        SheetSettings.Paper paper = fitted().paper();
        SheetFlow flow = flowOf(pages, "tables", words.tablesSheet(), paper, columnWidth(paper, 2));
        if (!tools.isEmpty()) {
            flow.table(holeTable(tools));
        }
        flow.table(stackTable());
        flow.notes(words.notesHeading(), notes());
    }

    private void materials(List<SheetPage> pages) {
        SheetSettings.Paper paper = fitted().paper();
        SheetTable table = materialsTable();
        if (!table.rows().isEmpty()) {
            flowOf(pages, "materials", words.materialsSheet(), paper, columnWidth(paper, 1)).table(table);
        }
    }

    private SheetFlow flowOf(List<SheetPage> pages, String name, String sheetTitle, SheetSettings.Paper paper, double width) {
        int[] continued = {0};
        return new SheetFlow(settings, width, () -> {
            continued[0]++;
            SheetPage page = new SheetPage(continued[0] == 1 ? name : name + "-" + continued[0], sheetTitle, settings, paper, "", lettering, revision, SheetPurpose.DOCUMENT);
            pages.add(page);
            return page;
        });
    }

    private double columnWidth(SheetSettings.Paper paper, int columns) {
        double usable = paper.width() - 2 * settings.margin() - 2 * settings.columnGap();
        return (usable - (columns - 1) * settings.columnGap()) / columns;
    }

    private SheetPage newPage(List<SheetPage> pages, String name, String sheetTitle, Fitted fitted, SheetPurpose purpose) {
        SheetPage page = new SheetPage(name, sheetTitle, settings, fitted.paper(), fitted.scale().label() + "  " + words.units(), lettering, revision, purpose);
        pages.add(page);
        return page;
    }

    /** A paper and a scale for the view: the smallest paper the pcb fits on at full size, at the largest scale it fits there. */
    private record Fitted(SheetSettings.Paper paper, SheetSettings.Scale scale) {
    }

    private double roomAcross(SheetSettings.Paper paper) {
        return paper.width() - 2 * settings.margin() - 2 * settings.viewMargin();
    }

    private double roomUp(SheetSettings.Paper paper) {
        return paper.height() - 2 * settings.margin() - settings.titleHeight() - 2 * settings.viewMargin();
    }

    private Fitted fitted() {
        Fitted best = null;
        for (SheetSettings.Paper paper : settings.papers()) {
            for (SheetSettings.Scale scale : settings.scales()) {
                if (bounds.width() * scale.factor() <= roomAcross(paper) && bounds.height() * scale.factor() <= roomUp(paper)) {
                    best = new Fitted(paper, scale);
                    if (scale.factor() >= 1) {
                        return best;
                    }
                    break;
                }
            }
        }
        if (best == null) {
            throw new IllegalArgumentException("A pcb of " + length(bounds.width()) + " by " + length(bounds.height())
                                               + " mm fits on no paper of the drawing's at any of its scales.");
        }
        return best;
    }

    /** The view in the middle of what the paper has above its title block. */
    private BoardView viewOn(Fitted fitted, boolean mirrored) {
        double factor = fitted.scale().factor();
        SheetSettings.Paper paper = fitted.paper();
        double left = (paper.width() - bounds.width() * factor) / 2;
        double floor = settings.margin() + settings.titleHeight() + settings.viewMargin();
        double bottom = floor + (roomUp(paper) - bounds.height() * factor) / 2;
        return new BoardView(bounds, factor, left, bottom, mirrored);
    }

    private void outline(SheetPage page, BoardView view) {
        double pen = settings.penOf(SheetLayer.OUTLINE).pen();
        page.add(new Sheet.Mark.Line(SheetLayer.OUTLINE, view.at(profile.outline()), true, pen));
        for (List<Point> cutout : profile.cutouts()) {
            page.add(new Sheet.Mark.Line(SheetLayer.OUTLINE, view.at(cutout), true, pen));
        }
    }

    /** The material of the board, under what is drawn on it. */
    private void substrate(SheetPage page, BoardView view) {
        List<List<Sheet.Point>> rings = new ArrayList<>();
        rings.add(view.at(profile.outline()));
        profile.cutouts().forEach(cutout -> rings.add(view.at(cutout)));
        page.add(new Sheet.Mark.Inked(new Sheet.Mark.Area(SheetLayer.ARTWORK, rings), ink.substrate()));
    }

    /** Every drill through what is drawn: paper showing, as light does through the board. */
    private void drills(SheetPage page, BoardView view) {
        for (PcbDrills.Drill drill : PcbDrills.of(drawn.fabrication().pcb(), drawn.fabrication().footprints())) {
            page.add(new Sheet.Mark.Inked(new Sheet.Mark.Area(SheetLayer.HOLES, List.of(drillOutline(view, drill))), PAPER));
        }
    }

    private void holes(SheetPage page, BoardView view, HoleTool tool) {
        double pen = settings.penOf(SheetLayer.HOLES).pen();
        for (PcbDrills.Drill drill : tool.drills()) {
            if (drill.round()) {
                page.add(new Sheet.Mark.Circle(SheetLayer.HOLES, view.at(drill.from()), drill.width() * view.factor() / 2, pen));
            } else {
                page.add(new Sheet.Mark.Line(SheetLayer.HOLES, drillOutline(view, drill), true, pen));
            }
            double middleX = (drill.from().x() + drill.to().x()) / 2;
            double middleY = (drill.from().y() + drill.to().y()) / 2;
            Sheet.Point middle = view.at(middleX, middleY);
            page.words(SheetLayer.HOLES, tool.mark(), middle.x(), middle.y(), settings.text().holeMark(), StrokeText.Anchor.MIDDLE, StrokeText.Baseline.MIDDLE);
        }
    }

    private List<Sheet.Point> drillOutline(BoardView view, PcbDrills.Drill drill) {
        Geometry line = drill.round() ? GEOMETRY.createPoint(new Coordinate(drill.from().x(), drill.from().y()))
                : GEOMETRY.createLineString(new Coordinate[]{new Coordinate(drill.from().x(), drill.from().y()), new Coordinate(drill.to().x(), drill.to().y())});
        Geometry outline = line.buffer(drill.width() / 2, settings.curveSteps());
        List<Sheet.Point> points = new ArrayList<>();
        for (Coordinate corner : outline.getCoordinates()) {
            points.add(view.at(corner.x, corner.y));
        }
        return points;
    }

    /** What the pcb itself draws for the factory's eyes: on a panel, its score lines. */
    private void documentation(SheetPage page, BoardView view) {
        for (Shape shape : drawn.fabrication().pcb().shapes()) {
            if (!PcbVocabulary.SHEET_LAYERS.contains(shape.layer())) {
                continue;
            }
            for (List<double[]> run : drawn.runs(shape.path())) {
                page.add(new Sheet.Mark.Line(SheetLayer.DIMENSIONS, run.stream().map(point -> view.at(point[0], point[1])).toList(), false,
                        settings.penOf(SheetLayer.DIMENSIONS).pen()));
            }
        }
    }

    private void overallDimensions(SheetPage page, BoardView view) {
        double offset = settings.viewMargin() * 0.5;
        page.dimension(new Sheet.Point(view.left(), view.bottom()), new Sheet.Point(view.right(), view.bottom()), -offset, length(bounds.width()));
        page.dimension(new Sheet.Point(view.left(), view.bottom()), new Sheet.Point(view.left(), view.top()), offset, length(bounds.height()));
    }

    private void ownDimension(SheetPage page, BoardView view, Dimension dimension) {
        double measured = Math.hypot(dimension.to().x() - dimension.from().x(), dimension.to().y() - dimension.from().y());
        page.dimension(view.at(dimension.from()), view.at(dimension.to()), dimension.offset() * view.factor(), length(measured));
    }

    /** Features in a colour of their own, at their true widths: what a copper layer is. */
    private void drawInk(SheetPage page, BoardView view, List<ArtworkFeature> features, String colour) {
        for (ArtworkFeature feature : features) {
            markOf(view, feature, true).forEach(mark -> page.add(new Sheet.Mark.Inked(mark, colour)));
        }
    }

    private void feature(SheetPage page, BoardView view, ArtworkFeature feature) {
        markOf(view, feature, false).forEach(page::add);
    }

    private List<Sheet.Mark> markOf(BoardView view, ArtworkFeature feature, boolean solid) {
        double pen = settings.penOf(SheetLayer.ARTWORK).pen();
        return switch (feature) {
            case ArtworkFeature.Stroke stroke -> runOf(view, stroke.points(), stroke.width());
            case ArtworkFeature.Arc arc -> runOf(view, areas.arcPoints(arc), arc.width());
            case ArtworkFeature.Pad pad -> {
                List<Sheet.Point> outline = view.at(drawn.outlines().placed(pad.placement(), pad.pad(), pad.growth()));
                yield List.of(solid ? new Sheet.Mark.Area(SheetLayer.ARTWORK, List.of(outline)) : new Sheet.Mark.Line(SheetLayer.ARTWORK, outline, true, pen));
            }
            case ArtworkFeature.Disc disc -> {
                double radius = disc.diameter() / 2;
                Geometry round = GEOMETRY.createPoint(new Coordinate(disc.centre().x(), disc.centre().y())).buffer(radius, settings.curveSteps());
                List<Sheet.Point> outline = new ArrayList<>();
                for (Coordinate corner : round.getCoordinates()) {
                    outline.add(view.at(corner.x, corner.y));
                }
                yield List.of(solid ? new Sheet.Mark.Area(SheetLayer.ARTWORK, List.of(outline)) : new Sheet.Mark.Circle(SheetLayer.ARTWORK, view.at(disc.centre()), radius * view.factor(), pen));
            }
            case ArtworkFeature.Region region -> {
                List<List<Sheet.Point>> rings = new ArrayList<>();
                rings.add(view.at(region.outline()));
                region.holes().forEach(hole -> rings.add(view.at(hole)));
                yield List.of(new Sheet.Mark.Area(SheetLayer.ARTWORK, rings));
            }
        };
    }

    private List<Sheet.Mark> runOf(BoardView view, List<Point> points, double width) {
        if (points.size() < 2) {
            return List.of();
        }
        return List.of(new Sheet.Mark.Line(SheetLayer.ARTWORK, view.at(points), false, Math.max(width * view.factor(), settings.penOf(SheetLayer.ARTWORK).pen())));
    }

    /** The holes by tool: plated ones first, each kind from the smallest up, marked A, B, … AA in that order. */
    static List<HoleTool> toolsOf(List<PcbDrills.Drill> drills) {
        record Tool(boolean plated, double width, double slot) {
        }
        Map<Tool, List<PcbDrills.Drill>> grouped = new LinkedHashMap<>();
        drills.stream().sorted(Comparator.comparing((PcbDrills.Drill drill) -> !drill.plated()).thenComparingDouble(PcbDrills.Drill::width)
                        .thenComparingDouble(FactorySheets::slotLength))
                .forEach(drill -> grouped.computeIfAbsent(new Tool(drill.plated(), drill.width(), slotLength(drill)),
                        ignored -> new ArrayList<>()).add(drill));
        List<HoleTool> tools = new ArrayList<>();
        grouped.forEach((tool, held) -> tools.add(new HoleTool(markOf(tools.size()), tool.plated(), tool.width(), tool.slot(), held)));
        return tools;
    }

    private static double slotLength(PcbDrills.Drill drill) {
        return drill.round() ? 0 : Math.hypot(drill.to().x() - drill.from().x(), drill.to().y() - drill.from().y()) + drill.width();
    }

    private static String markOf(int index) {
        String last = String.valueOf((char) ('A' + index % LETTERS));
        return index < LETTERS ? last : markOf(index / LETTERS - 1) + last;
    }

    private SheetTable holeTable(List<HoleTool> tools) {
        List<List<String>> rows = new ArrayList<>();
        for (HoleTool tool : tools) {
            rows.add(List.of(tool.mark(), length(tool.width()), tool.slot() > 0 ? length(tool.slot()) : "", tool.plated() ? words.plated() : words.unplated(),
                    String.valueOf(tool.drills().size())));
        }
        return new SheetTable(words.holesHeading(), List.of(new SheetTable.Column(words.holeMark(), 1, false),
                new SheetTable.Column(words.holeSize(), 1.2, true), new SheetTable.Column(words.holeSlot(), 1.2, true),
                new SheetTable.Column(words.holePlating(), 1.2, false), new SheetTable.Column(words.holeCount(), 1, true)), rows);
    }

    private SheetTable stackTable() {
        int copperLayers = drawn.fabrication().pcb().copperLayers();
        double copper = layers.copper().thickness();
        double mask = layers.mask().thickness();
        double silkscreen = layers.silkscreen().thickness();
        double core = thickness - copperLayers * copper - 2 * mask;
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of(words.stackSilkscreen(), length(silkscreen)));
        rows.add(List.of(words.stackMask(), length(mask)));
        for (int layer = 1; layer <= copperLayers; layer++) {
            rows.add(List.of(words.stackCopper() + " " + layer, length(copper)));
            if (layer < copperLayers) {
                rows.add(List.of(words.stackCore(), length(core / (copperLayers - 1))));
            }
        }
        rows.add(List.of(words.stackMask(), length(mask)));
        rows.add(List.of(words.stackSilkscreen(), length(silkscreen)));
        rows.add(List.of(words.stackTotal(), length(thickness)));
        return new SheetTable(words.stackHeading(), List.of(new SheetTable.Column(words.stackLayer(), 3, false),
                new SheetTable.Column(words.stackThickness(), 2, true)), rows);
    }

    private List<String> notes() {
        PcbDrawing pcb = drawn.fabrication().pcb();
        String rules = pcb.rules().map(Rules::profile).orElse("standard");
        List<String> notes = new ArrayList<>();
        for (String note : settings.fabricationNotes()) {
            notes.add(note.replace("{thickness}", length(thickness)).replace("{copperLayers}", String.valueOf(pcb.copperLayers()))
                    .replace("{rules}", rules));
        }
        // What the pcb itself writes on its documentation layer is said to the factory beside the general notes.
        pcb.texts().stream().filter(text -> PcbVocabulary.SHEET_LAYERS.contains(text.layer())).map(Lettering::text)
                .filter(text -> !text.isBlank()).forEach(notes::add);
        return notes;
    }

    /** What is to be bought and placed: a line for each kind of part. */
    private SheetTable materialsTable() {
        List<List<String>> rows = new ArrayList<>();
        int item = 1;
        for (BillOfMaterials.Line line : BillOfMaterials.of(drawn.fabrication()).lines()) {
            rows.add(List.of(String.valueOf(item), String.join(" ", line.designators()), line.value(), line.footprint().map(footprintTitles).orElse(""),
                    String.valueOf(line.quantity())));
            item++;
        }
        return new SheetTable(words.partsHeading(), List.of(new SheetTable.Column(words.partsItem(), 0.5, true), new SheetTable.Column(words.partsDesignators(), 4, false),
                new SheetTable.Column(words.partsValue(), 2.4, false), new SheetTable.Column(words.partsFootprint(), 3, false),
                new SheetTable.Column(words.partsCount(), 0.7, true)), rows);
    }

    /** A length as a sheet says it: three decimals at most, no trailing zeros. */
    static String length(double millimetres) {
        BigDecimal rounded = BigDecimal.valueOf(millimetres).setScale(LENGTH_DECIMALS, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.scale() < 0 ? rounded.setScale(0, RoundingMode.UNNECESSARY).toPlainString() : rounded.toPlainString();
    }
}
