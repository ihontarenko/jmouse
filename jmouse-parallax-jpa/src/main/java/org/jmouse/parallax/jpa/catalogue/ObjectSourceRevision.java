package org.jmouse.parallax.jpa.catalogue;

import java.time.Instant;

/**
 * 📜 One saved revision of an object's text, without the text.
 *
 * @param revision  the revision number, from 1
 * @param checksum  the text's checksum
 * @param writtenAt when it was saved
 * @param writtenBy who saved it; {@code null} for the shipped catalogue writing its own file
 */
public record ObjectSourceRevision(int revision, String checksum, Instant writtenAt, String writtenBy) {
}
