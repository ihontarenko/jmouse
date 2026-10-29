package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.store.problem.CadProblem;

/** An owner a request names in words — {@code PROJECT:42} —, read once for every route and service that takes one. */
public final class WrittenOwners {

    private WrittenOwners() {
    }

    /**
     * The owner written, or null when none is: the installation's place, or the catalogue's.
     *
     * @throws CadProblem.Invalid when something is written that names no owner
     */
    public static DocumentOwner optional(String written) {
        if (written == null || written.isBlank()) {
            return null;
        }
        return DocumentOwner.parse(written.strip())
                .orElseThrow(() -> CadProblem.invalid("An owner is written TYPE:id — PROJECT:42."));
    }
}
