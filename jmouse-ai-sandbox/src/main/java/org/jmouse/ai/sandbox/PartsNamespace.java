package org.jmouse.ai.sandbox;

/**
 * The one name two features publish under.
 *
 * <p>{@link PartTool} owns what a part <em>is</em> and {@link PartMovementTool} owns where it sits, and
 * a caller should still see one {@code parts} tool rather than two halves it has to reconcile. The case
 * reads like a bug until something exercises it — a catalogue that counted definitions instead of
 * namespaces would report a duplicate registration here, and the split is exactly the arrangement
 * {@link org.jmouse.ai.ToolDefinition} argues for.
 *
 * <p>A constant rather than the literal twice, because the moment the two spellings drift the two
 * features stop being one tool and nothing says so — the catalogue is perfectly happy to publish
 * {@code part_move} beside {@code parts_list}.
 */
public final class PartsNamespace {

    /** Lower case, and plural because that is what the domain says. */
    public static final String NAME = "parts";

    private PartsNamespace() {
    }
}
