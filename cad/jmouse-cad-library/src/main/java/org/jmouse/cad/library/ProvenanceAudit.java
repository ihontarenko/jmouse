package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.licence.Licences;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔎 What of a collection of documents fails to say whose it is: each document is to name its holder and a licence whose
 * terms the program knows — ours as plainly as anyone else's. Silence is not "ours": a document that says nothing is one
 * nobody checked, and one passed on without its terms cannot be passed on rightly ({@link LicenceLedger}). Whatever ships
 * documents — the library, a product's catalogue — holds them to this.
 */
public final class ProvenanceAudit {

    private ProvenanceAudit() {
    }

    /** What is wrong with each document that does not say whose it is, in words; none where every one does. */
    public static List<String> faults(Iterable<DocumentNode> documents, Licences licences) {
        List<String> faults = new ArrayList<>();
        for (DocumentNode document : documents) {
            Provenance provenance = Provenance.of(document);
            if (provenance.authors() == null) {
                faults.add(document.slug() + " names no holder");
            }
            if (provenance.license() == null) {
                faults.add(document.slug() + " names no licence");
            } else if (!known(provenance.license(), licences)) {
                faults.add(document.slug() + " names the licence '" + provenance.license() + "', whose terms the program does not know");
            }
        }
        return faults;
    }

    /** Whether every licence and exception an SPDX expression names is one whose terms the program holds. */
    private static boolean known(String expression, Licences licences) {
        return licences.homes(expression).stream()
                .allMatch(home -> licences.terms(home.spdx()).isPresent() || licences.exception(home.spdx()).isPresent());
    }
}
