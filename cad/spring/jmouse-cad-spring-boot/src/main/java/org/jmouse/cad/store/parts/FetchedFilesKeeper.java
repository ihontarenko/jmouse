package org.jmouse.cad.store.parts;

import org.jmouse.cad.document.part.FetchedFile;
import org.jmouse.cad.store.files.DocumentFiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The files of fetched or imported documents stored beside them: only of the documents named, and only those a document does
 * not hold yet, so that an earlier fetch where no files were kept is completed and never written over.
 */
public final class FetchedFilesKeeper {

    private final DocumentFiles files;

    /** @param files where a document's files are kept */
    public FetchedFilesKeeper(DocumentFiles files) {
        this.files = files;
    }

    /**
     * @param filed each document that may take files, its id by its slug
     * @return what to say of them: that they were not kept; empty when all is well
     */
    public Optional<String> keep(List<FetchedFile> made, Map<String, String> filed) {
        List<FetchedFile> taken = made.stream().filter(file -> filed.containsKey(file.document())).toList();
        if (taken.isEmpty()) {
            return Optional.empty();
        }
        taken.stream().filter(file -> !holds(filed.get(file.document()), file.name()))
                .forEach(file -> files.store(filed.get(file.document()), file.name(), file.content()));
        return Optional.empty();
    }

    private boolean holds(String documentId, String name) {
        return files.of(documentId).stream().anyMatch(file -> name.equals(file.name()));
    }
}
