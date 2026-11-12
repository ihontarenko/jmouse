package org.jmouse.cad.plugin.drawings;

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
public record SheetSettings(List<Paper> papers, List<Scale> scales, double margin, double columnWidth, double columnGap,
                            double titleHeight, double viewMargin, double rowHeight, double dimensionArrow, int curveSteps,
                            int circleSteps, String schematicPaper, List<String> fontFamilies, List<String> fontFolders, Text text, Ink ink, Cover cover,
                            Map<SheetLayer, Pen> layers, Words words, List<String> fabricationNotes) {


    /**
     * A sheet's paper, lying on its long side.
     *
     * @param name   what the paper is called: A4
     * @param width  its long side, in millimetres
     * @param height its short side, in millimetres
     */
    public record Paper(String name, double width, double height) {
    }

    /**
     * How large a pcb is drawn: {@code factor} millimetres of paper to one of pcb.
     *
     * @param label  how the scale is printed: 10:1
     * @param factor millimetres of paper to one of pcb
     */
    public record Scale(String label, double factor) {
    }

    /**
     * Heights of a capital, by what is printed.
     *
     * @param title          a sheet's title, in millimetres on paper
     * @param heading        a table's heading, in millimetres
     * @param body           a table's text, in millimetres
     * @param label          a label on a view, in millimetres
     * @param holeMark       a hole's mark on the drill drawing, in millimetres
     * @param schematicLeast the least a schematic's text is printed at, however small its scale, in millimetres
     * @param stroke         how wide a letter's stroke is, as a share of its height
     */
    public record Text(double title, double heading, double body, double label, double holeMark, double schematicLeast, double stroke) {
    }

    /**
     * What a layer of a sheet is drawn with.
     *
     * @param pen    how wide its lines are, in millimetres on paper
     * @param colour its colour in a DXF: AutoCAD's index
     * @param hex    its colour in a PDF
     */
    public record Pen(double pen, int colour, String hex) {
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
    public record Ink(String copperTop, String copperBottom, String copperInner, String silkscreen, String substrate) {
    }

    /**
     * A document's front.
     *
     * @param product       the product the documents are made by
     * @param subtitle      what the document is
     * @param markSize      how large the mark is printed, in millimetres
     * @param productHeight how high the product's name is printed, in millimetres
     * @param titleHeight   how high the design's name is printed, in millimetres
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
    public record Cover(String product, String subtitle, double markSize, double productHeight, double titleHeight, double titleAtShare, int factColumns, double markBox, String markPrimary,
                        String markAccent, Dot markDot, String primary, String accent, String muted) {

        /**
         * @param x      its centre across the mark's box
         * @param y      its centre down the box
         * @param radius its radius, in the box's units
         */
        public record Dot(double x, double y, double radius) {
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
    public record Words(String fabricationSheet, String assemblyTopSheet, String assemblyBottomSheet, String holesHeading,
                        String holeMark, String holeSize, String holeSlot, String holePlating, String holeCount, String plated,
                        String unplated, String stackHeading, String stackLayer, String stackThickness, String stackTotal,
                        String stackSilkscreen, String stackMask, String stackCopper, String stackCore, String partsHeading,
                        String partsDesignators, String partsValue, String partsFootprint, String partsCount, String notesHeading,
                        String titleDrawing, String titleSheet, String titleOf, String titleRevision, String titleScale,
                        String titleUnits, String titlePaper, String units, String unsaved, String schematicScale, String schematicSheet, String overviewSheet, String copperSheet, String tablesSheet, String materialsSheet,
                        String contentsSheet, String partsItem, String factAuthor, String factDate, String factSize, String factLayers, String factThickness, String factParts, String factHoles,
                        String factSheets) {
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
