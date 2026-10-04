package org.jmouse.files.management.access;

import org.jmouse.files.jpa.directory.StorageDirectory;

import java.util.Collection;
import java.util.Set;

/**
 * 🔒 Which of these folders the caller may not read — answered by the product, once per listing.
 *
 * <p>The folder twin of {@link FileRefusals}. {@link DirectoryVisibility} otherwise asks the access
 * engine about every folder a listing returns, and that was measured at about five milliseconds a
 * folder: 176 folders took a second, and a household with a folder per film would wait ten.
 *
 * <p>A product can do far better because it knows its vocabulary: a folder no rule names — not by
 * itself, not through an ancestor, not by its path, and with no owner of its own — is decided exactly
 * like every other such folder, so one decision covers all of them and only the few named ones need
 * one each.
 *
 * <p>⚠️ No bean, no change: the listing asks the engine per folder, as it always has.
 */
public interface DirectoryRefusals {

    /**
     * @param directories the folders a listing is about to return
     * @return the identifiers of the ones this caller may not read — a subset, usually empty
     */
    Set<String> refusedAmong(Collection<StorageDirectory> directories);
}
