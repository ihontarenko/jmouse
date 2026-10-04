package org.jmouse.files.management.access;

import java.util.Collection;
import java.util.Set;

/**
 * 🔒 Which of these files the caller may not read, beyond what their folder already decided.
 *
 * <p>The product's answer, because only the product knows how a single file can be closed — a
 * per-file place in its access vocabulary, a flag it interprets, nothing at all. A folder listing is
 * guarded at the folder; this is asked about the files the listing is about to return, and the ones it
 * names are left out.
 *
 * <h2>⚠️ ONE QUESTION FOR THE WHOLE LISTING, and that is the contract</h2>
 *
 * <p>The obvious implementation — ask the access engine about every file — was built first and
 * measured at about fifteen milliseconds a file: a folder of a thousand photographs took fifteen
 * seconds to list. A file placed only by its folder cannot be answered differently from its folder, so
 * an implementation should find the few files something names on its own (usually none) in one query
 * and decide only those.
 *
 * <p>⚠️ No bean, no per-file filtering — what the listing did before this existed.
 */
public interface FileRefusals {

    /**
     * @param fileIds the files a listing is about to return
     * @return the ones this caller may not read — a subset of {@code fileIds}, usually empty
     */
    Set<String> refusedAmong(Collection<String> fileIds);
}
