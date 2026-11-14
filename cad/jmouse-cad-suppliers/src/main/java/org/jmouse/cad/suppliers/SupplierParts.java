package org.jmouse.cad.suppliers;

import org.jmouse.cad.document.part.PartCandidate;
import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.document.part.ProviderState;
import org.jmouse.suppliers.SupplierAccess;
import org.jmouse.suppliers.SupplierDriver;
import org.jmouse.suppliers.SupplierFailure;
import org.jmouse.suppliers.SupplierOffer;

import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * 🔎 One distributor as a place parts are looked for: live, with its stock and prices, and kept by nobody.
 *
 * <p>A distributor's offer changes by the hour, so a candidate is never {@linkplain PartCandidate#fetchable() fetchable}
 * and nothing is cached: what a reader sees is what it answered just now, with the page to buy it on.</p>
 */
public final class SupplierParts implements PartSearchProvider {

    private final SupplierDriver driver;
    /** What it is reached with, read at each call: a key changed while the server runs is the next call's. */
    private final Supplier<SupplierAccess> access;
    /** How many offers one search asks for, read at each search: it may be changed while the server runs. */
    private final IntSupplier    limit;
    private final String         whatIsMissing;

    /** @param whatIsMissing what a reader is told while the access's credentials are not given: which settings they are */
    public SupplierParts(SupplierDriver driver, SupplierAccess access, IntSupplier limit, String whatIsMissing) {
        this(driver, constant(access), limit, whatIsMissing);
    }

    private static Supplier<SupplierAccess> constant(SupplierAccess access) {
        Objects.requireNonNull(access, "access");
        return () -> access;
    }

    /** @param access what it is reached with now */
    public SupplierParts(SupplierDriver driver, Supplier<SupplierAccess> access, IntSupplier limit, String whatIsMissing) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.access = Objects.requireNonNull(access, "access");
        this.limit = Objects.requireNonNull(limit, "limit");
        this.whatIsMissing = whatIsMissing;
    }

    @Override
    public String id() {
        return driver.kind();
    }

    @Override
    public String name() {
        return driver.name();
    }

    /** Whether the server was given what this distributor knows it by. */
    public boolean configured() {
        return access.get().credentials().complete();
    }

    /** A distributor's parts: part numbers with their offers, not yet any drawing. */
    @Override
    public Set<PartCandidate.Kind> offers() {
        return Set.of(PartCandidate.Kind.PART);
    }

    @Override
    public ProviderState state() {
        if (!configured()) {
            return ProviderState.notConfigured(whatIsMissing);
        }
        return ProviderState.ready("Live stock and prices; shown, not kept.");
    }

    @Override
    public List<PartCandidate> search(String query, Set<PartCandidate.Kind> kinds, int limit) {
        if (!configured() || query == null || query.isBlank()) {
            return List.of();
        }
        try {
            return offers(query.strip(), limit).stream().map(this::candidateOf).filter(Objects::nonNull).toList();
        } catch (SupplierFailure refused) {
            throw new IllegalStateException(refused.getMessage());
        }
    }

    /**
     * What it offers for a part number or a few words, as many as the plugin's setting asks for.
     *
     * @throws SupplierFailure when it could not be asked, or refused
     */
    public List<SupplierOffer> offers(String query) {
        return offers(query, Integer.MAX_VALUE);
    }

    private List<SupplierOffer> offers(String query, int limit) {
        return driver.search(access.get(), query, Math.min(limit, Math.min(this.limit.getAsInt(), driver.mostPerSearch())));
    }

    /** The offer as a candidate; null where it names no part at all, so there is nothing a reader could look up. */
    private PartCandidate candidateOf(SupplierOffer offer) {
        String title = offer.partNumber() != null ? offer.partNumber() : offer.supplierPartNumber();
        if (title == null) {
            return null;
        }
        String identifier = offer.supplierPartNumber() != null ? offer.supplierPartNumber() : offer.partNumber();
        PartCandidate.Link page = offer.page() == null ? null : new PartCandidate.Link(driver.name(), offer.page());
        return new PartCandidate(id(), identifier, PartCandidate.Kind.PART, title, offer.partNumber(), offer.manufacturer(),
                offer.description(), offer.datasheet(), page, null, false, supplyOf(offer));
    }

    private static PartCandidate.Supply supplyOf(SupplierOffer offer) {
        List<PartCandidate.Price> prices = offer.priceBreaks().stream()
                .map(priceBreak -> new PartCandidate.Price(priceBreak.quantity(), priceBreak.unitPrice())).toList();
        return new PartCandidate.Supply(offer.supplierPartNumber(), offer.status(), offer.stock(), offer.minimumOrder(),
                offer.leadTime(), offer.currency(), prices);
    }
}
