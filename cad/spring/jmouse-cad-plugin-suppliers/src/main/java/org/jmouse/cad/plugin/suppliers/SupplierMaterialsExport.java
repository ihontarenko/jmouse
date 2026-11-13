package org.jmouse.cad.plugin.suppliers;

import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ExportPurpose;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.exchange.pcb.BillOfMaterials;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.suppliers.SupplierMaterials;
import org.jmouse.cad.suppliers.SupplierParts;
import org.jmouse.core.text.CsvText;
import org.jmouse.suppliers.SupplierOffer;
import org.jmouse.suppliers.SupplierOrder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * 💶 A pcb's bill of materials with what each distributor would sell of it right now, as a CSV: one row a kind of part at
 * one distributor. Written at the moment it is asked for and kept by nobody, as a price changes by the hour.
 */
public final class SupplierMaterialsExport implements DocumentExport {

    /** The format's word on the exchange routes: {@code /exchange/supplier-materials/export}. */
    private static final String FORMAT = "supplier-materials";

    /** The fields of the component's own head that say which part it is. */
    private static final String PART_NUMBER = "part-number";
    private static final String MANUFACTURER = "manufacturer";

    private final List<SupplierParts>    places;
    private final IntSupplier            pcbs;
    private final Supplier<SupplierMaterialsTable> table;

    /** @param pcbs how many pcbs are priced, read at each export: it may be changed while the server runs */
    public SupplierMaterialsExport(List<SupplierParts> places, IntSupplier pcbs, Supplier<SupplierMaterialsTable> table) {
        this.places = List.copyOf(places);
        this.pcbs = pcbs;
        this.table = table;
    }

    @Override
    public String id() {
        return FORMAT;
    }

    @Override
    public String title() {
        return "Bill of materials · supplier offers";
    }

    /** A bill a person buys from. */
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
        PcbFabrication fabrication = PcbFabrication.of(document, catalogue);
        SupplierMaterials.Priced priced = SupplierMaterials.of(BillOfMaterials.of(fabrication),
                component -> headField(fabrication, component, PART_NUMBER), places, pcbs.getAsInt());
        List<String> notes = new ArrayList<>(priced.notes());
        notes.addAll(fabrication.notes());
        return ExportedFiles.single(fabrication.name() + "-" + table.get().file(), written(fabrication, priced), notes);
    }

    /** The table's text, its first line the headings; the same item number on every row of one kind. */
    private String written(PcbFabrication fabrication, SupplierMaterials.Priced priced) {
        List<String> lines = new ArrayList<>();
        lines.add(CsvText.row(table.get().columns().stream().map(SupplierMaterialsTable.Column::heading).toList()));
        int item = 0;
        BillOfMaterials.Line previous = null;
        for (SupplierMaterials.Row row : priced.rows()) {
            if (row.line() != previous) {
                item++;
                previous = row.line();
            }
            Map<String, String> fields = fields(row, item, headField(fabrication, row.line().component(), MANUFACTURER).orElse(null));
            lines.add(CsvText.row(table.get().columns().stream().map(column -> cell(column, fields)).toList()));
        }
        return String.join("\n", lines) + "\n";
    }

    private static String cell(SupplierMaterialsTable.Column column, Map<String, String> fields) {
        String value = fields.get(column.field());
        if (value == null) {
            throw new IllegalStateException("The table's column '" + column.heading() + "' shows '" + column.field()
                    + "', which no row has.");
        }
        return value;
    }

    /** ⚠️ A distributor's words and a document's are shown, never run: see {@link CsvText#inert}. */
    private Map<String, String> fields(SupplierMaterials.Row row, int item, String writtenManufacturer) {
        SupplierOrder order = row.order();
        Optional<SupplierOffer> offer = Optional.ofNullable(order).map(SupplierOrder::offer);
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("item", String.valueOf(item));
        fields.put("designators", String.join(" ", row.line().designators()));
        fields.put("quantity", String.valueOf(row.line().quantity()));
        fields.put("needed", String.valueOf(row.needed()));
        fields.put("value", outside(row.line().value()));
        fields.put("footprint", row.line().footprint().orElse(""));
        fields.put("component", row.line().component());
        fields.put("part-number", outside(row.partNumber()));
        fields.put("manufacturer", outside(offer.map(SupplierOffer::manufacturer).orElse(writtenManufacturer)));
        fields.put("supplier", outside(row.supplier()));
        fields.put("standing", table.get().standings().get(row.standing()));
        fields.put("order-number", outside(offer.map(SupplierOffer::supplierPartNumber).orElse(null)));
        fields.put("buy", order == null ? "" : String.valueOf(order.quantity()));
        fields.put("stock", number(offer.map(SupplierOffer::stock).orElse(null)));
        fields.put("minimum-order", number(offer.map(SupplierOffer::minimumOrder).orElse(null)));
        fields.put("lead-time", outside(offer.map(SupplierOffer::leadTime).orElse(null)));
        fields.put("status", outside(offer.map(SupplierOffer::status).orElse(null)));
        fields.put("currency", outside(offer.map(SupplierOffer::currency).orElse(null)));
        fields.put("unit-price", price(order == null ? null : order.unitPrice()));
        fields.put("total", price(order == null ? null : order.total()));
        fields.put("page", outside(offer.map(SupplierOffer::page).orElse(null)));
        return fields;
    }

    private static String outside(String text) {
        return text == null ? "" : CsvText.inert(text);
    }

    private static String number(Long value) {
        return value == null ? "" : value.toString();
    }

    private static String price(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static Optional<String> headField(PcbFabrication fabrication, String component, String name) {
        return fabrication.facts().component(component).flatMap(written -> written.head().field(name));
    }
}
