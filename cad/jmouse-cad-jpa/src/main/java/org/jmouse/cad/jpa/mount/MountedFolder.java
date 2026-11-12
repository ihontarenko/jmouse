package org.jmouse.cad.jpa.mount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.source.DocumentLocation;

import java.time.Instant;

/**
 * 📁 A folder of the server a curator mounted: where it is on disk, how it is named, what it lets be done, and whose it
 * is. A folder the server's configuration mounts is not one of these.
 */
@Entity(name = "MountedFolder")
@Table(name = "cad_mounted_folders")
public class MountedFolder {

    @Id
    @Column(length = 80, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "folder_path", nullable = false, length = 1024, updatable = false)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DocumentLocation.Access access;

    @Column(name = "owner_type", length = 40, updatable = false)
    private String ownerType;

    @Column(name = "owner_id", length = 80, updatable = false)
    private String ownerId;

    @Column(name = "mounted_at", nullable = false, updatable = false)
    private Instant mountedAt;

    protected MountedFolder() {
    }

    /** @param owner whose it is; null for the installation's */
    public MountedFolder(String id, String title, String path, DocumentLocation.Access access, DocumentOwner owner,
                         Instant mountedAt) {
        this.id = id;
        this.title = title;
        this.path = path;
        this.access = access;
        this.ownerType = owner == null ? null : owner.type();
        this.ownerId = owner == null ? null : owner.id();
        this.mountedAt = mountedAt;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public DocumentLocation.Access getAccess() {
        return access;
    }

    /** Whose it is; null for the installation's. */
    public DocumentOwner getOwner() {
        return ownerType == null ? null : new DocumentOwner(ownerType, ownerId);
    }

    public Instant getMountedAt() {
        return mountedAt;
    }
}
