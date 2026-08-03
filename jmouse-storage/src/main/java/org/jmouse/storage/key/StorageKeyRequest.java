package org.jmouse.storage.key;

import org.jmouse.storage.Content;
import org.jmouse.storage.exception.StorageKeyException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 📐 What a caller knows about content, expressed so a {@link StorageKeyStrategy} can decide where
 * it goes.
 *
 * <p>Built rather than constructed positionally on purpose. The mechanism this replaces offered
 * {@code store(stream, a, b)} and {@code store(stream, a, b, c)} where every parameter was a
 * {@code String}, and reading a call site told you nothing about what {@code a} and {@code b}
 * were — which is how two products ended up disagreeing about it.</p>
 *
 * @param namespace       logical content class, e.g. {@code documents/md}; may be blank
 * @param ownerIdentifier who the content belongs to (never blank)
 * @param segments        further path segments, e.g. a category slug; blanks are dropped
 * @param filenameBase    filename without extension; blank means "generate one"
 * @param extension       extension without its dot; blank means "no extension"
 * @param contentDigest   lower-case hex SHA-256 of the bytes, for layouts that place content by
 *                        what it is rather than by whose it is; {@code null} otherwise
 */
public record StorageKeyRequest(String namespace, String ownerIdentifier, List<String> segments,
                                String filenameBase, String extension, String contentDigest) {

    /**
     * 🏗️ Normalise the collection and enforce the one field a layout cannot do without.
     *
     * <p>Blank and {@code null} segments are dropped here rather than by each strategy, so every
     * strategy receives a segment list it can simply join.</p>
     */
    public StorageKeyRequest {
        if (ownerIdentifier == null || ownerIdentifier.isBlank()) {
            throw new StorageKeyException("Storage key request must carry an owner identifier.");
        }

        segments = (segments == null) ? List.of() : segments.stream()
                .filter(segment -> segment != null && !segment.isBlank())
                .toList();
    }

    /**
     * 🔐 This request with the content's digest filled in.
     *
     * <p>What a caller does once it has spooled the bytes: the request is assembled before the
     * content is read, and a content-addressed layout only becomes composable afterwards.</p>
     *
     * @param contentDigest lower-case hex SHA-256
     * @return a copy carrying the digest
     */
    public StorageKeyRequest withContentDigest(String contentDigest) {
        return new StorageKeyRequest(namespace, ownerIdentifier, segments, filenameBase, extension,
                                     contentDigest);
    }

    /**
     * 🧱 Start describing content belonging to an owner.
     *
     * @param ownerIdentifier who the content belongs to
     * @return a builder
     */
    public static Builder forOwner(String ownerIdentifier) {
        return new Builder(ownerIdentifier);
    }

    /**
     * 🧱 Start describing a piece of {@link Content}, taking its extension from its filename.
     *
     * @param ownerIdentifier who the content belongs to
     * @param content         the content being ingested
     * @return a builder pre-filled with the content's extension
     */
    public static Builder forContent(String ownerIdentifier, Content content) {
        return new Builder(ownerIdentifier).extension(content.extension());
    }

    /**
     * 🧱 Assembles a {@link StorageKeyRequest} one named part at a time.
     */
    public static final class Builder {

        private final String       ownerIdentifier;
        private final List<String> segments = new ArrayList<>();
        private       String       namespace;
        private       String       filenameBase;
        private       String       extension;
        private       String       contentDigest;

        private Builder(String ownerIdentifier) {
            this.ownerIdentifier = ownerIdentifier;
        }

        /**
         * 🗂️ Set the logical content class.
         *
         * @param namespace e.g. {@code documents/md}
         * @return this builder
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * ➕ Append further path segments, in order. Blank and {@code null} entries are dropped.
         *
         * @param segments segments to append
         * @return this builder
         */
        public Builder segments(String... segments) {
            if (segments != null) {
                this.segments.addAll(Arrays.asList(segments));
            }

            return this;
        }

        /**
         * 🏷️ Set the filename without its extension. Leave unset to have one generated.
         *
         * @param filenameBase stable, human-meaningful filename base
         * @return this builder
         */
        public Builder filenameBase(String filenameBase) {
            this.filenameBase = filenameBase;
            return this;
        }

        /**
         * 📄 Set the extension, without its dot.
         *
         * @param extension e.g. {@code md}
         * @return this builder
         */
        public Builder extension(String extension) {
            this.extension = extension;
            return this;
        }

        /**
         * 🔐 Set the digest of the bytes, for a layout that places content by what it is.
         *
         * @param contentDigest lower-case hex SHA-256
         * @return this builder
         */
        public Builder contentDigest(String contentDigest) {
            this.contentDigest = contentDigest;
            return this;
        }

        /**
         * ✅ Produce the request.
         *
         * @return the assembled request
         */
        public StorageKeyRequest build() {
            return new StorageKeyRequest(namespace, ownerIdentifier, segments, filenameBase, extension,
                                         contentDigest);
        }
    }
}
