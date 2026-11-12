package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.licence.LicenceHome;
import org.jmouse.licence.LicenceRuling;
import org.jmouse.licence.LicenceUse;
import org.jmouse.licence.Licences;
import org.jmouse.licence.Verdict;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 🧾 Whose work a document carries and what passing it on owes each holder: every borrowed document — one whose {@link Provenance}
 * names a holder or a licence — among the document and what it is {@link BuiltFrom}, each ruled on by its licence. The document
 * itself is passed on as itself; what it is built from, built into it. What a person made, or a design brought of its own, names
 * no holder and owes nothing; nor does what the program's own library holds ({@link CadLibrary#holder()}), which its designs
 * are made with.
 *
 * @param entries the borrowed works, the document's own first, then as they were met
 */
public record LicenceLedger(List<Entry> entries) {

    public LicenceLedger {
        entries = List.copyOf(entries);
    }

    /**
     * One borrowed work and what its licence answers for its use.
     *
     * @param slug       the document's slug
     * @param title      its title
     * @param kind       the word its kind is written with: {@code footprint}, {@code model}
     * @param provenance where it came from, whose it is and under what terms
     * @param ruling     what its licence answers for the use it is put to
     */
    public record Entry(String slug, String title, String kind, Provenance provenance, LicenceRuling ruling) {
    }

    /**
     * What passing a document on owes: the document as itself, and every document it is built from as built into it.
     *
     * @param ownHolder the holder whose works are the program's own; none of theirs is ledgered
     */
    public static LicenceLedger passedOn(DocumentNode document, CatalogueDocuments catalogue, Licences licences, String ownHolder) {
        List<Entry> entries = new ArrayList<>();
        entryOf(document, LicenceUse.REDISTRIBUTED, licences, ownHolder).ifPresent(entries::add);
        BuiltFrom.of(document, catalogue).forEach(built -> entryOf(built, LicenceUse.EMBEDDED, licences, ownHolder).ifPresent(entries::add));
        return new LicenceLedger(entries);
    }

    /** What taking documents into a collection as its own owes, each as itself. */
    public static LicenceLedger adopted(List<DocumentNode> documents, Licences licences, String ownHolder) {
        return new LicenceLedger(documents.stream().flatMap(document -> entryOf(document, LicenceUse.ADOPTED, licences, ownHolder).stream())
                .toList());
    }

    private static Optional<Entry> entryOf(DocumentNode document, LicenceUse use, Licences licences, String ownHolder) {
        Provenance provenance = Provenance.of(document);
        if (!provenance.borrowed() || ownHolder.equals(provenance.authors())) {
            return Optional.empty();
        }
        return Optional.of(new Entry(document.slug(), document.title(), document.kind().word(), provenance,
                licences.rule(provenance.license(), use)));
    }

    /** What the whole owes: the most any one work owes; {@link Verdict#FREE} for a ledger of nothing borrowed. */
    public Verdict verdict() {
        return entries.stream().map(entry -> entry.ruling().verdict()).max(Comparator.naturalOrder()).orElse(Verdict.FREE);
    }

    /** The works whose licence refuses their use. */
    public List<Entry> refused() {
        return entries.stream().filter(entry -> !entry.ruling().permits()).toList();
    }

    /** The works whose credit must travel with them. */
    public List<Entry> owingCredit() {
        return entries.stream().filter(entry -> entry.ruling().verdict().owesCredit()).toList();
    }

    /**
     * The credit of each work that owes it, as a plain text that travels with what is passed on: what the work is, where it was
     * taken from, who holds it and under what licence — that licence's text linked — and what the licence asks.
     */
    public String notice(Licences licences) {
        return owingCredit().stream().map(entry -> credit(entry, licences)).collect(Collectors.joining("\n\n")) + "\n";
    }

    /** One line a person reads of each refused work: what it is and why. */
    public List<String> refusals() {
        return refused().stream().map(entry -> entry.title() + " (" + entry.kind() + " " + entry.slug() + "): " + entry.ruling().reason()).toList();
    }

    private static String credit(Entry entry, Licences licences) {
        Provenance provenance = entry.provenance();
        List<String> lines = new ArrayList<>();
        lines.add(entry.title() + " — " + entry.kind() + " " + entry.slug());
        Optional.ofNullable(provenance.source()).ifPresent(source -> lines.add("Taken from: " + source));
        Optional.ofNullable(provenance.authors()).ifPresent(authors -> lines.add("Held by: " + authors));
        List<String> homes = licences.homes(provenance.license()).stream().map(LicenceHome::home).toList();
        lines.add("Licence: " + Optional.ofNullable(provenance.license()).orElse("not stated") + (homes.isEmpty() ? "" : " — " + String.join(", ", homes)));
        lines.add(entry.ruling().reason());
        return String.join("\n", lines);
    }
}
