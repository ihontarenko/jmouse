package org.jmouse.files.management;

/**
 * 🌐 Fetch this address and keep what comes back.
 *
 * @param url the address to fetch, scheme included
 */
public record ImportFileRequest(String url) {
}
