package org.jmouse.ai.sandbox;

import org.jmouse.ai.ToolCatalog;
import org.jmouse.ai.ToolDispatcher;
import org.jmouse.ai.ToolInvocation;
import org.jmouse.ai.ToolOutcome;
import org.jmouse.ai.ToolRefusedException;
import org.jmouse.ai.guard.TokenBucketCallerRateLimiter;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every path through the dispatcher, run once, printing what happened.
 *
 * <p>The running check that stands in for the tests this effort deliberately writes last. It is not a
 * demonstration of the happy path: two thirds of what it runs is refused, because a refusal is what a
 * model actually reads and the sentence it reads is most of what this library is.
 *
 * <p>⚠️ <strong>The order of the scenarios is load-bearing.</strong> The inventory is mutated as it
 * goes — a confirmed restock, a confirmed discard, three parts received — so a scenario moved above one
 * that changes a count silently stops testing what it was written for. Where a scenario depends on a
 * number, the number is named in its claim.
 *
 * <p>The two entry points that arrive with tickets 06 and 07 extend this same application rather than
 * starting new ones.
 */
public final class SandboxApplication {

    private final SandboxWorkshop    workshop    = new SandboxWorkshop();
    private final Transcript         transcript  = new Transcript();

    private final WorkshopInventory  inventory   = workshop.inventory();
    private final SandboxCallers     callers     = workshop.callers();
    private final RecordingTrace     trace       = workshop.trace();
    private final ToolCatalog        catalog     = workshop.catalog();
    private final ToolDispatcher     dispatcher  = workshop.dispatcher();

    public static void main(String[] arguments) {
        new SandboxApplication().run();
    }

    private SandboxApplication() {
    }

    private void run() {
        theTrivialPath();
        theScopeADefaultSupplies();
        theThreeWaysAScopeFails();
        permissionAtTwoDepths();
        confirmationAndItsToken();
        theCeilingIsNotABiggerThreshold();
        destructionThatWasLookedAt();
        aFilterThatMatchedNothing();
        argumentsNestedTwoDeep();
        theSameCallTwice();
        oneRecordAddressedByName();
        recordsThatBelongToNoPlace();
        aNameThatIsNotThere();
        aCallerInALoop();
        whatTheCatalogueWouldNotStartWith();
        whatWasRecorded();
    }

    // ── The scenarios ────────────────────────────────────────────────────────────

    private void theTrivialPath() {
        transcript.scenario("The trivial path",
                "Read-only, no scope, no arguments - and the one action that reads the CALLER rather "
                + "than the acting subject. The assistant sees one workshop; the person sees three.");

        asAssistant();
        attempt("workshops_list", noArguments());

        asPerson();
        attempt("workshops_list", noArguments());
    }

    private void theScopeADefaultSupplies() {
        transcript.scenario("The scope a default supplies",
                "The assistant can see exactly one workshop, so naming none is not ambiguous. The "
                + "answer says where it ran and that a default supplied it - which is the whole point "
                + "of the echo.");

        asAssistant();
        attempt("parts_list", noArguments());
        attempt("parts_list", arguments("shelf", "B"));
    }

    private void theThreeWaysAScopeFails() {
        transcript.scenario("The three ways a scope fails",
                "The person can see three workshops, two of them called 'Garage'. Undetermined, "
                + "ambiguous and unknown are three different problems with three different fixes, and "
                + "each refusal names what this caller can actually see.");

        asPerson();
        attempt("parts_list", noArguments());
        attempt("parts_list", arguments("scope", "Garage"));
        attempt("parts_list", arguments("scope", "Shed"));
    }

    private void permissionAtTwoDepths() {
        transcript.scenario("Permission, asked twice",
                "The person holds no 'parts:delete' anywhere, and holds 'parts:write' only in the two "
                + "garages. The first refusal names no workshop at all - it is raised before any scope "
                + "is resolved, so a caller who may do nothing cannot enumerate the places by reading "
                + "refusals.");

        asPerson();
        attempt("parts_discard", arguments("scope", "Bench", "shelf", "A"));
        attempt("parts_receive", arguments("scope", "Bench", "items", List.of(anItem("M8 bolt", "A", 5))));

        transcript.note("");
        transcript.note("...and this caller cannot reach either place where it DOES hold that "
                      + "permission, because both are called 'Garage'. Two visible scopes sharing a "
                      + "name is an inconvenience for a read and a wall for a write.");
    }

    private void confirmationAndItsToken() {
        transcript.scenario("Confirmation, and what its token is worth",
                "Five parts on shelf A, over a threshold of three. Nothing is changed; a token is "
                + "issued against those five records and no others. It works exactly once.");

        asAssistant();
        Map<String, Object> restock = arguments("shelf", "A", "by", 1);

        Optional<String> token = attempt("parts_restock", restock).flatMap(SandboxApplication::tokenIn);

        token.ifPresent(issued -> {
            attempt("parts_restock", with(restock, ToolInvocation.CONFIRM_ARGUMENT, issued));

            transcript.note("...and the same token a second time:");
            attempt("parts_restock", with(restock, ToolInvocation.CONFIRM_ARGUMENT, issued));
        });
    }

    private void theCeilingIsNotABiggerThreshold() {
        transcript.scenario("The ceiling is not a bigger threshold",
                "The same action over the whole workshop reaches eight parts, past a ceiling of six. "
                + "No token is offered, because no amount of agreeing would get past it - and the "
                + "refusal has to say so, or a model reads it as 'ask again with confirm'.");

        asAssistant();
        attempt("parts_restock", arguments("by", 1));
    }

    private void destructionThatWasLookedAt() {
        transcript.scenario("Destruction that was looked at",
                "Three parts on shelf B. Destructive, so it is previewed however few it touches - and "
                + "the preview keeps each record's state, because after the work there is no other copy "
                + "of what they were.");

        asAssistant();
        Map<String, Object> discard = arguments("shelf", "B");

        attempt("parts_discard", discard)
                .flatMap(SandboxApplication::tokenIn)
                .ifPresent(issued ->
                        attempt("parts_discard", with(discard, ToolInvocation.CONFIRM_ARGUMENT, issued)));
    }

    private void aFilterThatMatchedNothing() {
        transcript.scenario("A filter that matched nothing",
                "There is no shelf Z. Without this refusal the call previews zero records and hands "
                + "back a token to confirm them, which a model reads as 'ready to proceed'.");

        asAssistant();
        attempt("parts_discard", arguments("shelf", "Z"));
    }

    private void argumentsNestedTwoDeep() {
        transcript.scenario("Arguments nested two deep",
                "A list of objects, each with its own required members. The refusal has to name WHICH "
                + "entry was wrong - three entries all reporting a problem with something called "
                + "'quantity' is a message nobody can act on.");

        asAssistant();
        attempt("parts_receive", arguments("items", List.of(
                anItem("M8 bolt",   "A", 20),
                anItem("M8 nut",    "A", 20),
                anItem("M10 bolt",  "A",  0))));

        attempt("parts_receive", arguments("items", List.of(
                anItem("M8 bolt",  "A", 20),
                anItem("M8 nut",   "A", 20),
                anItem("M10 bolt", "A", 15))));
    }

    private void theSameCallTwice() {
        transcript.scenario("The same call twice",
                "A create reaches nothing that exists, so neither the ceiling nor the threshold has "
                + "anything to fire on - deduplication is the only thing standing between a retried "
                + "transport and a second set of records. The caller says otherwise with "
                + "'" + ToolInvocation.ALLOW_DUPLICATE_ARGUMENT + "'.");

        asAssistant();
        Map<String, Object> receive = arguments("items", List.of(anItem("Grease cartridge", "C", 6)));

        attempt("parts_receive", receive);
        attempt("parts_receive", receive);
        attempt("parts_receive", with(receive, ToolInvocation.ALLOW_DUPLICATE_ARGUMENT, true));
    }

    private void oneRecordAddressedByName() {
        transcript.scenario("One record, addressed by name",
                "A second definition contributing to the same 'parts' namespace, and the two refusals "
                + "every single-record action needs: an identifier that exists somewhere this call may "
                + "not look, and one that exists nowhere. They are raised in different places and say "
                + "different things.");

        asAssistant();
        attempt("parts_move", arguments("partId", "part-1", "toShelf", "C"));
        attempt("parts_move", arguments("partId", "part-9", "toShelf", "C"));
        attempt("parts_move", arguments("partId", "part-404", "toShelf", "C"));
    }

    private void recordsThatBelongToNoPlace() {
        transcript.scenario("Records that belong to no place",
                "Notes are the acting SUBJECT's, and this action takes no scope at all - so every "
                + "refusal on the path has to have a scopeless half. Note whose notes come back: the "
                + "assistant's caller identity read the workshops, and the owner's identity reads "
                + "these.");

        asAssistant();
        attempt("notes_list", noArguments());

        Map<String, Object> delete = arguments("containing", "vice");

        attempt("notes_delete", delete)
                .flatMap(SandboxApplication::tokenIn)
                .ifPresent(issued ->
                        attempt("notes_delete", with(delete, ToolInvocation.CONFIRM_ARGUMENT, issued)));

        transcript.note("...and a phrase that only matches somebody else's note:");
        attempt("notes_delete", arguments("containing", "lighting"));
    }

    private void aNameThatIsNotThere() {
        transcript.scenario("A name that is not there",
                "A stale tool list or a hallucinated name. Counted rather than merely refused - it is "
                + "otherwise the one failure an operator has no way of seeing.");

        asAssistant();
        attempt("parts_explode", noArguments());
    }

    private void aCallerInALoop() {
        transcript.scenario("A caller in a loop",
                "A separately wired dispatcher whose bucket holds two, over the same catalogue. The "
                + "only guard that applies to reads: every one of these calls is valid, permitted and "
                + "within its ceiling, and nothing else in the chain is looking at the sequence.");

        ToolDispatcher throttled = workshop.dispatcherWith(
                new TokenBucketCallerRateLimiter(2, Duration.ofMinutes(1)));

        asAssistant();

        for (int round = 1; round <= 3; round++) {
            attempt(throttled, "workshops_list", noArguments());
        }
    }

    private void whatTheCatalogueWouldNotStartWith() {
        transcript.heading("What the catalogue would not start with");

        CatalogueRefusals.all().forEach(refusal -> {
            transcript.note("");
            transcript.note(refusal.what() + ":");
            transcript.note("  " + refusal.message());
        });
    }

    private void whatWasRecorded() {
        transcript.heading("What was recorded");
        transcript.trail(trace.entries());
        transcript.totals(trace.outcomeTotals());

        transcript.note("");
        transcript.note("Catalogue: " + catalog.size() + " action(s) across " + catalog.toolNames().size()
                      + " tool(s) - " + String.join(", ", catalog.publishedNames()));
        transcript.note("Inventory: " + inventory.partCount() + " part(s) left, "
                      + inventory.notesOf(SandboxCallers.OWNER).size() + " note(s) for "
                      + SandboxCallers.OWNER);
    }

    // ── Driving one call ─────────────────────────────────────────────────────────

    private Optional<ToolOutcome> attempt(String publishedName, Map<String, Object> arguments) {
        return attempt(dispatcher, publishedName, arguments);
    }

    /**
     * One call, printed either way.
     *
     * <p>A refusal is caught rather than allowed to end the run, because most of what this application
     * is here to show is refusals — and a driver that stopped at the first one would show exactly one.
     */
    private Optional<ToolOutcome> attempt(
            ToolDispatcher through, String publishedName, Map<String, Object> arguments) {

        transcript.calling(callers.resolve().describe(), publishedName, arguments);

        try {
            ToolOutcome outcome = through.dispatch(publishedName, arguments);
            transcript.ran(outcome);

            return Optional.of(outcome);

        } catch (ToolRefusedException refusal) {
            transcript.refused(refusal);

            return Optional.empty();
        }
    }

    private void asAssistant() {
        callers.actAs(SandboxCallers.ASSISTANT_FOR_OWNER);
    }

    private void asPerson() {
        callers.actAs(SandboxCallers.PERSON);
    }

    // ── Arguments, as a client would send them ───────────────────────────────────

    private static Map<String, Object> noArguments() {
        return Map.of();
    }

    /** Pairs rather than a builder: this is what arrives as JSON, and it reads best as what it is. */
    private static Map<String, Object> arguments(Object... pairs) {
        Map<String, Object> arguments = new LinkedHashMap<>();

        for (int index = 0; index < pairs.length; index += 2) {
            arguments.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }

        return arguments;
    }

    private static Map<String, Object> with(Map<String, Object> arguments, String name, Object value) {
        Map<String, Object> extended = new LinkedHashMap<>(arguments);
        extended.put(name, value);

        return extended;
    }

    private static Map<String, Object> anItem(String name, String shelf, int quantity) {
        return arguments("name", name, "shelf", shelf, "quantity", quantity);
    }

    /**
     * The token out of a preview's body.
     *
     * <p>What a model does by reading {@code howToProceed}, and the reason the token is a top-level key
     * of the result rather than a sentence inside it.
     */
    @SuppressWarnings("unchecked")
    private static Optional<String> tokenIn(ToolOutcome outcome) {
        if (!(outcome.payload() instanceof Map<?, ?> body)) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                (String) ((Map<String, Object>) body).get(ToolInvocation.CONFIRM_ARGUMENT));
    }
}
