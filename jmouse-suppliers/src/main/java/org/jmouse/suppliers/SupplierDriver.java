package org.jmouse.suppliers;

import java.net.URI;
import java.util.List;

/**
 * 🏬 One distributor's protocol.
 *
 * <p>⚠️ <strong>A failure is thrown, never folded into an empty list.</strong> An empty list means the distributor
 * answered and has nothing; a refused key or an unreachable host reads very differently to whoever is buying a part.</p>
 */
public interface SupplierDriver {

    /** How the distributor is named in an address: {@code mouser}. */
    String kind();

    /** How it is named to a reader: {@code Mouser}. */
    String name();

    /** Where it is called when an access names no address of its own. */
    URI defaultAddress();

    /** The most offers it answers one search with. */
    int mostPerSearch();

    /**
     * What it offers for a part number or a few words.
     *
     * @param limit how many offers at most; no more than {@link #mostPerSearch()} are asked for
     * @return the offers, as the distributor ranked them; empty when it has none
     * @throws SupplierFailure when it could not be asked, or refused
     */
    List<SupplierOffer> search(SupplierAccess access, String query, int limit);
}
