package org.jmouse.cad.plugin.suppliers;

import org.jmouse.settings.Label;
import org.jmouse.cad.suppliers.SupplierMaterials;

import java.util.List;
import java.util.Map;

/**
 * How a priced bill of materials is written — which columns, under what headings, how a standing is said —: data, in the
 * plugin's jar ({@code settings/plugin/cad-suppliers/materials}).
 *
 * @param file      what its file is called after the pcb's name: {@code supplier-materials.csv}
 * @param standings how each standing is said
 * @param columns   the columns, in order
 */
public record SupplierMaterialsTable(@Label("File") String file,
                                     @Label("Standings") Map<SupplierMaterials.Standing, String> standings,
                                     @Label("Columns") List<Column> columns) {

    /**
     * A column: its heading, and the field it shows — {@code item}, {@code designators}, {@code quantity} (on one pcb),
     * {@code needed} (on all of them), {@code value}, {@code footprint}, {@code component}, {@code part-number},
     * {@code manufacturer}, {@code supplier}, {@code standing}, {@code order-number}, {@code buy} (how many have to be
     * bought), {@code stock}, {@code minimum-order}, {@code lead-time}, {@code status}, {@code currency},
     * {@code unit-price}, {@code total}, {@code page}.
     *
     * @param heading what the column is headed with
     * @param field   what it shows, by the field's word
     */
    public record Column(@Label("Heading") String heading,
                         @Label("Field") String field) {
    }

    public SupplierMaterialsTable {
        standings = Map.copyOf(standings);
        columns = List.copyOf(columns);
    }
}
