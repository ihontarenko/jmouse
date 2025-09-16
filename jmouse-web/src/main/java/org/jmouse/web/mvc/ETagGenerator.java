package org.jmouse.web.mvc;

import org.jmouse.core.IdGenerator;
import org.jmouse.core.Md5HashGenerator;
import org.jmouse.core.MediaTypeHelper;
import org.jmouse.web.http.request.ETag;

/**
 * ETag generator based on content length and last-modified timestamp.
 *
 * <p>Computes a hash over the pair "{@code length:lastModified}" and wraps it as
 * a weak or strong {@link ETag}. Note that a hash derived only from length and mtime
 * is typically suitable for <em>weak</em> ETags; for truly strong ETags prefer hashing
 * the actual representation bytes.</p>
 *
 * <p>Hash function defaults to MD5 via {@link Md5HashGenerator} (adequate for ETag use;
 * not for cryptographic security).</p>
 *
 * @see ETag
 * @see Md5HashGenerator
 */
public final class ETagGenerator implements IdGenerator<ETag, String> {

    /**
     * Whether generated tags should be marked as weak (prefixed with {@code W/}).
     * <p>See RFC 7232 §2.1 for weak vs strong validator semantics.</p>
     */
    private final boolean weak;

    /**
     * Hash function used to derive the tag from a "{@code length:lastModified}" seed.
     */
    private final IdGenerator<String, String> generator = new Md5HashGenerator();

    /**
     * @param weak whether to mark the resulting ETag as weak
     */
    public ETagGenerator(boolean weak) {
        this.weak = weak;
    }

    /**
     * Unsupported in this implementation.
     *
     * <p>Use {@link #generate(String)} and pass a seed produced by
     * {@link #seed(long, long)} or your own representation-dependent seed.</p>
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public ETag generate() {
        throw new UnsupportedOperationException("Use generate(String seed) instead");
    }

    /**
     * Generates an ETag by hashing a caller-provided seed.
     *
     * <p>Typical seed: {@link #seed(long, long)} — i.e., {@code "length:lastModified"}.
     * For strong validators, prefer hashing the actual bytes of the representation.</p>
     *
     * @param seed input string to hash (e.g., {@code "12345:1726450000000"})
     * @return weak or strong {@link ETag}, depending on the {@code weak} flag
     */
    @Override
    public ETag generate(String seed) {
        String hash = generator.generate(seed);

        if (!hash.isBlank()) {
            hash = "%s_%s".formatted(MediaTypeHelper.TITLE, hash.toUpperCase().substring(0, 16));
        }

        return weak ? ETag.weak(hash) : ETag.strong(hash);
    }

    /**
     * Convenience helper to build a standard seed from content metadata.
     *
     * @param length         entity length in bytes
     * @param lastModifiedMs last-modified timestamp in epoch milliseconds
     * @return seed string of the form {@code "length:lastModifiedMs"}
     */
    public static String seed(long length, long lastModifiedMs) {
        return "%d:%d".formatted(length, lastModifiedMs);
    }
}
