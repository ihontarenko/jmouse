package org.jmouse.files.management;

/**
 * 🙈 Whether a file should be listed and served only to whoever may already reach it.
 *
 * <p>A body rather than a query parameter, and a record rather than a bare boolean, because this is the
 * shape a second field would arrive in — and a route whose contract changes from scalar to object the
 * first time it grows one is a route every client has to be told about twice.</p>
 *
 * @param isPrivate whether it should be private
 */
public record PrivacyRequest(boolean isPrivate) {
}
