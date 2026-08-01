package org.jmouse.storage.policy;

/**
 * 🚦 How an {@link UploadPolicy} reads its lists of content types and extensions.
 *
 * <p>Two modes rather than two components: "reject these dangerous types" and "accept only these
 * formats" are the same rule pointed in opposite directions, and products that answered them
 * differently were running two implementations of one idea.</p>
 */
public enum AcceptanceMode {

    /**
     * ✅ Only what is listed may enter storage. Anything unlisted — including content with no
     * extension at all — is refused.
     */
    ALLOWLIST,

    /**
     * ⛔ Everything may enter storage except what is listed.
     */
    DENYLIST
}
