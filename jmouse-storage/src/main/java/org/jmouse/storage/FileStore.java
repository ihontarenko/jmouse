package org.jmouse.storage;

import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceSegment;
import org.jmouse.http.Range;
import org.jmouse.storage.exception.ObjectNotFoundException;
import org.jmouse.storage.exception.StorageException;

import java.util.Optional;

/**
 * 🗄️ The byte layer: how content is written, read, described and removed.
 *
 * <p>One method per question, each taking an explicit {@link StorageKey} and — for writes — a
 * neutral {@link Content}, so a call site reads correctly without opening this interface. Deciding
 * <em>where</em> content goes is not an argument here; it is {@link org.jmouse.storage.key.StorageKeyStrategy}.</p>
 *
 * <p>Adding a backend means implementing this interface and registering one bean. There is no base
 * class to extend and nothing to override selectively.</p>
 *
 * <p>Backends are meant to be interchangeable: whatever a caller can do against local disk it can
 * do against an object store, with one deliberate exception — {@link #resolveDirectLink} is
 * optional and returns {@link Optional#empty()} when a backend cannot serve its own bytes.
 * <strong>Every caller must handle the empty case</strong> by streaming through {@link #read}
 * instead; local disk always returns empty, and any backend is free to decline.</p>
 */
public interface FileStore {

    /**
     * 🏷️ Stable identifier of this backend, recorded against every object it writes.
     *
     * <p>Knowing which backend wrote an object is what makes moving objects between backends a
     * feature to build later rather than a schema change to negotiate.</p>
     *
     * @return short lower-case backend name, e.g. {@code local}
     */
    String backendName();

    /**
     * 📝 Write content at an exact key, computing size and digest in the same pass as the bytes.
     *
     * <p>Idempotent at the same key: writing again replaces what was there. The returned size is
     * the number of bytes that actually arrived, never the number {@link Content#declaredSize()}
     * claimed.</p>
     *
     * @param key     where to write
     * @param content what to write
     * @return a receipt describing what was written
     * @throws StorageException on I/O failure
     */
    StoredObject write(StorageKey key, Content content);

    /**
     * 📖 Read a stored object.
     *
     * <p>Pulls the bytes through this application, which is exactly the hop
     * {@link #resolveDirectLink} avoids — prefer that when the backend offers it.</p>
     *
     * @param key key of the stored object
     * @return the object's bytes as a resource
     * @throws ObjectNotFoundException when nothing is stored under {@code key}
     */
    Resource read(StorageKey key);

    /**
     * ✂️ Read part of a stored object, for partial-content responses.
     *
     * @param key   key of the stored object
     * @param range requested byte range
     * @return the requested segment
     * @throws ObjectNotFoundException when nothing is stored under {@code key}
     * @throws StorageException        when the range cannot be satisfied by the object's length
     */
    ResourceSegment readRange(StorageKey key, Range range);

    /**
     * 📋 Report size and content type without transferring the bytes.
     *
     * <p>Size is exact. Content type is only as good as the key: a key ending in an extension
     * describes as the type {@link #write} recorded, but a key composed without one carries no
     * type at all, and a backend will fall back to {@link ContentTypes#DEFAULT} rather than invent
     * an answer. That is a property of extensionless layouts, not a defect — the authoritative
     * type of such an object is the one on its {@link StoredObject} write receipt, which belongs
     * in a registry rather than in the object's name.</p>
     *
     * @param key key of the stored object
     * @return what the backend knows about the object
     * @throws ObjectNotFoundException when nothing is stored under {@code key}
     */
    ObjectDescription describe(StorageKey key);

    /**
     * 🔗 Produce a link letting the client fetch the bytes straight from the backend.
     *
     * <p>Returns {@link Optional#empty()} when this backend cannot serve its own bytes. Callers
     * <strong>must</strong> handle that by streaming via {@link #read}.</p>
     *
     * @param key          key of the stored object
     * @param presentation headers the backend should respond with
     * @return the link, or empty when this backend declines
     */
    default Optional<DirectLink> resolveDirectLink(StorageKey key, Presentation presentation) {
        return Optional.empty();
    }

    /**
     * 🗑️ Permanently remove the stored bytes.
     *
     * <p>Removing an object that is already gone is a success. Implementations must log and return
     * rather than throw — a caller cleaning up after a failure should not have to guard against
     * the cleanup itself failing.</p>
     *
     * @param key key of the object to remove
     */
    void delete(StorageKey key);
}
