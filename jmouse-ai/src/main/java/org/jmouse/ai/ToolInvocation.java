package org.jmouse.ai;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything one handler is given.
 *
 * <p>The arguments arrive as whatever JSON the client sent, so the accessors here read them through
 * {@link Arguments}, which converts and produces refusals a model can act on. The conversion lives
 * there rather than here so that a nested object is read by the same rules, and refused in the same
 * words, as a top-level one.
 *
 * <p><strong>{@code scope} is already resolved.</strong> Handlers never see the scope
 * <em>argument</em>, only the scope it resolved to, so no handler can implement the resolution rules
 * slightly differently — and a handler cannot widen its own reach by reading the argument again.
 *
 * <p><strong>{@code confirmedRecords} is that same idea applied to two-step confirmation.</strong> A
 * handler that re-ran its own filter on the confirming call could touch records the preview never
 * showed; it is handed the resolved, frozen set instead, so it cannot. The preview is a promise about
 * specific records, not an estimate of how many there might be by then.
 *
 * @param caller           who is calling, and whom for
 * @param scope            where this call runs, and whether a default supplied it; {@code null} for an
 *                         action that is not confined to one
 * @param arguments        the raw arguments as sent
 * @param confirmedRecords the exact records the guards resolved, snapshotted; empty when none applied
 */
public record ToolInvocation(
        CallerIdentity          caller,
        InvocationScope         scope,
        Map<String, Object>     arguments,
        List<AffectedRecords.Record> confirmedRecords
) {

    /**
     * The scope argument, in the one place both the schema and the resolver read it.
     *
     * <p>Neutral rather than named after any product's word for a place: a workspace, a persona and a
     * project are the same argument, and a library that called it {@code workspace} would have every
     * other product's model reading a description about something it does not have.
     */
    public static final String SCOPE_ARGUMENT = "scope";

    /** The single-use token that turns a previewed operation into a committed one. */
    public static final String CONFIRM_ARGUMENT = "confirm";

    /** The caller's explicit "yes, really, do that a second time". */
    public static final String ALLOW_DUPLICATE_ARGUMENT = "allowDuplicate";

    /** How many results a listing returns when the caller says nothing. */
    public static final String LIMIT_ARGUMENT = "limit";

    /** The most any listing will ever return, however large a number is asked for. */
    public static final int MAXIMUM_LIMIT = 100;

    /**
     * Arguments that steer the mechanism rather than describe the work.
     *
     * <p>Named as a set because two places need exactly this list and for opposite reasons: the
     * fingerprint drops them so that {@code allowDuplicate} can do its job, and a schema builder adds
     * them so a model knows they exist.
     */
    public static final List<String> RESERVED_ARGUMENTS =
            List.of(SCOPE_ARGUMENT, CONFIRM_ARGUMENT, ALLOW_DUPLICATE_ARGUMENT, LIMIT_ARGUMENT);

    public ToolInvocation {
        arguments        = arguments == null ? Map.of() : arguments;
        confirmedRecords = confirmedRecords == null ? List.of() : List.copyOf(confirmedRecords);
    }

    public ToolInvocation(CallerIdentity caller, InvocationScope scope, Map<String, Object> arguments) {
        this(caller, scope, arguments, List.of());
    }

    /** The same invocation, carrying the record set the guards resolved. */
    public ToolInvocation confirmedFor(List<AffectedRecords.Record> records) {
        return new ToolInvocation(caller, scope, arguments, records);
    }

    /** The identifiers a handler acts on — always the resolved set, never the arguments. */
    public List<String> confirmedRecordIdentifiers() {
        return confirmedRecords.stream().map(AffectedRecords.Record::id).toList();
    }

    /** Who the authorization decision was made against. */
    public String callerId() {
        return caller.callerId();
    }

    /** Whose records are in view — what a handler passes to a domain service. */
    public String actingSubject() {
        return caller.actingSubject();
    }

    /** The scope this call runs in, or null for an action that is not confined to one. */
    public String scopeId() {
        return InvocationScope.identifierOf(scope);
    }

    /** The scope's name, for a refusal that has to say where it was looking. */
    public String scopeLabel() {
        return InvocationScope.labelOf(scope);
    }

    /** The arguments, read as what they were meant to be. */
    public Arguments reader() {
        return Arguments.of(arguments);
    }

    public Optional<String> optionalString(String name) {
        return reader().optionalString(name);
    }

    public String requiredString(String name) {
        return reader().requiredString(name);
    }

    public Optional<Double> optionalNumber(String name) {
        return reader().optionalNumber(name);
    }

    public Optional<LocalDate> optionalDate(String name) {
        return reader().optionalDate(name);
    }

    public int intArgument(String name, int fallback) {
        return optionalNumber(name).map(Double::intValue).orElse(fallback);
    }

    public double decimalArgument(String name, double fallback) {
        return optionalNumber(name).orElse(fallback);
    }

    /**
     * How many results to return, clamped.
     *
     * <p>Here rather than in each tool because every listing wants the same three lines, and a
     * per-tool copy is where one of them eventually forgets the upper bound — which is how a model
     * asking for ten thousand rows gets them.
     */
    public int limitArgument(int fallback) {
        int requested = intArgument(LIMIT_ARGUMENT, fallback);

        if (requested <= 0) {
            return fallback;
        }

        return Math.min(requested, MAXIMUM_LIMIT);
    }

    public boolean flag(String name) {
        return reader().flag(name);
    }

    public List<String> stringList(String name) {
        return reader().stringList(name);
    }

    public Map<String, String> stringMap(String name) {
        return reader().stringMap(name);
    }

    public Map<String, String> requiredStringMap(String name) {
        return reader().requiredStringMap(name);
    }

    public List<Map<String, Object>> objectList(String name) {
        return reader().objectList(name);
    }

    /**
     * The single record this call is about, as the guards resolved it.
     *
     * <p>For the many actions addressing one thing by identifier. The refusal is the same in every
     * domain: the guards resolved nothing, which means the identifier named something this caller
     * cannot reach from here — another scope, another subject, or something already gone. Saying all
     * three is what stops a model retrying the same identifier.
     *
     * @param what               the kind of thing, in the user's words, e.g. {@code "page"}
     * @param verb               what was going to happen to it, e.g. {@code "delete"}
     * @param identifierArgument which argument named it, so the refusal can quote it back
     */
    public String requireConfirmedRecord(String what, String verb, String identifierArgument) {
        return confirmedRecords.stream().findFirst()
                .map(AffectedRecords.Record::id)
                .orElseThrow(() -> new ToolRefusedException(RefusalReason.INVALID_ARGUMENT,
                        "No " + what + " '" + requiredString(identifierArgument) + "' is visible "
                        + (scope == null ? "to this caller" : "in the " + scope.kind() + " '" + scope.label() + "'")
                        + ", so there is nothing to " + verb + ". Use the matching list action to see "
                        + "what is there" + (scope == null ? "." : ", or name another " + scope.kind() + ".")));
    }
}
