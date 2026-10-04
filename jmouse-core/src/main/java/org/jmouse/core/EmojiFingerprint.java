package org.jmouse.core;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * 🦊 An identifier hashed to an emoji — a fingerprint a person tells apart at a glance.
 *
 * <p>Meant for identifiers people compare by eye and never read: a build, a deployment, a session.
 * Two builds that report the same version and commit differ in their identifier, and "the fox build"
 * versus "the rocket build" is noticed where {@code lz3k9q1} versus {@code lz3kb20} is not.
 *
 * <p>⚠️ <b>A fingerprint, not an identity.</b> There are {@value #PALETTE_SIZE} emoji, so two different
 * identifiers share one often. It says "these are probably different", never "these are the same".
 *
 * <p>⚠️ <b>The palette and the hash are a contract.</b> The same identifier must give the same emoji in
 * every process and every client that draws one — a server, a browser, a television. So the list is
 * never reordered or extended in place: a client carrying a copy (a Kotlin one, say) would silently
 * disagree. The hash is 32-bit FNV-1a over the UTF-8 bytes, which is trivial to reproduce anywhere.
 *
 * <p>Every entry is a single code point with emoji presentation by default — no variation selector,
 * no joiner sequence — so it is one glyph wherever a current emoji font draws it. Some come from
 * Emoji 13–15; a device whose font is older than that shows those few as an empty box.
 */
public final class EmojiFingerprint {

    /** How many emoji the palette holds. */
    public static final int PALETTE_SIZE = 256;

    private static final List<String> PALETTE = List.of(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔",
            "🐧", "🦆", "🦅", "🦉", "🦇", "🐺", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞", "🐢", "🐍", "🦎",
            "🐙", "🦑", "🦐", "🦀", "🐠", "🐬", "🐳", "🦈", "🐊", "🐘", "🦏", "🐪", "🦌", "🐓", "🦃", "🐇",
            "🌵", "🌲", "🌴", "🌱", "🍀", "🍁", "🍄", "🌷", "🌻", "🌹", "🍏", "🍎", "🍐", "🍊", "🍋", "🍌",
            "🍉", "🍇", "🍓", "🍈", "🍒", "🍑", "🍍", "🥝", "🥑", "🍆", "🥕", "🌽", "🥔", "🧀", "🍞", "🥐",
            "🥞", "🍔", "🍟", "🍕", "🌭", "🌮", "🍩", "🍪", "🎈", "🎁", "🎀", "🎃", "🎄", "🎨", "🎭", "🎲",
            "🎯", "🎳", "🎸", "🎺", "🎻", "🥁", "🎹", "📷", "🔭", "💡", "🔔", "🔑", "🔨", "⚓", "⌛", "💎",
            "🏆", "🎩", "🚀", "🚁", "🚂", "🚲", "🚗", "⛵", "🚢", "🌈", "🔥", "🌙", "⭐", "🌊", "⚡", "⛄",
            "🦒", "🦓", "🦔", "🦕", "🦖", "🦗", "🦘", "🦙", "🦚", "🦛", "🦜", "🦝", "🦞", "🦠", "🦡", "🦢",
            "🦣", "🦤", "🦥", "🦦", "🦧", "🦨", "🦩", "🦫", "🦬", "🦭", "🪲", "🪼", "🪿", "🐡", "🦍", "🐅",
            "🐆", "🐃", "🐎", "🐏", "🐑", "🐐", "🐩", "🐈", "🫎", "🐤", "🐣", "🪸", "🪷", "🪻", "🌸", "🌼",
            "🌾", "🥀", "🪴", "🌿", "🎋", "🎍", "🥭", "🥥", "🥦", "🥬", "🥒", "🫐", "🫑", "🫒", "🧄", "🧅",
            "🥯", "🥨", "🥖", "🧇", "🥓", "🍗", "🍝", "🍜", "🍣", "🍱", "🥟", "🍤", "🍙", "🍥", "🥠", "🍡",
            "🍨", "🍦", "🥧", "🧁", "🎂", "🍭", "🍬", "🍫", "🍿", "🧈", "🧂", "🥤", "🧃", "🧉", "🧊", "🍵",
            "🫖", "🍯", "🪀", "🪁", "🪃", "🪄", "🪅", "🪆", "🪐", "🧩", "🧸", "🧶", "🧵", "🧿", "🔮", "🪩",
            "🎮", "🎰", "🧲", "🪜", "🔧", "🔩", "🧪", "🧬", "🔬", "🩺", "💊", "🧯", "🪣", "🧽", "🧴", "🧼");

    private static final int FNV_OFFSET_BASIS = 0x811c9dc5;
    private static final int FNV_PRIME        = 0x01000193;

    private EmojiFingerprint() {
    }

    /**
     * The emoji {@code identifier} hashes to.
     *
     * @param identifier any text; the same text always gives the same emoji
     * @return one emoji out of the palette
     */
    public static String of(String identifier) {
        requireNonNull(identifier, "identifier");
        return PALETTE.get(Integer.remainderUnsigned(fnv1a(identifier), PALETTE_SIZE));
    }

    /** The whole palette, in hashing order — for a client that must reproduce {@link #of(String)}. */
    public static List<String> palette() {
        return PALETTE;
    }

    private static int fnv1a(String identifier) {
        int hash = FNV_OFFSET_BASIS;
        for (byte character : identifier.getBytes(StandardCharsets.UTF_8)) {
            hash ^= character & 0xff;
            hash *= FNV_PRIME;
        }
        return hash;
    }
}
