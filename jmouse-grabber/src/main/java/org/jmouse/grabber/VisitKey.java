package org.jmouse.grabber;

import java.net.URI;
import java.util.Objects;

/**
 * 🔑 What "already done" means.
 *
 * <p>Two visits with the same key are the same visit, so this type decides both deduplication within
 * a run and resume across runs. ⚠️ It is written into every journal on disk, which is why it is a
 * plain string rather than a structure: a key that changes shape invalidates work somebody already
 * paid for.</p>
 *
 * <p>The default key is the normalised address — see {@link AddressNormalizer}. A route may override
 * it with anything that identifies the thing rather than the address, which is how one product
 * reachable at two addresses stays one product.</p>
 */
public record VisitKey(String value) {

    public VisitKey {
        Objects.requireNonNull(value, "A visit key cannot be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("A visit key cannot be blank");
        }
    }

    /**
     * 🔑 The key of an address, normalised.
     */
    public static VisitKey ofAddress(URI address, AddressNormalizer normalizer) {
        return new VisitKey(normalizer.normalize(address).toString());
    }

    /**
     * 🔑 A key the caller decided on — a SKU, a content hash, an identifier read off the page.
     */
    public static VisitKey of(String value) {
        return new VisitKey(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
