package org.jmouse.cad.exchange;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.document.part.FetchedFile;
import org.jmouse.cad.exchange.imported.ImportFiling;

import java.util.ArrayList;
import java.util.List;

/**
 * What an import made, in the order it is stored.
 *
 * @param library what a design is built from — footprints, symbols, components —, each before what names it; one already
 *                held elsewhere is reused rather than written over
 * @param designs the circuits, each placed sheet before the sheet that places it, the root last; empty for an import
 *                that made a component alone
 * @param notes   what the import could not carry, part by part
 * @param files   the files its documents hold, a model's mesh, by the document's slug
 */
public record ImportedDocuments(List<DocumentNode> library, List<DocumentNode> designs, List<String> notes, List<FetchedFile> files) {

    /** An import of documents alone. */
    public ImportedDocuments(List<DocumentNode> library, List<DocumentNode> designs, List<String> notes) {
        this(library, designs, notes, List.of());
    }

    public ImportedDocuments {
        library = List.copyOf(library);
        designs = List.copyOf(designs);
        notes = List.copyOf(notes == null ? List.of() : notes);
        files = List.copyOf(files == null ? List.of() : files);
        if (library.isEmpty() && designs.isEmpty()) {
            throw new IllegalArgumentException("An import made nothing.");
        }
    }

    /**
     * The same, the category of each library its library documents come from before them, as the filing writes it
     * ({@link ImportFiling#withLibraryCategories}): stored in order, every document finds its category standing.
     */
    public ImportedDocuments filedBy(ImportFiling filing) {
        return new ImportedDocuments(filing.withLibraryCategories(library), designs, notes, files);
    }

    /**
     * The same, each library document whose own text names no holder and no terms given those the reader stated for its
     * file ({@link ExchangeFiles#stated()}): a library somebody made is theirs, said once by whoever brought it in. One
     * whose text names its own keeps them. Stated nothing, the documents claim none and a note says so.
     *
     * @param file the file they were read from, as the note names it
     */
    public ImportedDocuments whoseAsStated(Provenance stated, String file) {
        if (!stated.borrowed()) {
            List<String> said = new ArrayList<>(notes);
            said.add(file + " said no holder and no licence: its documents claim none until somebody says whose they are.");
            return new ImportedDocuments(library, designs, said, files);
        }
        Provenance terms = new Provenance(null, stated.authors(), stated.license());
        for (DocumentNode document : library) {
            document.block(MetaNode.class).filter(meta -> !Provenance.of(meta).borrowed()).ifPresent(terms::writeInto);
        }
        return this;
    }

    /** The document the import stands for: the root of its design, or the one thing it made. */
    public DocumentNode root() {
        return designs.isEmpty() ? library.getLast() : designs.getLast();
    }
}
