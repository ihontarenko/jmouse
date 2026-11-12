package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.simulation.CatalogueDocuments;

import java.util.List;
import java.util.Optional;

/**
 * A footprint another product names without saying which library it is of — Altium's schematic writes {@code R0805},
 * keeping the library in a project it does not carry —, matched against the footprints seen where the import is read:
 * those an earlier import brought, the place's own, the catalogue's. One footprint of that name is the one meant, and the
 * part lands on it; none or several leave the part on none — its package still named as written —, and the import says why.
 *
 * @param written what the product wrote
 * @param codes   the codes of the footprints of that name seen here, the nearest first
 */
public record NamedFootprint(String written, List<String> codes) {

    public NamedFootprint {
        codes = List.copyOf(codes);
    }

    /** The footprint a written name is matched to where the import is read. */
    public static NamedFootprint of(String written, CatalogueDocuments catalogue) {
        String name = ImportedNames.codeName(written);
        return new NamedFootprint(written, name.isEmpty() ? List.of() : catalogue.codesNamed(TechnicalClass.FOOTPRINT, name));
    }

    /** How the name was matched. */
    public enum Match {
        /** One footprint has the name: it is the one meant. */
        ONE,
        /** No footprint seen here has it: its library has not been brought in. */
        NONE,
        /** Several have it, in different libraries: which one is meant is the reader's to say. */
        SEVERAL
    }

    public Match match() {
        return switch (codes.size()) {
            case 0 -> Match.NONE;
            case 1 -> Match.ONE;
            default -> Match.SEVERAL;
        };
    }

    /** The footprint the part lands on: the one of that name; empty when none or several are. */
    public Optional<String> code() {
        return match() == Match.ONE ? Optional.of(codes.getFirst()) : Optional.empty();
    }

    /** What the import says of a name it could not match; empty for one it matched. */
    public Optional<String> note() {
        return switch (match()) {
            case ONE -> Optional.empty();
            case NONE -> Optional.of("Footprint " + written + ": none of that name is held here — import its PCB library, "
                    + "then the schematic again, or choose one on the part.");
            case SEVERAL -> Optional.of("Footprint " + written + ": several are named so (" + String.join(", ", codes)
                    + ") — choose one on the part.");
        };
    }
}
