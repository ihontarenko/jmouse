package org.jmouse.files.management.access;

import org.jmouse.files.jpa.ManagedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Set;

/**
 * 🔒 Which of the files a listing returns this caller may actually read.
 *
 * <p>The twin of {@link DirectoryVisibility}, one level down. A folder listing is guarded at the
 * folder — may you read what is filed here — and until this class it then returned <em>every</em> file
 * filed there. So a product that closed ONE file with a rule of its own had the rule honoured when
 * somebody opened that file and ignored by the listing: the name, the size and the uploader of a file
 * somebody was refused were shown to them.
 *
 * <h2>⚠️ ASKED OF THE PRODUCT, ONCE PER LISTING — not of the engine once per file</h2>
 *
 * <p>The first version asked the access engine about every file, the route guard's own question
 * through the product's resolver. It was correct and it cost about fifteen milliseconds a file,
 * measured on a folder of fifty-six: a thousand photographs would have listed in fifteen seconds, in
 * every product mounting this module. So the question moved to {@link FileRefusals}, which a product
 * answers for the whole listing — and which knows, as this library cannot, that only the few files
 * something names on its own can be answered differently from their folder.
 *
 * <p>⚠️ WHERE NO PRODUCT ANSWERS, NOTHING IS FILTERED — what the listing always did. Said once in the
 * log rather than silently, because a filter that quietly does nothing is how
 * {@link DirectoryVisibility} came to exist.
 */
public class FileVisibility {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileVisibility.class);

    private final ObjectProvider<FileRefusals> refusals;

    /** ⚠️ Said once, not per request — a listing runs on every screen that opens a folder. */
    private volatile boolean announced;

    /** @param refusals the product's answer, if it gives one */
    public FileVisibility(ObjectProvider<FileRefusals> refusals) {
        this.refusals = refusals;
    }

    /**
     * 🌿 The files this caller may read, in the order they came.
     *
     * <p>⚠️ A file the caller may not read is <strong>absent</strong>, never marked — a greyed row
     * tells somebody exactly what a hidden one does not.
     *
     * @param found what the folder holds
     * @return the readable subset, or all of it where the product gives no answer
     */
    public List<ManagedFile> readable(List<ManagedFile> found) {
        if (found == null || found.isEmpty()) {
            return found;
        }

        FileRefusals answering = refusals.getIfAvailable();

        if (!announced) {
            announced = true;
            LOGGER.info(answering == null
                        ? "File listings are not filtered per file: no FileRefusals bean. A folder's rules still decide the listing."
                        : "File listings are filtered per file by {}.",
                        answering == null ? null : answering.getClass().getSimpleName());
        }

        if (answering == null) {
            return found;
        }

        Set<String> refused = answering.refusedAmong(found.stream().map(ManagedFile::getId).toList());

        if (refused.isEmpty()) {
            return found;
        }

        return found.stream()
                .filter(file -> !refused.contains(file.getId()))
                .toList();
    }
}
