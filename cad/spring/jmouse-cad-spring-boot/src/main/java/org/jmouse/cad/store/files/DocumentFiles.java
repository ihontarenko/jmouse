package org.jmouse.cad.store.files;

import org.jmouse.cad.document.source.DocumentResources;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

/**
 * 📎 The files a document holds besides its text — a 3D model's geometry, a part's datasheet —: files of the library
 * folder beside the document's own, in {@code <slug>.files/}, each change a commit by whoever made it (ADR 0002). The
 * document names its files by name; this finds them.
 */
public class DocumentFiles {

    /** How large one file a document holds may be, read whole: a part's 3D model, not an assembly. */
    public static final long LARGEST_FILE = 64L * 1024 * 1024;

    /** A file a document holds: its name, and how large it is. */
    public record HeldFile(String name, long size) {
    }

    private final DocumentStore documents;
    private final CadAuthority  authority;

    public DocumentFiles(DocumentStore documents, CadAuthority authority) {
        this.documents = documents;
        this.authority = authority;
    }

    /** A file kept for a document, under the name the document gives it; one larger than {@link #LARGEST_FILE} is refused. */
    public void store(String documentId, String name, Resource content) {
        try (InputStream stream = content.getInputStream()) {
            byte[] bytes = stream.readNBytes((int) LARGEST_FILE + 1);
            if (bytes.length > LARGEST_FILE) {
                throw new IllegalArgumentException("The file " + name + " is larger than a document's file may be ("
                        + LARGEST_FILE / (1024 * 1024) + " MB)");
            }
            documents.keepFile(documentId, name, bytes, authority.actor().orElse(null));
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The file " + name + " could not be read to be kept", unreadable);
        }
    }

    /**
     * One of a document's files by the name the document gives it, read whole — a file larger than {@code maximumBytes} is
     * refused, never cut.
     *
     * @return empty when the document holds no file of that name
     */
    public Optional<byte[]> read(String documentId, String name, long maximumBytes) {
        return documents.readFile(documentId, name).map(bytes -> {
            if (bytes.length > maximumBytes) {
                throw new IllegalArgumentException("The file " + name + " is larger than " + maximumBytes + " bytes");
            }
            return bytes;
        });
    }

    /** A document's files as the source layer hands them on: each read whole, one larger than {@link #LARGEST_FILE} refused. */
    public DocumentResources of(String documentId, String slug) {
        return name -> new ByteArrayInputStream(read(documentId, name, LARGEST_FILE).orElseThrow(() -> new FileNotFoundException(
                "The document " + slug + " names the file " + name + ", which it does not hold")));
    }

    /**
     * Takes one of a document's files away.
     *
     * @return whether it held one of that name
     */
    public boolean remove(String documentId, String name) {
        if (!documents.fileNames(documentId).contains(name)) {
            return false;
        }
        documents.keepFile(documentId, name, null, authority.actor().orElse(null));
        return true;
    }

    /** The files a document holds, in order. */
    public List<HeldFile> of(String documentId) {
        return documents.fileNames(documentId).stream()
                .map(name -> new HeldFile(name, documents.readFile(documentId, name).map(bytes -> (long) bytes.length).orElse(0L)))
                .toList();
    }
}
