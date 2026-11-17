package org.jmouse.cad.folder;

import java.time.Instant;

/**
 * 📜 One saved revision of a document's text, without the text.
 *
 * @param revision  the revision number, from 1: the key
 * @param checksum  the text's checksum
 * @param writtenAt when it was saved
 * @param writtenBy who saved it; {@code null} for the shipped library writing its own file
 * @param origin    where it came from: a person, or an import
 */
public record DocumentRevision(int revision, String checksum, Instant writtenAt, String writtenBy, RevisionOrigin origin) {
}
