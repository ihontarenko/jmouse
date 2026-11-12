package org.jmouse.cad.plugin.datasheets;

import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.source.ChangeableResources;
import org.jmouse.cad.document.source.DocumentLocation;
import org.jmouse.cad.document.source.DocumentResources;
import org.jmouse.cad.document.source.HeldDocument;
import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.catalogue.StoredDocuments;
import org.jmouse.cad.store.files.DocumentFiles;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.core.io.ByteArrayResource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * 📄 A part's datasheets: the PDFs kept beside its document, filed against it as any file a document holds, and where its
 * text says one is on the web. Keeping one changes nothing in the text, so a part the installation ships, read only, can
 * have its datasheet without being copied: what is kept beside it is the installation's own.
 */
public class Datasheets {

    /** What may be done with a document's datasheets, by the one who asks. */
    public enum Keeping {
        /** They may be read, added and taken away. */
        CHANGEABLE,
        /** They may only be read. */
        READ_ONLY
    }

    /** One kept beside the document. */
    public record KeptDatasheet(String name, long size) {
    }

    /** All there is of a document's datasheets, as its reader may have them. */
    public record DatasheetShelf(Keeping keeping, List<KeptDatasheet> files, List<String> links) {
    }

    /** A datasheet's bytes. */
    public record OpenedDatasheet(String name, String mediaType, byte[] content) {
    }

    private final CadCatalogue    catalogue;
    private final CadLanguage     language;
    private final DocumentAccess  access;
    private final DocumentFiles   files;
    private final DatasheetFormat format;
    private final LongSupplier    largest;
    private final CadAuthority    authority;
    private final CadPlaces       places;

    /**
     * @param files   the files documents hold; null where the installation keeps none
     * @param largest how large one may be, read at each upload
     */
    public Datasheets(CadCatalogue catalogue, CadLanguage language, DocumentAccess access, DocumentFiles files,
                      DatasheetFormat format, LongSupplier largest, CadAuthority authority, CadPlaces places) {
        this.authority = authority;
        this.places = places;
        this.largest = largest;
        this.catalogue = catalogue;
        this.language = language;
        this.access = access;
        this.files = files;
        this.format = format;
    }

    public DatasheetShelf shelf(String address, DocumentOwner owner) {
        Optional<FolderPart> beside = folderPart(address, owner);
        if (beside.isPresent()) {
            return beside.get().shelf();
        }
        StoredDocument document = access.requireReadable(address);
        return new DatasheetShelf(keepingOf(document), keptOf(document), linksOf(address, document));
    }

    public OpenedDatasheet open(String address, DocumentOwner owner, String name) {
        format.requireName(name);
        Optional<FolderPart> beside = folderPart(address, owner);
        if (beside.isPresent()) {
            return beside.get().open(name);
        }
        StoredDocument document = access.requireReadable(address);
        byte[] content = files.read(document.getId(), name, DocumentFiles.LARGEST_FILE)
                .orElseThrow(() -> CadProblem.notFound("The datasheet"));
        return new OpenedDatasheet(name, format.mediaType(), content);
    }

    /** Keeps a datasheet beside the document; a name it already holds is refused, never written over. */
    public DatasheetShelf keep(String address, DocumentOwner owner, String name, InputStream body) {
        format.requireName(name);
        byte[] content = boundedContent(name, body);
        format.requireContent(name, content);
        Optional<FolderPart> beside = folderPart(address, owner);
        if (beside.isPresent()) {
            beside.get().keep(name, content);
            return shelf(address, owner);
        }
        StoredDocument document = access.requireReadable(address);
        access.requireFilingBeside(document);
        if (files.of(document.getId()).stream().anyMatch(file -> name.equals(file.name()))) {
            throw CadProblem.conflict(document.getSlug() + " already holds " + name + ": take that one away first.");
        }
        files.store(document.getId(), name, new ByteArrayResource(content));
        return shelf(address, owner);
    }

    public DatasheetShelf remove(String address, DocumentOwner owner, String name) {
        format.requireName(name);
        Optional<FolderPart> beside = folderPart(address, owner);
        if (beside.isPresent()) {
            beside.get().remove(name);
            return shelf(address, owner);
        }
        StoredDocument document = access.requireReadable(address);
        access.requireFilingBeside(document);
        if (!files.remove(document.getId(), name)) {
            throw CadProblem.notFound("The datasheet");
        }
        return shelf(address, owner);
    }

    /** The part a place holds in a folder of its, when the address names one there: for a reader who may read that place. */
    private Optional<FolderPart> folderPart(String address, DocumentOwner owner) {
        if (owner == null) {
            return Optional.empty();
        }
        CadPlace place = places.requireOwner(owner);
        authority.require(CadCapability.OWNER_READ, place);
        StoredDocuments scope = catalogue.scope(owner);
        return scope.locate(address).filter(held -> held.location() instanceof DocumentLocation.Mounted)
                .map(held -> new FolderPart(held, scope.resources(held), authority.permits(CadCapability.OWNER_WRITE, place)));
    }

    /**
     * A part kept as a file of a folder, and the PDFs named after it beside it: {@code tl072.component.jm}, {@code tl072.pdf},
     * {@code tl072 rev F.pdf}. A folder of many parts has many PDFs: only its own are this part's.
     */
    private final class FolderPart {

        private final HeldDocument      held;
        private final DocumentResources resources;
        private final boolean           mayChange;
        private final String            stem;

        FolderPart(HeldDocument held, DocumentResources resources, boolean mayChange) {
            this.held = held;
            this.resources = resources;
            this.mayChange = mayChange;
            String key = ((DocumentLocation.Mounted) held.location()).key();
            String file = key.substring(key.lastIndexOf('/') + 1);
            int dot = file.indexOf('.');
            this.stem = dot < 0 ? file : file.substring(0, dot);
        }

        DatasheetShelf shelf() {
            List<KeptDatasheet> kept = new ArrayList<>();
            for (String name : resources.names()) {
                if (format.names(name) && name.startsWith(stem)) {
                    kept.add(new KeptDatasheet(name, sizeOf(name)));
                }
            }
            List<String> links = held.document().block(MetaNode.class).map(MetaNode::links).orElse(List.of()).stream()
                    .filter(link -> format.linkLabel().equalsIgnoreCase(link.label()))
                    .map(MetaNode.Link::address).filter(Datasheets::isWebAddress).toList();
            boolean changeable = mayChange && resources instanceof ChangeableResources;
            return new DatasheetShelf(changeable ? Keeping.CHANGEABLE : Keeping.READ_ONLY, List.copyOf(kept), links);
        }

        private long sizeOf(String name) {
            try (InputStream body = resources.open(name)) {
                return body.transferTo(OutputStream.nullOutputStream());
            } catch (IOException failed) {
                return 0;
            }
        }

        OpenedDatasheet open(String name) {
            if (!name.startsWith(stem)) {
                throw CadProblem.notFound("The datasheet");
            }
            try (InputStream body = resources.open(name)) {
                return new OpenedDatasheet(name, format.mediaType(), body.readAllBytes());
            } catch (FileNotFoundException missing) {
                throw CadProblem.notFound("The datasheet");
            } catch (IOException failed) {
                throw new UncheckedIOException(failed);
            }
        }

        /** Kept under a name that says whose it is: a file called otherwise would be no part's. */
        void keep(String name, byte[] content) {
            String kept = name.startsWith(stem) ? name : stem + " " + name;
            try {
                requireChangeable().keep(kept, content);
            } catch (FileAlreadyExistsException taken) {
                throw CadProblem.conflict("The folder already holds " + kept + ": take that one away first.");
            } catch (IOException failed) {
                throw new UncheckedIOException(failed);
            }
        }

        void remove(String name) {
            try {
                if (!name.startsWith(stem) || !requireChangeable().remove(name)) {
                    throw CadProblem.notFound("The datasheet");
                }
            } catch (IOException failed) {
                throw new UncheckedIOException(failed);
            }
        }

        private ChangeableResources requireChangeable() {
            if (!mayChange || !(resources instanceof ChangeableResources changeable)) {
                throw CadProblem.conflict("This folder is not yours to change here.");
            }
            return changeable;
        }
    }

    private Keeping keepingOf(StoredDocument document) {
        return access.canFileBeside(document) ? Keeping.CHANGEABLE : Keeping.READ_ONLY;
    }

    private List<KeptDatasheet> keptOf(StoredDocument document) {
        return files.of(document.getId()).stream().filter(file -> format.names(file.name()))
                .map(file -> new KeptDatasheet(file.name(), file.size())).toList();
    }

    /** Where its text says its datasheet is on the web; anything but a web address is no link a page should offer. */
    private List<String> linksOf(String address, StoredDocument document) {
        String source = catalogue.detail(address).source();
        DocumentNode read = language.language().readOne(DocumentInput.text(document.getSlug(), source));
        return read.block(MetaNode.class).map(MetaNode::links).orElse(List.of()).stream()
                .filter(link -> format.linkLabel().equalsIgnoreCase(link.label()))
                .map(MetaNode.Link::address).filter(Datasheets::isWebAddress).toList();
    }

    private static boolean isWebAddress(String address) {
        try {
            String scheme = new URI(address).getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (URISyntaxException unreadable) {
            return false;
        }
    }

    /** Read whole, and one byte past what a document may hold: a body that says nothing of its length is still bounded. */
    private byte[] boundedContent(String name, InputStream body) {
        long most = Math.min(largest.getAsLong(), DocumentFiles.LARGEST_FILE);
        try {
            byte[] content = body.readNBytes(Math.toIntExact(most) + 1);
            if (content.length > most) {
                throw CadProblem.invalid(name + " is larger than a datasheet may be: " + most + " bytes.");
            }
            return content;
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
