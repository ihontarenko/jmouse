package org.jmouse.files.jpa;

import java.io.Serializable;
import java.util.Objects;

/**
 * 🔑 What identifies a binding: the file, and the thing it is filed against.
 *
 * <p>⚠️ The key is the whole row, so there is no surrogate identifier and nothing to hand out. A
 * binding is not a thing anybody names — it is the statement that these two are connected, and
 * removing it is addressed by naming both ends.</p>
 */
public class FileBindingId implements Serializable {

    private String fileId;
    private String ownerType;
    private String ownerId;

    protected FileBindingId() {
    }

    /**
     * 🏗️ Address one binding.
     *
     * @param fileId    the file
     * @param ownerType what kind of thing holds it
     * @param ownerId   which one
     */
    public FileBindingId(String fileId, String ownerType, String ownerId) {
        this.fileId    = fileId;
        this.ownerType = ownerType;
        this.ownerId   = ownerId;
    }

    public String getFileId() {
        return fileId;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public String getOwnerId() {
        return ownerId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FileBindingId key
               && Objects.equals(fileId, key.fileId)
               && Objects.equals(ownerType, key.ownerType)
               && Objects.equals(ownerId, key.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, ownerType, ownerId);
    }
}
