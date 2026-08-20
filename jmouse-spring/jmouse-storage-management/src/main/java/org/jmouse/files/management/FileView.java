package org.jmouse.files.management;

import org.jmouse.files.jpa.ManagedFile;

import java.time.LocalDateTime;

/**
 * 📄 A file as a screen draws it.
 *
 * <p>Flat on purpose. Every product's file list renders the same six things, and nesting the stored
 * object inside would leak a registry identifier into an interface that has no use for one — while
 * making every client learn a second shape to read a size.</p>
 *
 * @param id          the file's identifier, which every other route takes
 * @param name        what to call it here, which is the binding's name and not the registry's
 * @param contentType what the bytes are
 * @param sizeBytes   how large they are
 * @param uploadedBy  who put it there, or {@code null} where nobody was signed in
 * @param createdAt   when
 */
public record FileView(String id, String name, String contentType, long sizeBytes,
                       String uploadedBy, LocalDateTime createdAt) {

    /**
     * 🏗️ Describe a file.
     *
     * @param file the file
     * @return the view
     */
    public static FileView of(ManagedFile file) {
        return new FileView(
            file.getId(), file.getDisplayName(),
            file.getStoredFile().getContentType().toString(), file.getStoredFile().getSizeBytes(),
            file.getUploadedBy(), file.getCreatedAt());
    }
}
