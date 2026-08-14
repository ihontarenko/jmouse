package org.jmouse.ai;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * One thing that can be done.
 *
 * <p><strong>The action, not the tool, is the unit of the catalogue.</strong> A tool is only a
 * namespace over a domain; everything that varies — the arguments, the permission it costs, whether it
 * changes anything, whether it can say what it would touch — varies per action. Modelling the tool as
 * the unit would force one permission to cover reading and deleting alike, which is the shape of every
 * over-broad credential ever issued.
 *
 * <p>{@link #requiredPermission()} is <strong>mandatory and has no default</strong>, and
 * {@link ToolCatalog} refuses to start when one is missing. That is not defensiveness. A handler
 * reaches a domain service directly, bypassing whatever authorization a product hangs on its HTTP
 * layer, which makes the dispatcher's check the only one that runs on this path — so a defaulted
 * permission would silently register an unguarded action. Failing to start is loud; an unguarded
 * action is silent.
 *
 * <p>The library never interprets the permission. The catalogue insists it is present and, where a
 * {@link org.jmouse.ai.spi.PermissionVocabulary} is supplied, that it exists; what it <em>means</em> is
 * {@link org.jmouse.ai.spi.ToolAuthorizer}'s. That split is what keeps this module free of any
 * authorization engine while losing nothing that matters.
 *
 * <p>Built through {@link #builder()} rather than constructed directly, because the schema may only
 * arrive as an {@link ArgumentSchema} — see that class for why a hand-assembled map is refused a door.
 *
 * @param toolName           the namespace this action belongs to, e.g. {@code entries}
 * @param name               the action within it, e.g. {@code list}
 * @param title              a short label for a client's tool picker
 * @param description        what it does, written for a model deciding whether to call it
 * @param inputSchema        JSON Schema for the arguments, as the shape a transport publishes
 * @param requiredPermission what the caller must hold; never null, never blank
 * @param readOnly           true when the action cannot change anything — the guards read this
 * @param destructive        true when it removes or overwrites data that has no other copy
 * @param scopeConfined      whether it runs inside one scope, and so accepts and echoes one
 * @param origin             local, or forwarded to a server this application connected to
 * @param traceAttributes    whatever a product's trace wants to know about this action, carried and
 *                           never read — see below
 * @param affectedRecords    resolves which existing records this call would touch, before it touches
 *                           them; null for an action whose reach is not knowable in advance, such as a
 *                           create. <strong>Mandatory for a destructive action</strong>
 * @param handler            the work itself
 */
public record ToolAction(
        String                                     toolName,
        String                                     name,
        String                                     title,
        String                                     description,
        Map<String, Object>                        inputSchema,
        String                                     requiredPermission,
        boolean                                    readOnly,
        boolean                                    destructive,
        boolean                                    scopeConfined,
        ToolOrigin                                 origin,
        Map<String, String>                        traceAttributes,
        Function<ToolInvocation, AffectedRecords>  affectedRecords,
        Function<ToolInvocation, Object>           handler
) {

    /** What separates the namespace from the action on the wire, where the dot is not allowed. */
    private static final String PUBLISHED_SEPARATOR = "_";

    /** What separates them everywhere a person or a model reads them. */
    private static final String QUALIFIED_SEPARATOR = ".";

    public ToolAction {
        traceAttributes = traceAttributes == null ? Map.of() : Map.copyOf(traceAttributes);
        origin          = origin == null ? ToolOrigin.LOCAL : origin;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The name this action is published under.
     *
     * <p>Joined with an underscore rather than the dot the domain vocabulary uses, because the Model
     * Context Protocol constrains tool names to {@code [a-zA-Z0-9_-]}. The dotted form stays the name
     * in conversation, tickets and logs; this is only the wire spelling.
     *
     * <p>Callers never take it apart again — the catalogue resolves it by exact lookup, so an action
     * name containing an underscore cannot be mis-parsed back into the wrong tool.
     */
    public String publishedName() {
        return toolName + PUBLISHED_SEPARATOR + name;
    }

    /** How the action is referred to in refusals, log lines and conversation. */
    public String qualifiedName() {
        return toolName + QUALIFIED_SEPARATOR + name;
    }

    /** What a transport is allowed to see: everything above, and never the handler. */
    public PublishedTool published() {
        return new PublishedTool(
                publishedName(), qualifiedName(), title, description, inputSchema,
                requiredPermission, readOnly, destructive, scopeConfined, origin);
    }

    /** Whether this call could ever need a preview — anything that is not a read. */
    public boolean writes() {
        return !readOnly;
    }

    /** The records this call would touch, or none where the action cannot say in advance. */
    public AffectedRecords resolveAffected(ToolInvocation invocation) {
        return affectedRecords == null ? AffectedRecords.none() : affectedRecords.apply(invocation);
    }

    /**
     * Assembles one action, and is the only way to make one.
     *
     * <p>Hand-written rather than generated, for one reason worth the lines: {@link #inputSchema} takes
     * an {@link ArgumentSchema} and nothing else. A generated builder would take the record's
     * {@code Map} and quietly reopen the door {@link ArgumentSchema} exists to close.
     */
    public static final class Builder {

        private String                                    toolName;
        private String                                    name;
        private String                                    title;
        private String                                    description;
        private Map<String, Object>                       inputSchema;
        private String                                    requiredPermission;
        private boolean                                   readOnly;
        private boolean                                   destructive;
        private boolean                                   scopeConfined;
        private ToolOrigin                                origin = ToolOrigin.LOCAL;
        private final Map<String, String>                 traceAttributes = new LinkedHashMap<>();
        private Function<ToolInvocation, AffectedRecords> affectedRecords;
        private Function<ToolInvocation, Object>          handler;

        private Builder() {
        }

        public Builder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /** The arguments, built rather than assembled. See {@link ArgumentSchema}. */
        public Builder inputSchema(ArgumentSchema schema) {
            this.inputSchema = schema == null ? null : schema.build();
            return this;
        }

        public Builder requiredPermission(String requiredPermission) {
            this.requiredPermission = requiredPermission;
            return this;
        }

        /** An action that cannot change anything. Only the rate limit applies to it. */
        public Builder readOnly() {
            this.readOnly = true;
            return this;
        }

        /**
         * An action that removes or overwrites data with no other copy.
         *
         * <p>Says "removes or overwrites" rather than "deletes", because a destructive action is not
         * always a delete — rewriting a document replaces prose that has no second copy, which is the
         * same loss under a gentler name. Every such call is confirmed, however few records it
         * touches: one is exactly as irreversible as forty.
         *
         * <p>⚠️ Requires {@link #affectedRecords}, and the catalogue refuses to start without it.
         */
        public Builder destructive() {
            this.destructive  = true;
            this.readOnly     = false;
            return this;
        }

        /** An action that runs inside one scope, accepts one as an argument, and echoes it back. */
        public Builder scopeConfined() {
            this.scopeConfined = true;
            return this;
        }

        /** Set by a client registering a server's actions; local is the default and needs no call. */
        public Builder origin(ToolOrigin origin) {
            this.origin = origin;
            return this;
        }

        /**
         * Something a product's trace wants to know about this action.
         *
         * <p>Opaque: the mechanism carries these and never reads one. That is what lets a product
         * record a tool call and a human action as the <em>same</em> event in its own vocabulary —
         * "every record created this week" stays one query rather than two that have to be reconciled
         * — without the library learning what an audit action is.
         *
         * <p>⚠️ The cost is stated where it can be seen: a product's typed constants become strings
         * here, so a product that cares should build this map through a helper of its own rather than
         * writing the keys at each call site.
         */
        public Builder traceAttribute(String key, String value) {
            this.traceAttributes.put(key, value);
            return this;
        }

        public Builder traceAttributes(Map<String, String> attributes) {
            this.traceAttributes.putAll(attributes);
            return this;
        }

        /**
         * How this call says what it would touch, before it touches it.
         *
         * <p>Feeds the ceiling, the empty-destruction check and the preview. Left out by a create,
         * whose reach is not knowable in advance; mandatory for anything destructive, because a
         * preview is the whole of that protection and one showing nothing reads as "ready to proceed".
         */
        public Builder affectedRecords(Function<ToolInvocation, AffectedRecords> affectedRecords) {
            this.affectedRecords = affectedRecords;
            return this;
        }

        public Builder handler(Function<ToolInvocation, Object> handler) {
            this.handler = handler;
            return this;
        }

        /**
         * Assembles the action without validating it.
         *
         * <p>Validation is {@link ToolCatalog}'s, deliberately: a definition is checked against the
         * whole registered set — for a duplicated name, for a permission the vocabulary does not hold —
         * and half of those questions cannot be asked of one action alone. Checking what can be checked
         * here would split the answer across two places and leave a reader unsure which one to trust.
         */
        public ToolAction build() {
            return new ToolAction(
                    toolName, name, title, description, inputSchema, requiredPermission,
                    readOnly, destructive, scopeConfined, origin, traceAttributes,
                    affectedRecords, handler);
        }
    }
}
