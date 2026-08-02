package org.jmouse.storage.support;

import org.jmouse.http.Range;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.exception.StorageException;

/**
 * ✂️ Turns a requested {@link Range} into concrete byte positions against a known object length.
 *
 * <p>Shared by every backend so that "what does this range actually mean" is answered once. A range
 * is expressed relative to a length the requester does not know — {@code -500} is the last five
 * hundred bytes, {@code 500-} is everything after the five hundredth — so resolving it needs the
 * object, and refusing an impossible one needs the same arithmetic. Two backends resolving it
 * separately is two chances to disagree about an edge, and two error messages that drift apart.</p>
 */
public final class ByteRanges {

    private ByteRanges() {
    }

    /**
     * 📐 Resolve a range against an object's length.
     *
     * @param range      the requested range
     * @param key        key of the object, for the error message
     * @param sizeBytes  length of the object the range applies to
     * @return the resolved positions
     * @throws StorageException when the object's length cannot satisfy the range
     */
    public static ByteRange resolve(Range range, StorageKey key, long sizeBytes) {
        long start = range.getStart(sizeBytes);
        long end   = Math.min(range.getEnd(sizeBytes), sizeBytes - 1);

        if (start < 0 || start >= sizeBytes || end < start) {
            throw new StorageException("Range %s cannot be satisfied by '%s' of %d bytes"
                                               .formatted(range.toHeaderValue(), key, sizeBytes));
        }

        return new ByteRange(start, end, sizeBytes);
    }

    /**
     * 📏 A range resolved against a real object: both ends inclusive, and the length they were
     * resolved against.
     *
     * @param start     first byte of the range, inclusive
     * @param end       last byte of the range, inclusive
     * @param sizeBytes length of the whole object, not of the range
     */
    public record ByteRange(long start, long end, long sizeBytes) {

        /**
         * 📊 How many bytes the range covers.
         *
         * @return the range's length in bytes
         */
        public long length() {
            return end - start + 1;
        }
    }
}
