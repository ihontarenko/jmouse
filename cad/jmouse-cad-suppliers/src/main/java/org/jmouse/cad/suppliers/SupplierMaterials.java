package org.jmouse.cad.suppliers;

import org.jmouse.cad.document.part.ProviderState;
import org.jmouse.cad.exchange.pcb.BillOfMaterials;
import org.jmouse.suppliers.SupplierFailure;
import org.jmouse.suppliers.SupplierOffer;
import org.jmouse.suppliers.SupplierOrder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * 💶 A bill of materials priced at the distributors, as they answer right now: for each kind of part, what each of them would
 * sell of exactly that part.
 *
 * <p>⚠️ <strong>Only the part's own number is looked for, never its value.</strong> A distributor answers {@code 10 kΩ} with
 * a page of resistors of any size and tolerance, and the first of them priced here would read as what the pcb needs. A
 * kind with no number says so; it is not guessed.</p>
 */
public final class SupplierMaterials {

    /** Where a line stands with one distributor; or, with no distributor named, before any was asked. */
    public enum Standing {
        /** It holds what the pcbs need. */
        IN_STOCK,
        /** It sells the part and holds too few. */
        SHORT,
        /** It sells the part and did not say how many it holds. */
        STOCK_UNSTATED,
        /** It answered, and no offer names this part. */
        NO_OFFER,
        /** It could not be asked: refused, or unreachable. */
        NOT_ASKED,
        /** The component names no part number, so nobody was asked. */
        NO_PART_NUMBER
    }

    /**
     * One line of the table: one kind of part at one distributor.
     *
     * @param line       the kind of part
     * @param needed     how many the pcbs need
     * @param partNumber the manufacturer's part number; null when the component names none
     * @param supplier   the distributor's name; null when nobody was asked
     * @param order      what buying it there comes to; null without an offer
     * @param standing   where it stands
     */
    public record Row(BillOfMaterials.Line line, long needed, String partNumber, String supplier, SupplierOrder order,
                      Standing standing) {
    }

    /**
     * @param rows  the table, by the bill's order
     * @param notes what a reader has to know of the whole: what each distributor came to, who could not be asked
     */
    public record Priced(List<Row> rows, List<String> notes) {

        public Priced {
            rows = List.copyOf(rows);
            notes = List.copyOf(notes);
        }
    }

    private SupplierMaterials() {
    }

    /**
     * @param partNumberOf the part number a component names, by its slug
     * @param pcbs       how many pcbs are built, at least one
     * @throws IllegalArgumentException when no distributor is configured, with what each lacks
     */
    public static Priced of(BillOfMaterials materials, Function<String, Optional<String>> partNumberOf, List<SupplierParts> places,
                            int pcbs) {
        if (pcbs < 1) {
            throw new IllegalArgumentException("A price is for one pcb at least: " + pcbs);
        }
        List<SupplierParts> asked = places.stream().filter(SupplierParts::configured).toList();
        if (asked.isEmpty()) {
            throw new IllegalArgumentException("No distributor is configured to price the pcb. " + String.join(" ",
                    places.stream().map(SupplierParts::state).map(ProviderState::detail).toList()));
        }
        List<Row> rows = new ArrayList<>();
        Map<SupplierParts, String> refusals = new LinkedHashMap<>();
        for (BillOfMaterials.Line line : materials.lines()) {
            long needed = Math.multiplyExact((long) line.quantity(), pcbs);
            Optional<String> partNumber = partNumberOf.apply(line.component()).map(String::strip).filter(number -> !number.isEmpty());
            if (partNumber.isEmpty()) {
                rows.add(new Row(line, needed, null, null, null, Standing.NO_PART_NUMBER));
                continue;
            }
            for (SupplierParts place : asked) {
                rows.add(rowAt(place, line, needed, partNumber.get(), refusals));
            }
        }
        return new Priced(rows, notesOf(places, rows, refusals, pcbs));
    }

    /** A distributor that refused once is not asked again: each call counts against its daily allowance. */
    private static Row rowAt(SupplierParts place, BillOfMaterials.Line line, long needed, String partNumber,
                             Map<SupplierParts, String> refusals) {
        if (refusals.containsKey(place)) {
            return new Row(line, needed, partNumber, place.name(), null, Standing.NOT_ASKED);
        }
        List<SupplierOffer> offers;
        try {
            offers = place.offers(partNumber);
        } catch (SupplierFailure refused) {
            refusals.put(place, refused.getMessage());
            return new Row(line, needed, partNumber, place.name(), null, Standing.NOT_ASKED);
        }
        Optional<SupplierOrder> order = SupplierOrder.best(offers, partNumber, needed);
        return new Row(line, needed, partNumber, place.name(), order.orElse(null),
                order.map(SupplierMaterials::standingOf).orElse(Standing.NO_OFFER));
    }

    private static Standing standingOf(SupplierOrder order) {
        return switch (order.availability()) {
            case IN_STOCK -> Standing.IN_STOCK;
            case SHORT -> Standing.SHORT;
            case UNSTATED -> Standing.STOCK_UNSTATED;
        };
    }

    private static List<String> notesOf(List<SupplierParts> places, List<Row> rows, Map<SupplierParts, String> refusals, int pcbs) {
        List<String> notes = new ArrayList<>();
        for (SupplierParts place : places) {
            if (!place.configured()) {
                notes.add(place.name() + " was not asked: " + place.state().detail());
            } else if (refusals.containsKey(place)) {
                notes.add(place.name() + " could not be asked: " + refusals.get(place));
            } else {
                notes.add(comesTo(place.name(), rows, pcbs));
            }
        }
        List<String> unnumbered = rows.stream().filter(row -> row.standing() == Standing.NO_PART_NUMBER)
                .map(row -> row.line().component()).distinct().toList();
        if (!unnumbered.isEmpty()) {
            notes.add("Not priced, as the component names no part number: " + String.join(", ", unnumbered) + ".");
        }
        return notes;
    }

    /** {@code Mouser: 3 of 4 kinds in stock; 12.40 USD for 5 pcbs.} — by currency, as a distributor may answer in two. */
    private static String comesTo(String supplier, List<Row> rows, int pcbs) {
        List<Row> ours = rows.stream().filter(row -> supplier.equals(row.supplier())).toList();
        long inStock = ours.stream().filter(row -> row.standing() == Standing.IN_STOCK).count();
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        long unpriced = 0;
        for (Row row : ours) {
            if (row.order() == null || row.order().total() == null) {
                unpriced++;
                continue;
            }
            String currency = row.order().offer().currency() == null ? "" : row.order().offer().currency();
            totals.merge(currency, row.order().total(), BigDecimal::add);
        }
        String prices = totals.isEmpty() ? "no prices" : String.join(" + ", totals.entrySet().stream()
                .map(total -> (total.getValue().toPlainString() + " " + total.getKey()).strip()).toList());
        String missing = unpriced == 0 ? "" : ", " + unpriced + " unpriced";
        return supplier + ": " + inStock + " of " + ours.size() + " kinds in stock; " + prices + " for " + pcbs
                + (pcbs == 1 ? " pcb" : " pcbs") + missing + ".";
    }
}
