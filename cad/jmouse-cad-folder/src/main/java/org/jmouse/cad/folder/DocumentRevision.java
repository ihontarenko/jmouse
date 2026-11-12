package org.jmouse.cad.folder;

import org.jmouse.revisions.RevisionLabel;

import java.time.Instant;

/**
 * 📜 One saved revision of a document's text, without the text.
 *
 * @param revision  the revision number, from 1: the key
 * @param label     the name a person says it by: {@code 20261023_001}
 * @param checksum  the text's checksum
 * @param writtenAt when it was saved
 * @param writtenBy who saved it; {@code null} for the shipped library writing its own file
 * @param origin    where it came from: a person, or an import
 */
public record DocumentRevision(int revision, RevisionLabel label, String checksum, Instant writtenAt, String writtenBy, RevisionOrigin origin) {
}
