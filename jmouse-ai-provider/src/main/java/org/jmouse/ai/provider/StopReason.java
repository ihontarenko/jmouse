package org.jmouse.ai.provider;

import java.util.Locale;

/**
 * Why the model stopped talking.
 *
 * <p>A value rather than the provider's own string, for the same reason {@code jmouse-ai} carries a
 * verdict instead of looking for a {@code status} key: a loop that compares against {@code "tool_use"}
 * works until a provider spells it differently, and then it silently stops running tools. Two
 * providers already spell most of these differently, which is the argument made twice.
 *
 * <p>⚠️ The mapping lives here rather than in each model, so that a third implementation cannot quietly
 * invent a fourth spelling of "the turn ended".
 */
public enum StopReason {

    /** The model finished. Whatever it had to say is in the content. */
    END_TURN,

    /** The model asked for one or more tools to be run and the results sent back. */
    TOOL_USE,

    /** It ran out of room. The content is a sentence cut in half, and there is more it wanted to say. */
    MAX_TOKENS,

    /** It hit a sequence the caller asked it to stop at. */
    STOP_SEQUENCE,

    /** The provider declined to answer. Not an error — a decision, and the content usually says why. */
    REFUSAL,

    /**
     * Something this library has not seen.
     *
     * <p>Treated as the end of a turn by anything that has to choose, because the alternative — looping
     * on an unrecognised reason — is the one behaviour that costs money while achieving nothing. The
     * model that could not recognise it logs the spelling.
     */
    UNKNOWN;

    /**
     * Whatever a provider called it.
     *
     * <p>Two vocabularies, and they overlap less than they look. Anthropic's spellings are this enum's
     * own names in lower case — which is also the canonical wire form a gateway speaks — so they need
     * no table. OpenAI's share nothing with anyone's and are the whole of the one below.
     */
    public static StopReason of(String wireName) {
        if (wireName == null || wireName.isBlank()) {
            return UNKNOWN;
        }

        StopReason openAiName = switch (wireName) {
            case "stop"           -> END_TURN;
            case "tool_calls"     -> TOOL_USE;
            case "length"         -> MAX_TOKENS;
            case "content_filter" -> REFUSAL;
            default               -> null;
        };

        if (openAiName != null) {
            return openAiName;
        }

        try {
            return valueOf(wireName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unrecognised) {
            return UNKNOWN;
        }
    }

    /** The canonical spelling, for anything serving the shape {@code GatewayChatModel} reads. */
    public String wireName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Whether the caller is expected to run something and come back. */
    public boolean wantsTools() {
        return this == TOOL_USE;
    }

    /** Whether the model has said everything it is going to say on this turn. */
    public boolean endsTheTurn() {
        return !wantsTools();
    }
}
