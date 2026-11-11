package org.jmouse.cad.store.exchange;

import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ExchangeRefusal;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.jpa.catalogue.DocumentStore;
import org.jmouse.cad.jpa.catalogue.StoredDocument;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.StoredDocuments;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.el.grammar.GrammarRefusal;
import org.jmouse.plugin.Extensions;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Documents to and from the formats the plugins brought — {@code JMOUSE_CAD_API.md} §2.6. A format only converts; what
 * every format shares is here: whose components a conversion may see, who may read and store, how a refusal is said, and
 * how what an import made is stored beside what the owner holds.
 */
public class DocumentExchange {

    /** A format as the list offers it: what it reads, what it writes. */
    public record Format(String id, String title, List<String> reads, List<String> writes) {
    }

    /** What a read made, unstored: each document as jM text, the one it stands for first. */
    public record Read(List<ReadDocument> documents, List<String> notes) {
    }

    public record ReadDocument(String slug, String kind, String source) {
    }

    /**
     * What an import stored.
     *
     * @param root    the slug of what the import stands for: a design's root circuit, or the one thing it made
     * @param created what was stored new, by slug
     * @param updated what an earlier import made, written over where this one says otherwise
     * @param reused  what the owner or the catalogue already held, kept as it is
     */
    public record Imported(String root, List<String> created, List<String> updated, List<String> reused,
                           List<String> notes) {
    }

    private final Extensions    extensions;
    private final CadCatalogue  catalogue;
    private final DocumentStore documents;
    private final CadAuthority  authority;
    private final CadPlaces     places;
    private final CadLanguage   language;

    public DocumentExchange(Extensions extensions, CadCatalogue catalogue, DocumentStore documents, CadAuthority authority,
                            CadPlaces places, CadLanguage language) {
        this.extensions = extensions;
        this.catalogue = catalogue;
        this.documents = documents;
        this.authority = authority;
        this.places = places;
        this.language = language;
    }

    /** Every format a plugin brought, read or written; none without a plugin. */
    public List<Format> formats() {
        Map<String, Format> formats = new LinkedHashMap<>();
        for (DocumentImport reading : extensions.contributions(DocumentImport.POINT)) {
            formats.put(reading.id(), new Format(reading.id(), reading.title(), reading.fileNames(), List.of()));
        }
        for (DocumentExport writing : extensions.contributions(DocumentExport.POINT)) {
            List<String> kinds = writing.kinds().stream().map(DocumentKind::word).sorted().toList();
            Format known = formats.get(writing.id());
            formats.put(writing.id(), known == null ? new Format(writing.id(), writing.title(), List.of(), kinds)
                    : new Format(known.id(), known.title(), known.reads(), kinds));
        }
        return List.copyOf(formats.values());
    }

    /** Files read as documents and handed back, nothing stored: a netlist opened in an editor. */
    @Transactional(readOnly = true)
    public Read read(String format, ExchangeFiles files, DocumentOwner owner) {
        ImportedDocuments made = converted(() -> importOf(format).read(files, scope(owner)));
        List<ReadDocument> read = new ArrayList<>();
        read.add(readDocument(made.root()));
        for (DocumentNode document : allOf(made)) {
            if (document != made.root()) {
                read.add(readDocument(document));
            }
        }
        return new Read(read, made.notes());
    }

    /**
     * Files read as documents and stored as the owner's: what a design is built from first, each kept as it is where
     * something is already called so — an earlier import's untouched first revision excepted, which this one writes
     * over —, then the circuits, each written over where an earlier import of this owner made it.
     */
    @Transactional
    public Imported importInto(DocumentOwner owner, String format, ExchangeFiles files) {
        authority.require(CadCapability.OWNER_WRITE, places.requireOwner(owner));
        ImportedDocuments made = converted(() -> importOf(format).read(files, catalogue.scope(owner)));
        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        List<String> reused = new ArrayList<>();
        made.library().forEach(document -> storeLibraryDocument(owner, document, created, updated, reused));
        made.designs().forEach(document -> storeDesign(owner, document, created, updated, reused));
        return new Imported(made.root().slug(), created, updated, reused, made.notes());
    }

    /** A document's text written in a format. */
    @Transactional(readOnly = true)
    public ExportedFiles export(String format, String source, DocumentOwner owner) {
        DocumentExport writing = exportOf(format);
        DocumentNode document = read(source);
        if (!writing.kinds().contains(document.kind())) {
            throw CadProblem.invalid(writing.title() + " is written from " + writing.kinds().stream().map(DocumentKind::word)
                    .sorted().toList() + "; this text holds a " + document.kind().word() + ".");
        }
        return converted(() -> writing.write(document, scope(owner)));
    }

    private void storeLibraryDocument(DocumentOwner owner, DocumentNode made, List<String> created, List<String> updated,
                                      List<String> reused) {
        String source = language.language().write(made);
        Optional<StoredDocument> existing = documents.findBySlug(made.slug());
        if (existing.isEmpty()) {
            catalogue.createOwned(owner, source);
            created.add(made.slug());
            return;
        }
        StoredDocument stored = existing.get();
        boolean untouched = stored.owner().map(owner::equals).orElse(false) && stored.getRevision() == 1;
        if (!untouched) {
            reused.add(made.slug());
            return;
        }
        writeOver(stored, source, made.slug(), updated, reused);
    }

    private void storeDesign(DocumentOwner owner, DocumentNode design, List<String> created, List<String> updated,
                             List<String> reused) {
        String source = language.language().write(design);
        Optional<StoredDocument> existing = documents.findBySlug(design.slug());
        if (existing.isEmpty()) {
            catalogue.createOwned(owner, source);
            created.add(design.slug());
            return;
        }
        StoredDocument stored = existing.get();
        if (!stored.owner().map(owner::equals).orElse(false)) {
            throw CadProblem.conflict("A document is already called '" + design.slug() + "' and is not this owner's: "
                                      + "rename what is imported.");
        }
        writeOver(stored, source, design.slug(), updated, reused);
    }

    /** An unchanged text writes nothing: the document is as this import would make it. */
    private void writeOver(StoredDocument stored, String source, String slug, List<String> updated, List<String> reused) {
        int before = stored.getRevision();
        int after = catalogue.update(stored.getId(), source, stored.getVersion()).sourceRevision();
        (after == before ? reused : updated).add(slug);
    }

    private DocumentImport importOf(String format) {
        return extensions.contributions(DocumentImport.POINT).stream().filter(reading -> reading.id().equals(format))
                .findFirst().orElseThrow(() -> CadProblem.notFound("A format that reads '" + format + "'"));
    }

    private DocumentExport exportOf(String format) {
        return extensions.contributions(DocumentExport.POINT).stream().filter(writing -> writing.id().equals(format))
                .findFirst().orElseThrow(() -> CadProblem.notFound("A format that writes '" + format + "'"));
    }

    /** The components the reader may see: the catalogue's, and the owner's once the reader may read there. */
    private StoredDocuments scope(DocumentOwner owner) {
        if (owner == null) {
            return catalogue.scope(null);
        }
        authority.require(CadCapability.OWNER_READ, places.requireOwner(owner));
        return catalogue.scope(owner);
    }

    private ReadDocument readDocument(DocumentNode document) {
        return new ReadDocument(document.slug(), document.kind().word(), language.language().write(document));
    }

    private static List<DocumentNode> allOf(ImportedDocuments made) {
        List<DocumentNode> all = new ArrayList<>(made.library());
        all.addAll(made.designs());
        return all;
    }

    private DocumentNode read(String source) {
        if (source == null || source.isBlank()) {
            throw CadProblem.invalid("A document is its jM text: send it as 'source'.");
        }
        try {
            return language.language().readOne(DocumentInput.text("the document", source));
        } catch (GrammarRefusal refused) {
            throw CadProblem.invalidDocument(refused.detail(), refused.line(), refused.column());
        }
    }

    /** A conversion, its refusals as the problems every route answers with. */
    private static <T> T converted(Supplier<T> conversion) {
        try {
            return conversion.get();
        } catch (ExchangeRefusal refused) {
            throw CadProblem.invalidDocument(refused.detail(), refused.line(), refused.column());
        } catch (GrammarRefusal refused) {
            throw CadProblem.invalidDocument(refused.detail(), refused.line(), refused.column());
        } catch (IllegalArgumentException | IllegalStateException refused) {
            throw new CadProblem.Unprocessable("conversion-refused", refused.getMessage());
        }
    }
}
