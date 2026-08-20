package org.jmouse.ai;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * What one call produced, plus where it ran and what actually happened.
 *
 * <p>The scope is not decoration. Every answer states where it acted, including when a default
 * supplied it — that sentence in a transcript is the only thing standing between "wrong place" being
 * noticed now and being noticed in a week.
 *
 * <p><strong>The verdict is carried rather than inferred.</strong> The obvious alternative is to look
 * for a {@code status} key inside the payload and switch on its value, which is a guess dressed as a
 * check: it works until a status string is renamed, and then it silently stops telling a user that
 * nothing happened. The guards already know which of the three things they did; carrying the answer
 * costs one field.
 *
 * @param payload     what the caller receives
 * @param scope       where it ran, or null for an action that is not confined to one
 * @param verdict     whether the work happened, was previewed, or was suppressed as a repeat
 * @param operationId shared by a preview and the call that confirms it, so the two read as one
 *                    operation rather than as two unrelated events
 */
public record ToolOutcome(Object payload, InvocationScope scope, CallVerdict verdict, String operationId) {

    /**
     * The structured body a client receives: the result, wrapped so the scope travels with it rather
     * than being buried in prose the model may not read.
     */
    public Map<String, Object> asStructuredContent() {
        Map<String, Object> body = new LinkedHashMap<>();

        if (scope != null) {
            body.put("scope", Map.of(
                    "kind",       scope.kind(),
                    "name",       scope.label(),
                    "wasDefault", scope.defaulted()));
        }

        // ⚠️ An image is described here and carried as a content block, never both: base64 in the
        // JSON as well would send it twice, and the second copy lands somewhere no model looks at it
        // and every model pays for it.
        body.put("result", payload instanceof ToolImage picture ? picture.about() : payload);

        return body;
    }

    /**
     * The one-line preamble, so the important part is legible in a transcript without reading JSON.
     *
     * <p>Two things belong on that line. Where it ran, always. And — when the call did not do what it
     * was asked — <em>that</em>: a preview and a suppressed duplicate both come back as ordinary
     * successful results, and a client showing only this line would otherwise render a cheerful
     * location over an operation that has not happened yet.
     */
    public String describe() {
        // ⚠️ "Across everything this caller can see" was wrong for the arrangement this library was
        // built for. An action that is not scope-confined acts on the *acting subject's* records, and
        // a service credential's own reach is not what it ran over. The sentence now says only what is
        // true of every scopeless action: nothing narrowed it.
        String where = scope == null
                ? "Not confined to one place."
                : capitalise(scope.kind()) + ": " + scope.echo() + ".";

        return headline().map(headline -> headline + " " + where).orElse(where);
    }

    /** What the guards did instead of the work, when they did something instead of the work. */
    private Optional<String> headline() {
        return switch (verdict) {
            case CARRIED_OUT -> Optional.empty();

            case PREVIEWED -> Optional.of(
                    "NOTHING WAS CHANGED — this is a preview awaiting confirmation.");

            case DUPLICATE_SUPPRESSED -> Optional.of(
                    "NOTHING WAS CHANGED — an identical call was made moments ago; its result is below.");
        };
    }

    private static String capitalise(String word) {
        return word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /**
     * 🖼️ The picture this call is handing back, where it is handing one back.
     *
     * <p>⚠️ A transport that can carry an image should add it as a content block of its own; one that
     * cannot is still correct, because {@link #asStructuredContent()} always describes it in words. What
     * neither does is put the bytes in the JSON — see {@link ToolImage}.</p>
     *
     * @return the image, or empty
     */
    public Optional<ToolImage> image() {
        return payload instanceof ToolImage picture ? Optional.of(picture) : Optional.empty();
    }
}
