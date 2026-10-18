package org.jmouse.java.index.store.disk;

import java.util.HexFormat;

/** Validated SHA-256 source identity. Byte digesting remains in IndexFiles. */
record ContentHash(String value) {

    private static final int DIGEST_BYTES = 32;
    private static final HexFormat HEX = HexFormat.of();

    ContentHash {
        if (value == null || value.length() != DIGEST_BYTES * 2) {
            throw new IllegalArgumentException("A SHA-256 content hash is required.");
        }

        byte[] digest = HEX.parseHex(value);

        if (!HEX.formatHex(digest).equals(value)) {
            throw new IllegalArgumentException(
                    "Content hashes use canonical lowercase hexadecimal.");
        }
    }
}
