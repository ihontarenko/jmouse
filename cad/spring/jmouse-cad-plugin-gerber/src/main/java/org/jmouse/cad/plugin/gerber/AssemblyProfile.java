package org.jmouse.cad.plugin.gerber;

import java.util.List;
import java.util.Map;

/**
 * How a set's assembly tables are written — which columns, in what order, under what headings, how a side and a unit are
 * said —: data, read from a profile in the jar. The plain set has its own ({@code assembly.json}, KiCad's columns); a
 * board house brings its own and the same writer writes it ({@link AssemblyTables}).
 */
public record AssemblyProfile(Table placement, Table materials) {

    /**
     * One table.
     *
     * @param file                what its file is called after the set's name: {@code pos.csv} is {@code <name>-pos.csv}
     * @param role                what it is, as the set's README says it
     * @param sides               how each side is said: {@code top} → {@code Top}
     * @param unit                written after every length: {@code mm}, or nothing
     * @param designatorSeparator what stands between a line's designators: {@code " "}, {@code ","}
     * @param mountings           how each way a part is mounted is said: {@code surface} → {@code SMD}
     */
    public record Table(String file, String role, Map<String, String> sides, String unit, String designatorSeparator,
                        Map<String, String> mountings, List<Column> columns) {

        public Table {
            sides = sides == null ? Map.of() : Map.copyOf(sides);
            mountings = mountings == null ? Map.of() : Map.copyOf(mountings);
            unit = unit == null ? "" : unit;
            designatorSeparator = designatorSeparator == null ? " " : designatorSeparator;
            columns = List.copyOf(columns);
        }
    }

    /**
     * A column: its heading, and the field it shows — {@code designator}, {@code value}, {@code footprint},
     * {@code component}, {@code x}, {@code y}, {@code rotation}, {@code side}, {@code designators}, {@code quantity},
     * {@code item} (the line's number), {@code mounting} ({@code surface} or {@code through-hole}, by its pads), or
     * {@code meta:<name>}, a line of the component's own head ({@code meta:lcsc}).
     */
    public record Column(String heading, String field) {
    }

    /** A profile kept in a jar beside the class that names it. */
    public static AssemblyProfile of(Class<?> owner, String resource) {
        return PluginResources.read(owner, resource, AssemblyProfile.class);
    }
}
