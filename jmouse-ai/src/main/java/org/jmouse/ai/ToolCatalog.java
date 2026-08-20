package org.jmouse.ai;

import org.jmouse.ai.spi.PermissionVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Everything a caller can reach, and the gate that guarantees each of it is guarded.
 *
 * <p>Collects {@link ToolDefinition}s, vets the whole set, and is then read-only forever. What it
 * collects it does not own: every definition is declared by the feature whose capability it exposes,
 * so this class knows how many actions there are and that each one is guarded, and does not know a
 * single feature's name.
 *
 * <p><strong>It publishes {@link PublishedTool}, never {@link ToolAction}.</strong> The handler is
 * reachable only from {@link ToolDispatcher}, through a package-private door — which is what turns
 * <em>"there is no second path into an action"</em> from a claim into a fact a compiler enforces. See
 * {@link PublishedTool} for why that is worth a type of its own.
 *
 * <p><strong>Construction is validation, and failing it is loud.</strong> Seven conditions stop an
 * application from starting, and every one of them describes something whose alternative is silent:
 * an action nothing would ever check, a permission nobody can hold, a name that shadows another, a
 * tool a client can see and cannot use, a deletion that cannot say what it would delete, a name a
 * protocol will reject at connect time far from its cause, and a guard a product believes it has.
 * Silence is what makes each of these expensive; a refused startup is the cheapest possible version
 * of every one.
 *
 * <p>⚠️ <strong>Nothing is registered until the whole set passes.</strong> A check that failed halfway
 * through registration would leave a partly populated catalogue behind for the rest of the
 * application to read, which is worse than either outcome it was choosing between.
 *
 * <p>What is deliberately <em>not</em> checked: whether an action's permission matches what a
 * product's own HTTP layer requires for the same operation. Divergence there is possible and
 * accepted — the two are separate declarations about separate paths, and a library cannot know one of
 * them. Omission is what is made impossible here.
 */
public final class ToolCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolCatalog.class);

    /**
     * What the Model Context Protocol accepts as a tool name.
     *
     * <p>Checked here so a name that violates it fails beside the action that declared it, rather than
     * as a client-side rejection at connect time — which is far from the cause and reads as a broken
     * server.
     */
    private static final Pattern PUBLISHED_NAME = Pattern.compile("[a-zA-Z0-9_-]{1,64}");

    /** Published name to action. Populated once at construction and never written again. */
    private final Map<String, ToolAction> actionsByPublishedName;

    private ToolCatalog(Map<String, ToolAction> actionsByPublishedName) {
        this.actionsByPublishedName = Map.copyOf(actionsByPublishedName);
    }

    /** A catalogue for a product that has neither a permission vocabulary nor named guards. */
    public static ToolCatalog of(Collection<ToolDefinition> definitions) {
        return of(definitions, PermissionVocabulary.unchecked(), GuardRoster.unchecked());
    }

    public static ToolCatalog of(
            Collection<ToolDefinition> definitions, PermissionVocabulary vocabulary) {
        return of(definitions, vocabulary, GuardRoster.unchecked());
    }

    /**
     * Vets every contributed action and, only if all of them pass, publishes them.
     *
     * @throws IllegalStateException naming what is wrong <em>and what would have been accepted</em> —
     *                               these are read by a person at 2am, and by a model at every other hour
     */
    public static ToolCatalog of(
            Collection<ToolDefinition> definitions,
            PermissionVocabulary       vocabulary,
            GuardRoster                guards) {

        // Namespaces alphabetically, actions in the order their feature declares them — a tool reads
        // best with its listing actions before its writing ones, which no sort would reproduce.
        List<ToolAction> actions = definitions.stream()
                .sorted(Comparator.comparing(ToolDefinition::toolName))
                .flatMap(definition -> definition.actions().stream())
                .toList();

        refuseMissingPermissions(actions);
        refuseUnknownPermissions(actions, vocabulary);
        refuseDuplicateNames(actions);
        refuseIncompleteDefinitions(actions);
        refuseUnpreviewableDestruction(actions);
        refuseUnpublishableNames(actions);
        refuseMissingGuards(guards);

        Map<String, ToolAction> registered = new LinkedHashMap<>();
        actions.forEach(action -> registered.put(action.publishedName(), action));

        // Namespaces rather than definitions: one namespace contributed by two features is one tool,
        // and reporting the bean count would read as a duplicate registration rather than as a split.
        long namespaces = actions.stream().map(ToolAction::toolName).distinct().count();

        LOGGER.info("Tool catalogue: {} action(s) across {} tool(s) — {}",
                actions.size(), namespaces,
                actions.stream().map(ToolAction::qualifiedName).collect(Collectors.joining(", ")));

        return new ToolCatalog(registered);
    }

    // ── What a transport may read ────────────────────────────────────────────────

    /** Every action, in a stable order, as much of it as a transport is allowed to see. */
    public List<PublishedTool> published() {
        return actionsByPublishedName.values().stream().map(ToolAction::published).toList();
    }

    public Optional<PublishedTool> findPublished(String publishedName) {
        return Optional.ofNullable(actionsByPublishedName.get(publishedName)).map(ToolAction::published);
    }

    /** Action names as a client sees them, sorted, for the "no such action" refusal. */
    public List<String> publishedNames() {
        return actionsByPublishedName.keySet().stream().sorted().toList();
    }

    /**
     * The namespaces, which is how many <em>tools</em> there are.
     *
     * <p>Not derivable from {@link #size()}, and the difference is the whole point of a namespace being
     * contributable by two features: eight actions may be three tools. Anything reporting the catalogue
     * to a person needs both numbers, and this was otherwise computable only by the startup log line
     * that already does it.
     */
    public List<String> toolNames() {
        return actionsByPublishedName.values().stream()
                .map(ToolAction::toolName)
                .distinct()
                .sorted()
                .toList();
    }

    public boolean contains(String publishedName) {
        return actionsByPublishedName.containsKey(publishedName);
    }

    public int size() {
        return actionsByPublishedName.size();
    }

    // ── The one door to a handler ────────────────────────────────────────────────

    /**
     * The action behind a published name, handler included.
     *
     * <p>⚠️ <strong>Package-private, and that is the whole design.</strong> {@link ToolDispatcher} is
     * the only type in this package that calls it, so every path to a handler passes the identity,
     * permission, scope and guard steps. Widening this to public would cost nothing at the moment it
     * was done and everything the first time a transport found it convenient.
     */
    Optional<ToolAction> find(String publishedName) {
        return Optional.ofNullable(actionsByPublishedName.get(publishedName));
    }

    /**
     * Every action in registration order, handler included — for the one caller that has to weigh them.
     *
     * <p>⚠️ <strong>Package-private for the same reason {@link #find} is.</strong> {@link
     * ToolDispatcher} asks this to answer "what may <em>this</em> caller reach", which needs the
     * {@link ToolAction} a {@link org.jmouse.ai.spi.ToolAuthorizer} is asked about and which {@link #published()}
     * deliberately does not carry. Widening it would put a handler within reach of anything that
     * wanted to enumerate the catalogue.
     */
    List<ToolAction> actions() {
        return List.copyOf(actionsByPublishedName.values());
    }

    // ── The seven refusals ───────────────────────────────────────────────────────

    /**
     * 1. An action without a required permission is not a configuration mistake to be logged — it is
     * an action nothing will ever check.
     */
    private static void refuseMissingPermissions(List<ToolAction> actions) {
        List<String> unguarded = actions.stream()
                .filter(action -> action.requiredPermission() == null
                               || action.requiredPermission().isBlank())
                .map(ToolAction::qualifiedName)
                .sorted()
                .toList();

        if (!unguarded.isEmpty()) {
            throw new IllegalStateException(
                    "These actions declare no required permission: " + String.join(", ", unguarded)
                    + ". A handler reaches a domain service directly, so the dispatcher's permission "
                    + "check is the only authorization on this path and an action without one would be "
                    + "reachable by any caller. Give each one a permission name.");
        }
    }

    /**
     * 2. A permission nobody can hold makes the action permanently unreachable, and it reads as a
     * broken tool rather than as a typo.
     *
     * <p>Skipped where no vocabulary was supplied — see {@link PermissionVocabulary#unchecked()} for
     * why "I cannot enumerate them" must not be treated as "none of them exist".
     */
    private static void refuseUnknownPermissions(
            List<ToolAction> actions, PermissionVocabulary vocabulary) {

        Set<String> known = vocabulary.all();

        if (known.isEmpty()) {
            return;
        }

        List<ToolAction> unreachable = actions.stream()
                .filter(action -> !known.contains(action.requiredPermission()))
                .sorted(Comparator.comparing(ToolAction::publishedName))
                .toList();

        if (unreachable.isEmpty()) {
            return;
        }

        // ⚠️ Told apart, because the two have opposite fixes and one of them cannot be a typo. An
        // action that declared nothing got `tool:<published name>` derived from the name it already
        // has — so the name is right by construction and what is missing is the DECLARATION. An action
        // that asked for something explicitly may genuinely have misspelled it.
        List<ToolAction> undeclared = unreachable.stream()
                .filter(action -> action.requiredPermission().equals(ToolPermissions.of(action)))
                .toList();

        List<ToolAction> misnamed = unreachable.stream()
                .filter(action -> !undeclared.contains(action))
                .toList();

        StringBuilder complaint = new StringBuilder(
                "Every one of these actions asks for a permission this installation does not have, so no "
                + "caller could ever hold it and the action would be published and permanently "
                + "unreachable. Nothing is started.\n");

        if (!undeclared.isEmpty()) {
            complaint.append("\n  NOT DECLARED IN THE POLICY — this is almost certainly a forgotten line:\n");
            undeclared.forEach(action -> complaint
                    .append("    ").append(action.qualifiedName())
                    .append("  needs  ").append(action.requiredPermission()).append('\n'));
            complaint.append("  These derive their permission from their own name, so it cannot be "
                           + "misspelled — it is simply not declared anywhere. Add one line per action "
                           + "to a 'declare permissions' block in this application's policy, e.g.\n")
                    .append("        ").append(undeclared.getFirst().requiredPermission())
                    .append("  \"").append(undeclared.getFirst().title()).append("\"\n");
        }

        if (!misnamed.isEmpty()) {
            complaint.append("\n  ASKED FOR EXPLICITLY AND NOT FOUND — either a typo, or the "
                           + "declaration was removed and the call was not:\n");
            misnamed.forEach(action -> complaint
                    .append("    ").append(action.qualifiedName())
                    .append("  asks for  ").append(action.requiredPermission())
                    .append("  (dropping requiredPermission would derive ")
                    .append(ToolPermissions.of(action)).append(")\n"));
        }

        complaint.append("\nThe vocabulary this installation knows holds ")
                .append(known.size()).append(" name(s).");

        throw new IllegalStateException(complaint.toString());
    }

    /** 3. One would shadow the other, and which one wins would depend on the order beans arrived in. */
    private static void refuseDuplicateNames(List<ToolAction> actions) {
        Map<String, Long> counts = actions.stream()
                .collect(Collectors.groupingBy(ToolAction::publishedName, Collectors.counting()));

        List<String> duplicated = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        if (!duplicated.isEmpty()) {
            throw new IllegalStateException(
                    "These action names are declared more than once: " + String.join(", ", duplicated)
                    + ". One would shadow the other, and which one wins would depend on bean ordering. "
                    + "Two definitions may share a namespace — that is supported deliberately — but not "
                    + "an action name within it.");
        }
    }

    /** 4. A schema, handler or description left out produces a tool a client can see and cannot use. */
    private static void refuseIncompleteDefinitions(List<ToolAction> actions) {
        List<String> incomplete = new ArrayList<>();

        actions.forEach(action -> {
            if (action.toolName() == null || action.toolName().isBlank()) {
                incomplete.add(action.name() + " (no tool name)");
            }
            if (action.name() == null || action.name().isBlank()) {
                incomplete.add(action.toolName() + " (an action with no name)");
            }
            if (action.inputSchema() == null) {
                incomplete.add(action.qualifiedName() + " (no input schema — use ArgumentSchema.none() "
                             + "for an action that takes no arguments)");
            }
            if (action.handler() == null) {
                incomplete.add(action.qualifiedName() + " (no handler)");
            }
            if (action.description() == null || action.description().isBlank()) {
                incomplete.add(action.qualifiedName() + " (no description — a model decides whether to "
                             + "call an action by reading it)");
            }
        });

        if (!incomplete.isEmpty()) {
            throw new IllegalStateException(
                    "These actions are incompletely defined: " + String.join(", ", incomplete) + ".");
        }
    }

    /**
     * 5. A destructive action must be able to say what it would destroy.
     *
     * <p>Two-step confirmation is the whole of the protection against a model removing the wrong
     * thing, and it works by making the model look at a resolved list of records. An action marked
     * destructive with no way to resolve that list would confirm an empty preview — which reads to a
     * user as "nothing will be affected" immediately before something is.
     */
    private static void refuseUnpreviewableDestruction(List<ToolAction> actions) {
        List<String> unpreviewable = actions.stream()
                .filter(ToolAction::destructive)
                .filter(action -> action.affectedRecords() == null)
                .map(ToolAction::qualifiedName)
                .sorted()
                .toList();

        if (!unpreviewable.isEmpty()) {
            throw new IllegalStateException(
                    "These actions are destructive but cannot resolve what they would affect: "
                    + String.join(", ", unpreviewable)
                    + ". Confirmation would show an empty preview and then destroy something. Give each "
                    + "an affectedRecords resolver, or drop the destructive flag if it does not remove "
                    + "or overwrite anything.");
        }
    }

    /**
     * 6. A published name a protocol will not accept.
     *
     * <p>Left unchecked, this surfaces as a client-side rejection at connect time — a whole
     * conversation away from the action that caused it, and reading as a broken server rather than as
     * one character in one declaration.
     */
    private static void refuseUnpublishableNames(List<ToolAction> actions) {
        List<String> unpublishable = actions.stream()
                .map(ToolAction::publishedName)
                .filter(name -> !PUBLISHED_NAME.matcher(name).matches())
                .sorted()
                .toList();

        if (!unpublishable.isEmpty()) {
            throw new IllegalStateException(
                    "These published names are not legal tool names: " + String.join(", ", unpublishable)
                    + ". A name is the tool and the action joined with an underscore and must match "
                    + PUBLISHED_NAME.pattern() + " — letters, digits, underscore and hyphen, at most 64 "
                    + "characters. A client would reject the whole tool list at connect time.");
        }
    }

    /** 7. A guard named in configuration with nothing behind it. */
    private static void refuseMissingGuards(GuardRoster guards) {
        List<String> missing = guards.missing();

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "These guards were configured and do not exist: " + String.join(", ", missing)
                    + ". The application would run believing it has protection it does not — "
                    + guards.describeAvailable() + ".");
        }
    }
}
