package org.jmouse.cad.suppliers;

import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.exchange.pcb.BillOfMaterials;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.suppliers.PriceBreak;
import org.jmouse.suppliers.SupplierAccess;
import org.jmouse.suppliers.SupplierCredentials;
import org.jmouse.suppliers.SupplierDriver;
import org.jmouse.suppliers.SupplierFailure;
import org.jmouse.suppliers.SupplierOffer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierMaterialsTest {

    private static final Function<String, Optional<String>> NUMBERS = component -> switch (component) {
        case "tl072" -> Optional.of("TL072CDR");
        case "blank" -> Optional.of("  ");
        default -> Optional.empty();
    };

    private static BillOfMaterials.Line line(String component, String... designators) {
        List<PcbFabrication.SeatedPart> parts = new ArrayList<>();
        for (String designator : designators) {
            parts.add(new PcbFabrication.SeatedPart(new Placement(designator, 0, 0, 0, "top", Optional.empty()),
                    component, component, Optional.empty(), Optional.empty(), Map.of()));
        }
        return new BillOfMaterials.Line(component, component, Optional.empty(), parts);
    }

    private static SupplierOffer offer(String partNumber, long stock) {
        return new SupplierOffer("mouser", "595-" + partNumber, partNumber, "Texas Instruments", null, null, null, "Active", null, stock,
                1L, null, "USD", List.of(new PriceBreak(1, new BigDecimal("0.50")), new PriceBreak(10, new BigDecimal("0.40"))),
                null, null, null);
    }

    /** A distributor answering as told, keeping what it was asked. */
    private static final class Distributor implements SupplierDriver {

        final List<String>  asked = new ArrayList<>();
        final String        name;
        List<SupplierOffer> offers = List.of();
        SupplierFailure     failure;

        Distributor(String name) {
            this.name = name;
        }

        @Override
        public String kind() {
            return name.toLowerCase();
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public URI defaultAddress() {
            return URI.create("https://distributor.example");
        }

        @Override
        public int mostPerSearch() {
            return 50;
        }

        @Override
        public List<SupplierOffer> search(SupplierAccess access, String query, int limit) {
            asked.add(query);
            if (failure != null) {
                throw failure;
            }
            return offers;
        }

        SupplierParts place(String key) {
            return new SupplierParts(this, new SupplierAccess(name, null, new SupplierCredentials.ApiKey(key), null), () -> 10,
                    name + " has no key.");
        }
    }

    private final Distributor mouser = new Distributor("Mouser");
    private final Distributor digiKey = new Distributor("DigiKey");

    @Test
    void oneKindIsPricedAtEachDistributorForEveryPcb() {
        mouser.offers = List.of(offer("TL072CDR", 1000));
        digiKey.offers = List.of(offer("TL072CDR", 3));
        SupplierMaterials.Priced priced = SupplierMaterials.of(new BillOfMaterials(List.of(line("tl072", "U1", "U2"))), NUMBERS,
                List.of(mouser.place("key"), digiKey.place("key")), 5);
        assertEquals(2, priced.rows().size());
        SupplierMaterials.Row first = priced.rows().getFirst();
        assertEquals(10, first.needed());
        assertEquals("Mouser", first.supplier());
        assertEquals(SupplierMaterials.Standing.IN_STOCK, first.standing());
        assertEquals(new BigDecimal("0.40"), first.order().unitPrice());
        assertEquals(SupplierMaterials.Standing.SHORT, priced.rows().get(1).standing());
        assertEquals("Mouser: 1 of 1 kinds in stock; 4.00 USD for 5 pcbs.", priced.notes().getFirst());
    }

    @Test
    void aKindWithNoNumberAsksNobodyAndIsNeverGuessed() {
        SupplierMaterials.Priced priced = SupplierMaterials.of(new BillOfMaterials(List.of(line("resistor", "R1"), line("blank", "R2"))),
                NUMBERS, List.of(mouser.place("key")), 1);
        assertTrue(mouser.asked.isEmpty());
        assertEquals(2, priced.rows().size());
        priced.rows().forEach(row -> {
            assertEquals(SupplierMaterials.Standing.NO_PART_NUMBER, row.standing());
            assertNull(row.supplier());
        });
        assertTrue(priced.notes().getLast().contains("resistor, blank"));
    }

    @Test
    void onlyAnOfferOfThatVeryPartCounts() {
        mouser.offers = List.of(offer("TL072CP", 1000));
        SupplierMaterials.Priced priced = SupplierMaterials.of(new BillOfMaterials(List.of(line("tl072", "U1"))), NUMBERS,
                List.of(mouser.place("key")), 1);
        assertEquals(SupplierMaterials.Standing.NO_OFFER, priced.rows().getFirst().standing());
        assertNull(priced.rows().getFirst().order());
    }

    @Test
    void aDistributorThatRefusedIsNotAskedAgainAndTheOtherStillIs() {
        mouser.failure = new SupplierFailure(SupplierFailure.Reason.REFUSED, "Mouser refused the key.");
        digiKey.offers = List.of(offer("TL072CDR", 1000));
        BillOfMaterials materials = new BillOfMaterials(List.of(line("tl072", "U1"), line("tl072", "U2")));
        SupplierMaterials.Priced priced = SupplierMaterials.of(materials, NUMBERS, List.of(mouser.place("key"), digiKey.place("key")), 1);
        assertEquals(1, mouser.asked.size());
        assertEquals(2, digiKey.asked.size());
        assertEquals(List.of(SupplierMaterials.Standing.NOT_ASKED, SupplierMaterials.Standing.IN_STOCK,
                SupplierMaterials.Standing.NOT_ASKED, SupplierMaterials.Standing.IN_STOCK),
                priced.rows().stream().map(SupplierMaterials.Row::standing).toList());
        assertTrue(priced.notes().getFirst().startsWith("Mouser could not be asked: "));
    }

    @Test
    void aDistributorWithoutCredentialsIsNamedAndSkipped() {
        digiKey.offers = List.of(offer("TL072CDR", 1000));
        SupplierMaterials.Priced priced = SupplierMaterials.of(new BillOfMaterials(List.of(line("tl072", "U1"))), NUMBERS,
                List.of(mouser.place(""), digiKey.place("key")), 1);
        assertTrue(mouser.asked.isEmpty());
        assertEquals(1, priced.rows().size());
        assertEquals("Mouser was not asked: Mouser has no key.", priced.notes().getFirst());
    }

    @Test
    void withNobodyToAskItRefusesAndSaysWhatIsMissing() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> SupplierMaterials.of(
                new BillOfMaterials(List.of(line("tl072", "U1"))), NUMBERS, List.of(mouser.place(""), digiKey.place("")), 1));
        assertTrue(refused.getMessage().contains("Mouser has no key."));
        assertTrue(refused.getMessage().contains("DigiKey has no key."));
    }
}
