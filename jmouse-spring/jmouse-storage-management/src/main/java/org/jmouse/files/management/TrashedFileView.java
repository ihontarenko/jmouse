package org.jmouse.files.management;

import org.jmouse.files.OwnerReference;
import org.jmouse.files.jpa.ManagedFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 🗑️ One file in the trash, as the trash screen renders it.
 *
 * <p>⚠️ <strong>Its own record rather than three more components on {@link FileView}.</strong> A file
 * outside the trash has no "trashed at", and a view carrying always-null fields on every ordinary listing
 * is a view that lies about what it describes — besides changing a public constructor every product
 * compiles against.</p>
 *
 * @param file      the file itself
 * @param trashedAt when it went in
 * @param trashedBy who put it there, or {@code null}
 * @param owners    where it is still filed — and where restoring puts it back
 */
public record TrashedFileView(FileView file, LocalDateTime trashedAt, String trashedBy,
                              List<OwnerReference> owners) {

    /**
     * 🏗️ Describe a trashed file.
     *
     * @param file   the file
     * @param owners where it is filed
     * @return the view
     */
    public static TrashedFileView of(ManagedFile file, List<OwnerReference> owners) {
        return new TrashedFileView(FileView.of(file), file.getTrashedAt(), file.getTrashedBy(), owners);
    }
}
