package org.jmouse.cad.plugin.drawings;

import org.jmouse.settings.Bounds;
import org.jmouse.settings.Colour;
import org.jmouse.settings.Label;
import java.util.List;
import java.util.Map;

/**
 * How a drawing's sheets are made, as data (the settings tree's {@code plugin/drawings/sheets}): their papers and scales, their measures in millimetres on paper,
 * their pens and every word they print.
 *
 * @param papers           from the smallest up: the first a pcb fits on at full size or larger is taken
 * @param scales           from the largest down
 * @param margin           from the paper's edge to the frame
 * @param columnWidth      how wide a column of tables is
 * @param columnGap        between columns, and between a column and the view
 * @param titleHeight      how high the title block is; it is as wide as a column
 * @param viewMargin       kept free round a view of the pcb: its overall dimensions stand there
 * @param rowHeight        how high a table's row is, for one line
 * @param dimensionArrow   how long a dimension's arrowhead is
 * @param curveSteps       how many straight pieces a letter's arc is drawn with
 * @param circleSteps      how many straight pieces a circle is drawn with where a format has no circles
 * @param schematicPaper   the paper a schematic is drawn on
 * @param fontFamilies     the families a document is set in, the one wanted first: the first installed is taken
 * @param fontFolders      where fonts are usually installed, on any system: those that exist are looked through by default
 * @param text             how high a capital is, by what is printed
 * @param ink              the colours a pcb is printed in
 * @param cover            the document's front
 * @param layers           what each layer of a sheet is drawn with
 * @param words            every word a sheet prints that does not come from the pcb
 * @param fabricationNotes the general notes; {@code {thickness}}, {@code {copperLayers}} and {@code {rules}} are filled in
 */
public record SheetSettings(@Label("Papers") List<Paper> papers,
                            @Label("Scales") List<Scale> scales,
                            @Label("Margin") double margin,
                            @Label("Column width") double columnWidth,
                            @Label("Column gap") double columnGap,
                            @Label("Title height") double titleHeight,
                            @Label("View margin") double viewMargin,
                            @Label("Row height") double rowHeight,
                            @Label("Dimension arrow") double dimensionArrow,
                            @Label("Curve steps") int curveSteps,
                            @Label("Circle steps") int circleSteps,
                            @Label("Schematic paper") String schematicPaper,
                            @Label("Font families") List<String> fontFamilies,
                            @Label("Font folders") List<String> fontFolders,
                            @Label("Text") Text text,
                            @Label("Ink") Ink ink,
                            @Label("Cover") Cover cover,
                            @Label("Layers") Map<SheetLayer, Pen> layers,
                            @Label("Words") Words words,
                            @Label("Fabrication notes") List<String> fabricationNotes) {


    /**
     * A sheet's paper, lying on its long side.
     *
     * @param name   what the paper is called: A4
     * @param width  its long side
     * @param height its short side
     */
    public record Paper(@Label("Name") String name,
                        @Label(value = "Width", unit = "mm") double width,
                        @Label(value = "Height", unit = "mm") double height) {
    }

    /**
     * How large a pcb is drawn: {@code factor} millimetres of paper to one of pcb.
     *
     * @param label  how the scale is printed: 10:1
     * @param factor millimetres of paper to one of pcb
     */
    public record Scale(@Label("Label") String label,
                        @Label("Factor") double factor) {
    }

    /**
     * Heights of a capital, by what is printed.
     *
     * @param title          a sheet's title on paper
     * @param heading        a table's heading
     * @param body           a table's text
     * @param label          a label on a view
     * @param holeMark       a hole's mark on the drill drawing
     * @param schematicLeast the least a schematic's text is printed at, however small its scale
     * @param stroke         how wide a letter's stroke is, as a share of its height
     */
    public record Text(@Label(value = "Title", unit = "mm") double title,
                       @Label(value = "Heading", unit = "mm") double heading,
                       @Label(value = "Body", unit = "mm") double body,
                       @Label(value = "Label", unit = "mm") double label,
                       @Label(value = "Hole mark", unit = "mm") double holeMark,
                       @Label(value = "Schematic least", unit = "mm") double schematicLeast,
                       @Bounds(minimum = 0, maximum = 1, step = 0.01) @Label("Stroke") double stroke) {
    }

    /**
     * What a layer of a sheet is drawn with.
     *
     * @param pen    how wide its lines are on paper
     * @param colour its colour in a DXF: AutoCAD's index
     * @param hex    its colour in a PDF
     */
    public record Pen(@Label(value = "Pen", unit = "mm") double pen,
                      @Label("Colour") int colour,
                      @Colour @Label("Hex") String hex) {
    }

    /**
     * The colours a pcb is printed in.
     *
     * @param copperTop    the top copper's colour, #rrggbb
     * @param copperBottom the bottom copper's colour, #rrggbb
     * @param copperInner  an inner copper's colour, #rrggbb
     * @param silkscreen   the silkscreen's colour, #rrggbb
     * @param substrate    the pcb's own colour, #rrggbb
     */
    public record Ink(@Colour @Label("Copper top") String copperTop,
                      @Colour @Label("Copper bottom") String copperBottom,
                      @Colour @Label("Copper inner") String copperInner,
                      @Colour @Label("Silkscreen") String silkscreen,
                      @Colour @Label("Substrate") String substrate) {
    }

    /**
     * A document's front.
     *
     * @param product       the product the documents are made by
     * @param subtitle      what the document is
     * @param markSize      how large the mark is printed
     * @param productHeight how high the product's name is printed
     * @param titleHeight   how high the design's name is printed
     * @param titleAtShare  how far up the paper the design's name stands
     * @param factColumns   how many facts of the design stand in a row
     * @param markBox       the size of the box the mark's paths are drawn in
     * @param markPrimary   the mark's first path, as SVG path data in the box
     * @param markAccent    the mark's second path, as SVG path data in the box
     * @param markDot       the dot at the mark's middle
     * @param primary       the front's main colour, #rrggbb
     * @param accent        its second colour, #rrggbb
     * @param muted         the colour of what is said quietly, #rrggbb
     */
    public record Cover(@Label("Product") String product,
                        @Label("Subtitle") String subtitle,
                        @Label(value = "Mark size", unit = "mm") double markSize,
                        @Label(value = "Product height", unit = "mm") double productHeight,
                        @Label(value = "Title height", unit = "mm") double titleHeight,
                        @Bounds(minimum = 0, maximum = 1, step = 0.01) @Label("Title at share") double titleAtShare,
                        @Label("Fact columns") int factColumns,
                        @Label("Mark box") double markBox,
                        @Label("Mark primary") String markPrimary,
                        @Label("Mark accent") String markAccent,
                        @Label("Mark dot") Dot markDot,
                        @Colour @Label("Primary") String primary,
                        @Colour @Label("Accent") String accent,
                        @Colour @Label("Muted") String muted) {

        /**
         * The dot of the cover's mark: where it stands in the mark's box, and how big it is.
         *
         * @param x      its centre across the mark's box
         * @param y      its centre down the box
         * @param radius its radius, in the box's units
         */
        public record Dot(@Label("X") double x,
                          @Label("Y") double y,
                          @Label("Radius") double radius) {
        }
    }

    /**
     * Every word a sheet prints that does not come from the pcb.
     *
     * @param fabricationSheet    the fabrication drawing's title
     * @param assemblyTopSheet    the top placement drawing's title
     * @param assemblyBottomSheet the bottom placement drawing's title
     * @param holesHeading        the holes table's heading
     * @param holeMark            the holes table's mark column
     * @param holeSize            its size column
     * @param holeSlot            its slot column
     * @param holePlating         its walls column
     * @param holeCount           its count column
     * @param plated              how a plated hole's walls are said
     * @param unplated            how a bare hole's walls are said
     * @param stackHeading        the stack table's heading
     * @param stackLayer          its layer column
     * @param stackThickness      its thickness column
     * @param stackTotal          its total row
     * @param stackSilkscreen     its silkscreen row
     * @param stackMask           its solder mask row
     * @param stackCopper         its copper row
     * @param stackCore           its core and prepreg row
     * @param partsHeading        the parts table's heading
     * @param partsDesignators    its designators column
     * @param partsValue          its value column
     * @param partsFootprint      its footprint column
     * @param partsCount          its count column
     * @param notesHeading        the notes' heading
     * @param titleDrawing        the title block's drawing field
     * @param titleSheet          its sheet field
     * @param titleOf             the word between a sheet's number and their count
     * @param titleRevision       its revision field
     * @param titleScale          its scale field
     * @param titleUnits          its units field
     * @param titlePaper          its paper field
     * @param units               how the units are said
     * @param unsaved             what a revision of a design never saved is called
     * @param schematicScale      a schematic's scale, drawn to fit its paper
     * @param schematicSheet      a schematic sheet's title
     * @param overviewSheet       the whole pcb's sheet title
     * @param copperSheet         a copper layer's sheet title
     * @param tablesSheet         the sheet of tables' title
     * @param materialsSheet      the bill of materials' sheet title
     * @param contentsSheet       the contents' sheet title
     * @param partsItem           the parts table's item column
     * @param factAuthor          the cover's author fact
     * @param factDate            its date fact
     * @param factSize            its pcb size fact
     * @param factLayers          its copper layers fact
     * @param factThickness       its thickness fact
     * @param factParts           its parts fact
     * @param factHoles           its holes fact
     * @param factSheets          its schematic sheets fact
     */
    public record Words(@Label("Fabrication sheet") String fabricationSheet,
                        @Label("Assembly top sheet") String assemblyTopSheet,
                        @Label("Assembly bottom sheet") String assemblyBottomSheet,
                        @Label("Holes heading") String holesHeading,
                        @Label("Hole mark") String holeMark,
                        @Label("Hole size") String holeSize,
                        @Label("Hole slot") String holeSlot,
                        @Label("Hole plating") String holePlating,
                        @Label("Hole count") String holeCount,
                        @Label("Plated") String plated,
                        @Label("Unplated") String unplated,
                        @Label("Stack heading") String stackHeading,
                        @Label("Stack layer") String stackLayer,
                        @Label("Stack thickness") String stackThickness,
                        @Label("Stack total") String stackTotal,
                        @Label("Stack silkscreen") String stackSilkscreen,
                        @Label("Stack mask") String stackMask,
                        @Label("Stack copper") String stackCopper,
                        @Label("Stack core") String stackCore,
                        @Label("Parts heading") String partsHeading,
                        @Label("Parts designators") String partsDesignators,
                        @Label("Parts value") String partsValue,
                        @Label("Parts footprint") String partsFootprint,
                        @Label("Parts count") String partsCount,
                        @Label("Notes heading") String notesHeading,
                        @Label("Title drawing") String titleDrawing,
                        @Label("Title sheet") String titleSheet,
                        @Label("Title of") String titleOf,
                        @Label("Title revision") String titleRevision,
                        @Label("Title scale") String titleScale,
                        @Label("Title units") String titleUnits,
                        @Label("Title paper") String titlePaper,
                        @Label("Units") String units,
                        @Label("Unsaved") String unsaved,
                        @Label("Schematic scale") String schematicScale,
                        @Label("Schematic sheet") String schematicSheet,
                        @Label("Overview sheet") String overviewSheet,
                        @Label("Copper sheet") String copperSheet,
                        @Label("Tables sheet") String tablesSheet,
                        @Label("Materials sheet") String materialsSheet,
                        @Label("Contents sheet") String contentsSheet,
                        @Label("Parts item") String partsItem,
                        @Label("Fact author") String factAuthor,
                        @Label("Fact date") String factDate,
                        @Label("Fact size") String factSize,
                        @Label("Fact layers") String factLayers,
                        @Label("Fact thickness") String factThickness,
                        @Label("Fact parts") String factParts,
                        @Label("Fact holes") String factHoles,
                        @Label("Fact sheets") String factSheets) {
    }

    /** What a layer is drawn with. */
    public Pen penOf(SheetLayer layer) {
        Pen pen = layers.get(layer);
        if (pen == null) {
            throw new IllegalStateException("plugin/drawings/sheets gives no pen to the layer " + layer);
        }
        return pen;
    }
}
