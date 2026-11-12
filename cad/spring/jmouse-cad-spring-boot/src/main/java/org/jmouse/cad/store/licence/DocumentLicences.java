package org.jmouse.cad.store.licence;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ExportedFile;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.library.LicenceLedger;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.licence.LicenceHome;
import org.jmouse.licence.Licences;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧾 The licences of the documents a design is built from, as this installation keeps them: every file that leaves it — an export,
 * a release — leaves only as their licences let it, with the credit they ask for travelling beside it. A work whose licence
 * refuses to be passed on stops the export, named, so it can be replaced by one's own drawing.
 */
public class DocumentLicences {

    private final Licences licences;
    private final String   ownHolder;

    /**
     * @param licences  the terms of the licences works are taken under
     * @param ownHolder who holds the program's own library, whose works owe nothing to a design made with it
     */
    public DocumentLicences(Licences licences, String ownHolder) {
        this.licences = licences;
        this.ownHolder = ownHolder;
    }

    /**
     * One borrowed work a document carries, as a person reads it: what it is, whose, under what licence — each licence's text
     * linked — and what that licence answers for the use it is put to.
     *
     * @param use     how it travels: {@code redistributed} as itself, {@code embedded} built into the document
     * @param verdict what its licence answers: {@code free}, {@code attributed}, {@code same-terms}, {@code refused}
     * @param homes   where each licence and exception it names is read
     */
    public record BorrowedWork(String slug, String title, String kind, String use, String source, String authors, String license,
                               String verdict, String reason, List<LicenceHome> homes) {
    }

    /** The borrowed works a ledger holds, as a person reads them. */
    public List<BorrowedWork> worksOf(LicenceLedger ledger) {
        return ledger.entries().stream().map(entry -> new BorrowedWork(entry.slug(), entry.title(), entry.kind(), entry.ruling().use().word(),
                entry.provenance().source(), entry.provenance().authors(), entry.provenance().license(), entry.ruling().verdict().word(),
                entry.ruling().reason(), licences.homes(entry.provenance().license()))).toList();
    }

    /** What passing a document on owes the holders of what it is, and of what it is built from. */
    public LicenceLedger passedOn(DocumentNode document, CatalogueDocuments catalogue) {
        try {
            return LicenceLedger.passedOn(document, catalogue, licences, ownHolder);
        } catch (IllegalArgumentException unreadable) {
            throw CadProblem.invalid("A licence the design names does not read: " + unreadable.getMessage());
        }
    }

    /**
     * The files a document was written as, as they may leave: refused when a work it carries may not be passed on; otherwise with
     * the credit its works ask for written beside them, and said in the notes.
     *
     * @throws CadProblem when a work's licence refuses this use, naming each one and why
     */
    public ExportedFiles credited(ExportedFiles written, LicenceLedger ledger) {
        if (!ledger.refused().isEmpty()) {
            throw CadProblem.conflict("It cannot be passed on: " + String.join(" ", ledger.refusals())
                                      + " Replace each with a drawing of your own, or one under terms that let it travel.");
        }
        if (ledger.owingCredit().isEmpty()) {
            return written;
        }
        List<ExportedFile> files = new ArrayList<>(written.files());
        files.add(new ExportedFile(licences.noticeFile(), ledger.notice(licences)));
        List<String> notes = new ArrayList<>(written.notes());
        notes.add(ledger.owingCredit().size() + " borrowed works ask for their credit to travel with it: it is written in "
                  + licences.noticeFile() + ".");
        return new ExportedFiles(setName(written), files, notes);
    }

    /** What the files are called together: a set keeps its name, one file becomes a set called as the file is, without its extension. */
    private static String setName(ExportedFiles written) {
        if (written.files().size() > 1) {
            return written.name();
        }
        int extension = written.name().lastIndexOf('.');
        return extension > 0 ? written.name().substring(0, extension) : written.name();
    }
}
