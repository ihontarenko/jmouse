package org.jmouse.core.io;

/**
 * 📦 Represents a segment (subsection) of a {@link Resource}.
 *
 * <p>Typically used for serving partial content responses
 * in HTTP range requests (RFC 7233).</p>
 */
public class ResourceSegment {

    /**
     * 📍 Starting byte position within the resource.
     */
    private final long position;

    /**
     * 🎯 Target resource to read from.
     */
    private final Resource resource;

    /**
     * 📏 Total number of bytes in this segment.
     */
    private final long total;

    /**
     * 🏗️ Create a new resource segment.
     *
     * @param position starting position (0-based)
     * @param resource underlying resource
     * @param total    length of this segment in bytes
     */
    public ResourceSegment(long position, Resource resource, long total) {
        this.position = position;
        this.resource = resource;
        this.total = total;
    }

    /**
     * 🏷️ Factory method for creating a new segment.
     *
     * @param position starting position (0-based)
     * @param resource underlying resource
     * @param total    length of this segment in bytes
     * @return new {@link ResourceSegment}
     */
    public static ResourceSegment ofRange(long position, Resource resource, long total) {
        return new ResourceSegment(position, resource, total);
    }

    /**
     * @return starting position of this segment
     */
    public long getPosition() {
        return position;
    }

    /**
     * @return the underlying resource
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * @return total number of bytes in this segment
     */
    public long getTotal() {
        return total;
    }
}
