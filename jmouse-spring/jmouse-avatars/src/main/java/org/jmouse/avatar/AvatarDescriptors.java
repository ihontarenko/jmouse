package org.jmouse.avatar;

import org.jmouse.storage.exception.UploadRejectedException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * 🎲 What a generated face is stored as, and what this module will accept as one.
 *
 * <h3>The descriptor</h3>
 *
 * <pre>avatar.1.&lt;strategy&gt;.&lt;url-encoded seed&gt;.&lt;base64 parameters&gt;</pre>
 *
 * <p>A word, an engine version, the strategy that draws it, the seed it is drawn from, and whatever
 * that strategy's controls were set to.</p>
 *
 * <p>⚠️ <strong>The seed is url-encoded <em>and</em> its dots are escaped on top of that.</strong> A dot
 * is unreserved in a URI, so ordinary url-encoding leaves it exactly where it is — while here it is the
 * separator. A seed is an arbitrary string and one strategy takes a username or an address, so
 * {@code ivan.hontarenko} is the ordinary case rather than the exotic one; unescaped it splits the
 * descriptor into six parts. That is why the part count below is checked rather than assumed.</p>
 *
 * <h3>⚠️ A bare value with no dots is a legacy seed, and stays valid forever</h3>
 *
 * <p>Before the drawing engine became a package there was one generator, and what was stored was a
 * bare seed. Those values are live in three databases and a person who chose a face is entitled to
 * keep it, so the old generator ships as a strategy of its own and a dotless value is read as one.
 * This is not a transitional kindness with an end date: it is why adopting the shared engine changes
 * nobody's avatar.</p>
 *
 * <h3>⚠️ Shape only — this module does not draw</h3>
 *
 * <p>The face is assembled by the interface, from the strategy and the parameters. So there is no set
 * of valid strategies to check against and no schema for the parameters; inventing either would mean a
 * catalogue on the server edited in step with the interface every time a strategy is added or a control
 * gains an option. What is refused here is a value that could not have come from a picker at all.</p>
 *
 * <h4>⚠️ The version IS pinned, and that is a correction</h4>
 *
 * <p>An earlier draft accepted any version, reasoning that the interface owns drawing and therefore
 * owns the version. That reasoning has a hole: a permissive server stores {@code avatar.2.…}, the
 * engine that has to draw it refuses, and the row renders as no face at all — a person silently without
 * one, discovered by looking rather than by an error. Refusing at the door costs a constant bump beside
 * the engine's own; accepting costs a faceless row nobody is told about.</p>
 */
public final class AvatarDescriptors {

    /** ⚠️ The first segment, as a full word. Never an abbreviation — this is stored data. */
    public static final String PREFIX = "avatar";

    /**
     * The longest descriptor a column has to hold.
     *
     * <p>⚠️ The three products' columns were {@code VARCHAR(64)}, which fitted a bare seed and cannot
     * fit a descriptor with parameters. Each of them widens to this; a value longer is refused with the
     * number rather than truncated, because a truncated descriptor is a face nobody can draw and a row
     * nobody can explain.</p>
     */
    public static final int MAXIMUM_LENGTH = 512;

    /** How many dot-separated pieces a descriptor has. */
    private static final int SEGMENT_COUNT = 5;

    private static final int PREFIX_SEGMENT     = 0;
    private static final int VERSION_SEGMENT    = 1;
    private static final int STRATEGY_SEGMENT   = 2;
    private static final int SEED_SEGMENT       = 3;
    private static final int PARAMETERS_SEGMENT = 4;

    /**
     * What a legacy seed may look like, unchanged from before the descriptor existed.
     *
     * <p>Lowercase words joined by hyphens: long enough to be a name, too plain to be a payload.</p>
     */
    private static final Pattern LEGACY_SEED_SHAPE = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    private static final int LEGACY_SEED_MAXIMUM_LENGTH = 64;

    /** A strategy identifier, in the same shape the engine writes them. */
    private static final Pattern STRATEGY_SHAPE = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    /**
     * The engine version this library will accept.
     *
     * <p>⚠️ Bump it in the same change that ships a new drawing engine, and never before — a descriptor
     * stored under a version nothing can draw is a person with no face and no error to explain it.</p>
     */
    public static final int SUPPORTED_VERSION = 1;

    private AvatarDescriptors() {
    }

    /**
     * ✅ The descriptor, trimmed, or a refusal saying what is wrong with it.
     *
     * @param candidate what was submitted
     * @return the value to store
     * @throws UploadRejectedException when it could not have come from a picker
     */
    public static String validated(String candidate) {
        String value = candidate == null ? "" : candidate.trim();

        if (value.isEmpty()) {
            throw new UploadRejectedException("A generated avatar needs a descriptor.");
        }

        if (value.length() > MAXIMUM_LENGTH) {
            throw new UploadRejectedException(
                "An avatar descriptor may be up to %d characters — this one is %d."
                    .formatted(MAXIMUM_LENGTH, value.length()));
        }

        if (isLegacySeed(value)) {
            return validatedLegacySeed(value);
        }

        return validatedDescriptor(value);
    }

    /**
     * 🕰️ Whether this is a value from before the descriptor existed.
     *
     * <p>A bare seed carries no dots, and every descriptor carries four. Nothing subtler is needed and
     * nothing subtler would be honest — the absence of a separator <em>is</em> the distinction.</p>
     *
     * @param value the stored value
     * @return whether it is a legacy seed
     */
    public static boolean isLegacySeed(String value) {
        return value != null && value.indexOf('.') < 0;
    }

    private static String validatedLegacySeed(String value) {
        if (value.length() > LEGACY_SEED_MAXIMUM_LENGTH || !LEGACY_SEED_SHAPE.matcher(value).matches()) {
            throw new UploadRejectedException(
                "\"%s\" is not a usable avatar seed — lowercase words joined by hyphens, up to %d characters."
                    .formatted(value, LEGACY_SEED_MAXIMUM_LENGTH));
        }

        return value;
    }

    private static String validatedDescriptor(String value) {
        String[] segments = value.split("\\.", -1);

        if (segments.length != SEGMENT_COUNT) {
            throw new UploadRejectedException(
                "An avatar descriptor has %d dot-separated parts — this one has %d."
                    .formatted(SEGMENT_COUNT, segments.length));
        }

        if (!PREFIX.equals(segments[PREFIX_SEGMENT])) {
            throw new UploadRejectedException(
                "An avatar descriptor starts with \"%s\" — this one starts with \"%s\"."
                    .formatted(PREFIX, segments[PREFIX_SEGMENT]));
        }

        if (!String.valueOf(SUPPORTED_VERSION).equals(segments[VERSION_SEGMENT])) {
            throw new UploadRejectedException(
                "This installation draws avatar engine version %d — \"%s\" is not one it can draw."
                    .formatted(SUPPORTED_VERSION, segments[VERSION_SEGMENT]));
        }

        if (!STRATEGY_SHAPE.matcher(segments[STRATEGY_SEGMENT]).matches()) {
            throw new UploadRejectedException(
                "\"%s\" is not a usable strategy name.".formatted(segments[STRATEGY_SEGMENT]));
        }

        if (segments[SEED_SEGMENT].isEmpty()) {
            throw new UploadRejectedException("An avatar descriptor needs a seed.");
        }

        ensureParametersDecode(segments[PARAMETERS_SEGMENT]);

        return value;
    }

    /**
     * 📦 That the parameters are base64 and hold an object — never what is inside it.
     *
     * <p>⚠️ The braces are checked and the contents are not, on purpose. Every strategy has its own
     * controls and every control may gain an option; a server that validated the contents would have to
     * be redeployed to let somebody pick a new hairstyle.</p>
     */
    private static void ensureParametersDecode(String parameters) {
        byte[] decoded;

        try {
            decoded = Base64.getDecoder().decode(parameters);
        } catch (IllegalArgumentException exception) {
            throw new UploadRejectedException("The parameters of an avatar descriptor have to be base64.");
        }

        String text = new String(decoded, StandardCharsets.UTF_8).trim();

        if (!text.startsWith("{") || !text.endsWith("}")) {
            throw new UploadRejectedException(
                "The parameters of an avatar descriptor have to be a JSON object.");
        }
    }
}
