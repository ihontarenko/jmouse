package org.jmouse.ai.guard;

import org.jmouse.ai.AffectedRecords;
import org.jmouse.ai.CallVerdict;
import org.jmouse.ai.RefusalReason;
import org.jmouse.ai.ToolInvocation;
import org.jmouse.ai.ToolRefusedException;
import org.jmouse.ai.spi.ConfirmationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Shows what would happen before it happens, and redeems the token that says somebody looked.
 *
 * <p>Two halves of one idea, and they belong in one guard because they share a definition of what
 * makes two calls the same call. A preview issues a token against a fingerprint; the confirming call
 * is checked against it. Splitting them would mean two places agreeing on that definition, and the
 * disagreement would be a confirmed operation that is not the one that was shown.
 *
 * <p><strong>What is stored is the resolved set of records, never the filter.</strong> A preview is a
 * contract about specific records, not an estimate of how many there might be by then — the handler is
 * handed the frozen set and given no way to widen it. Anything added between the preview and the
 * confirmation is not affected, and the preview says so.
 *
 * <p>Everything destructive is confirmed however few records it touches, because one is exactly as
 * irreversible as forty. Everything else is confirmed only past a threshold, because a bulk update is
 * not a deletion but zeroing five hundred quantities is the same disaster without the word.
 */
public final class ConfirmationGuard implements InvocationGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationGuard.class);

    public static final String NAME = "confirmation";

    /** What a preview answers with, which a transport and a model both branch on. */
    public static final String CONFIRMATION_REQUIRED = "confirmation-required";

    private final ConfirmationStore confirmations;
    private final GuardSettings     settings;

    public ConfirmationGuard(ConfirmationStore confirmations, GuardSettings settings) {
        this.confirmations = confirmations;
        this.settings      = settings;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int order() {
        return CONFIRMATION_ORDER;
    }

    @Override
    public boolean appliesTo(GuardContext context) {
        return context.action().writes();
    }

    @Override
    public GuardedCall guard(GuardContext context, GuardContinuation next) {
        return context.presentedToken()
                .map(token -> redeem(context, next, token))
                .orElseGet(() -> previewOrProceed(context, next));
    }

    // ── Redemption ───────────────────────────────────────────────────────────────

    /**
     * Spends a token and runs exactly what it authorised.
     *
     * <p>The records come from the token rather than from the action's resolver — that is the entire
     * point, and the reason the ceiling and the emptiness check step aside for a redemption rather than
     * asking their questions again.
     */
    private GuardedCall redeem(GuardContext context, GuardContinuation next, String token) {
        PendingConfirmation pending = validate(context, token);

        LOGGER.info("{} confirmed for {} record(s) by caller {}",
                context.action().qualifiedName(), pending.records().size(), pending.callerId());

        context.freeze(pending.records(), pending.records().size(), true);

        return next.proceed(context);
    }

    /**
     * Five checks, five distinct refusals.
     *
     * <p>One message saying "that confirmation is not valid" would be five different problems with five
     * different fixes wearing one sentence — and a model that cannot tell "you changed the arguments"
     * from "that token expired" retries the wrong one.
     */
    private PendingConfirmation validate(GuardContext context, String token) {
        String publishedName = context.action().publishedName();

        PendingConfirmation pending = confirmations.consume(token)
                .orElseThrow(() -> new ToolRefusedException(RefusalReason.INVALID_CONFIRMATION,
                        "That confirmation is not valid — it has expired, or it was already used. A token "
                        + "works exactly once and lasts " + confirmations.lifetime().toSeconds()
                        + " seconds. Call '" + publishedName + "' again without '"
                        + ToolInvocation.CONFIRM_ARGUMENT + "' to get a fresh preview, and check the list "
                        + "before confirming: it may have changed. Nothing was changed."));

        if (!pending.callerId().equals(context.invocation().callerId())) {
            throw new ToolRefusedException(RefusalReason.INVALID_CONFIRMATION,
                    "That confirmation was issued to a different caller. Nothing was changed.");
        }

        if (!pending.publishedName().equals(publishedName)) {
            throw new ToolRefusedException(RefusalReason.INVALID_CONFIRMATION,
                    "That confirmation was issued for '" + pending.publishedName() + "', not for '"
                    + publishedName + "'. A preview authorises one operation and only that one. Preview "
                    + "this call to get its own token. Nothing was changed.");
        }

        if (!pending.fingerprint().equals(context.fingerprint())) {
            throw new ToolRefusedException(RefusalReason.INVALID_CONFIRMATION,
                    "The arguments changed since that preview, so it no longer describes what would "
                    + "happen. Call '" + publishedName + "' again without '"
                    + ToolInvocation.CONFIRM_ARGUMENT + "' to preview the call as it now stands. Nothing "
                    + "was changed.");
        }

        // ⚠️ The fingerprint covers the scope the caller *named*, which is not the scope the call
        // *resolved to* — omit the argument and a default supplies it, and the default can change
        // between the two calls when the caller gains or loses a place. Comparing the resolved one is
        // the only version of this check that means anything. Objects.equals so two absent scopes
        // match: an action that is not confined to one still goes through here.
        if (!Objects.equals(pending.scopeId(), context.invocation().scopeId())) {
            throw new ToolRefusedException(RefusalReason.INVALID_CONFIRMATION,
                    "That preview was made somewhere other than where this call resolved to, so the "
                    + "records it listed are not the records this call is about. Preview it again "
                    + describeHere(context) + ". Nothing was changed.");
        }

        return pending;
    }

    private String describeHere(GuardContext context) {
        return context.invocation().scope() == null
                ? "as this call now stands"
                : "in '" + context.invocation().scopeLabel() + "'";
    }

    // ── Preview ──────────────────────────────────────────────────────────────────

    private GuardedCall previewOrProceed(GuardContext context, GuardContinuation next) {
        AffectedRecords affected = context.affectedRecords();

        if (!needsConfirmation(context, affected)) {
            return next.proceed(context);
        }

        return preview(context, affected);
    }

    private boolean needsConfirmation(GuardContext context, AffectedRecords affected) {
        return context.action().destructive()
            || affected.totalCount() > settings.confirmationThreshold();
    }

    /**
     * Nothing is written. A token is issued that will authorise exactly these records and nothing else.
     *
     * <p>The body is written for two readers at once: a model, which needs to be told unambiguously
     * that nothing has happened yet and precisely how to make it happen, and a person, who needs to see
     * the list before agreeing to it. {@code howToProceed} says all four of the things that are
     * otherwise learned by getting them wrong — same arguments, plus the token, only these records,
     * once.
     */
    private GuardedCall preview(GuardContext context, AffectedRecords affected) {
        String token = confirmations.issue(new PendingConfirmation(
                context.operationId(),
                context.invocation().callerId(),
                context.action().publishedName(),
                context.fingerprint(),
                context.invocation().scopeId(),
                affected.records()));

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("status",           CONFIRMATION_REQUIRED);
        body.put("reason",           whyConfirmationIsNeeded(context, affected));
        body.put("affectedCount",    affected.totalCount());
        body.put("affected",         affected.describe());
        body.put(ToolInvocation.CONFIRM_ARGUMENT, token);
        body.put("expiresInSeconds", confirmations.lifetime().toSeconds());
        body.put("howToProceed",
                "Nothing has been changed. Show this list to the user. To go ahead, call '"
                + context.action().publishedName() + "' again with the same arguments plus "
                + ToolInvocation.CONFIRM_ARGUMENT + "='" + token + "'. Only the records listed above "
                + "will be affected — anything added in the meantime will not be. The token works once, "
                + "and only for this exact call.");

        LOGGER.info("{} previewed {} record(s) for caller {}",
                context.action().qualifiedName(), affected.totalCount(),
                context.invocation().callerId());

        return new GuardedCall(body, CallVerdict.PREVIEWED, context.operationId(),
                affected.records(), affected.totalCount(), true);
    }

    /**
     * Says "removes or overwrites" rather than "deletes", because a destructive action is not always a
     * delete — rewriting a document replaces prose that has no other copy, which is the same loss under
     * a gentler name.
     */
    private String whyConfirmationIsNeeded(GuardContext context, AffectedRecords affected) {
        if (context.action().destructive()) {
            return "'" + context.action().qualifiedName() + "' removes or overwrites data, and there is "
                 + "no undo. Every such call is confirmed, however few records it touches — one is "
                 + "exactly as irreversible as forty. The list above is what it would affect.";
        }

        return "This affects " + affected.totalCount() + " records, more than the "
             + settings.confirmationThreshold() + " that proceed without confirmation. It is not a "
             + "deletion, but changing this many records at once is worth looking at first.";
    }
}
