package org.jmouse.storage.support;

/**
 * 📊 What one digesting copy established about the bytes.
 *
 * <p>Both facts come from the same pass, which is the point: establishing either one afterwards
 * would mean reading the content a second time, and for content arriving over a network there may
 * not be a second time.</p>
 *
 * @param sizeBytes number of bytes that actually arrived, not the number that was claimed
 * @param sha256    lower-case hex SHA-256 of those bytes
 */
public record Digested(long sizeBytes, String sha256) {
}
