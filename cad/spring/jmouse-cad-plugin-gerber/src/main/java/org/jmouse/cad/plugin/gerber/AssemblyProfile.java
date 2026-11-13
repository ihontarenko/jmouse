package org.jmouse.cad.plugin.gerber;

import org.jmouse.settings.Label;
import java.util.List;
import java.util.Map;

/**
 * How a set's assembly tables are written — which columns, in what order, under what headings, how a side and a unit are
 * said —: data, a section of the settings tree each plugin that makes such files declares. The plain set has its own
 * ({@code settings/plugin/cad-gerber/assembly}, KiCad's columns); a pcb house brings its own and the same writer writes it
 * ({@link AssemblyTables}).
 *
 * @param placement the placement table: where each part stands and how it is turned
 * @param materials the bill of materials: what each line of parts is
 */
public record AssemblyProfile(@Label("Placement") PlacementTable placement,
                              @Label("Materials") MaterialsTable materials) {

    /**
     * What both tables are: a file of the set, a line of its README, and columns. Every word a table says is in its file — a
     * value it does not change is written as it is, never left for the engine to guess.
     */
    public sealed interface Table permits PlacementTable, MaterialsTable {

        /** What its file is called after the set's name: {@code pos.csv} is {@code <name>-pos.csv}. */
        String file();

        /** What it is, as the set's README says it. */
        String role();

        /** How each way a part is mounted is said: {@code surface} → {@code SMD}; a way not named is said as it is. */
        Map<String, String> mountings();

        /** Its columns, in order. */
        List<Column> columns();
    }

    /**
     * The placement table: one line a seated part, where it stands and how it is turned.
     *
     * @param file      what its file is called after the set's name: {@code pos.csv} is {@code <name>-pos.csv}
     * @param role      what it is, as the set's README says it
     * @param sides     how each side is said: {@code top} → {@code Top}; a side not named is said as it is
     * @param unit      written after every length: {@code mm}, or nothing
     * @param mountings how each way a part is mounted is said: {@code surface} → {@code SMD}; a way not named is said as it is
     * @param columns   its columns, in order
     */
    public record PlacementTable(@Label("File") String file,
                                 @Label("Role") String role,
                                 @Label("Sides") Map<String, String> sides,
                                 @Label("Unit") String unit,
                                 @Label("Mountings") Map<String, String> mountings,
                                 @Label("Columns") List<Column> columns) implements Table {

        public PlacementTable {
            sides = Map.copyOf(sides);
            mountings = Map.copyOf(mountings);
            columns = List.copyOf(columns);
        }
    }

    /**
     * The bill of materials: one line a kind of part, its designators together.
     *
     * @param file                what its file is called after the set's name: {@code bom.csv} is {@code <name>-bom.csv}
     * @param role                what it is, as the set's README says it
     * @param designatorSeparator what stands between a line's designators: {@code " "}, {@code ","}
     * @param mountings           how each way a part is mounted is said: {@code surface} → {@code SMD}; a way not named is said as
     *                            it is
     * @param columns             its columns, in order
     */
    public record MaterialsTable(@Label("File") String file,
                                 @Label("Role") String role,
                                 @Label("Designator separator") String designatorSeparator,
                                 @Label("Mountings") Map<String, String> mountings,
                                 @Label("Columns") List<Column> columns) implements Table {

        public MaterialsTable {
            mountings = Map.copyOf(mountings);
            columns = List.copyOf(columns);
        }
    }

    /**
     * A column: its heading, and the field it shows — {@code designator}, {@code value}, {@code footprint},
     * {@code component}, {@code x}, {@code y}, {@code rotation}, {@code side}, {@code designators}, {@code quantity},
     * {@code item} (the line's number), {@code mounting} ({@code surface} or {@code through-hole}, by its pads), or
     * {@code meta:<name>}, a line of the component's own head ({@code meta:lcsc}).
     *
     * @param heading what the column is headed with
     * @param field   what it shows, by the field's word
     */
    public record Column(@Label("Heading") String heading,
                         @Label("Field") String field) {
    }

}
