package org.jmouse.ai.sandbox;

import org.jmouse.ai.AffectedRecords;
import org.jmouse.ai.ArgumentSchema;
import org.jmouse.ai.ScopeConfinement;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolDefinition;
import org.jmouse.ai.ToolInvocation;

import java.util.List;
import java.util.Optional;

/**
 * The second half of one namespace, and the single-record shape.
 *
 * <p>Two things at once, both of which a sandbox with one part definition would assert rather than
 * exercise. It contributes {@code parts.move} to {@link PartsNamespace#NAME} — which
 * {@link PartTool} also contributes to — so the split-namespace case is real rather than described.
 * And it is the only write here that names <strong>one</strong> record, which is where the two refusals
 * every such action needs become reachable: an identifier that exists somewhere this call may not look,
 * and an identifier that exists nowhere.
 *
 * <p>⚠️ Those two are answered in different places on purpose. The first is
 * {@link ScopeConfinement}, raised from the resolver, because a part in another workshop is a boundary
 * question and the answer names the boundary. The second is
 * {@link ToolInvocation#requireConfirmedRecord}, raised from the handler, because nothing resolved and
 * the honest answer is that the identifier reached nothing — a distinction a single "not found" would
 * lose, along with the only advice that would have helped.
 *
 * <p>A write below every threshold, so it also covers the case the guarded paths make easy to forget:
 * a write that simply happens.
 */
public final class PartMovementTool implements ToolDefinition {

    static final String PART_ARGUMENT  = "partId";
    static final String SHELF_ARGUMENT = "toShelf";

    private final WorkshopInventory inventory;

    public PartMovementTool(WorkshopInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public String toolName() {
        return PartsNamespace.NAME;
    }

    @Override
    public List<ToolAction> actions() {
        return List.of(move());
    }

    private ToolAction move() {
        return ToolAction.builder()
                .toolName(toolName())
                .name("move")
                .title("Move a part")
                .description("Puts one part on a different shelf in the same workshop. Use 'parts_list' "
                           + "to find the identifier.")
                .inputSchema(ArgumentSchema.builder()
                        .scope(Workshop.KIND, "workshops_list")
                        .requiredString(PART_ARGUMENT,  "Which part to move, by identifier.")
                        .requiredString(SHELF_ARGUMENT, "Which shelf it goes on, e.g. 'C'."))
                .requiredPermission(SandboxPermissions.PARTS_WRITE)
                .scopeConfined()
                .traceAttribute("event", "part.move")
                .affectedRecords(this::selectPart)
                .handler(this::movePart)
                .build();
    }

    private Object movePart(ToolInvocation invocation) {
        String partId = invocation.requireConfirmedRecord("part", "move", PART_ARGUMENT);
        String shelf  = invocation.requiredString(SHELF_ARGUMENT);

        inventory.moveToShelf(partId, shelf);

        return inventory.part(partId).map(PartTool::describe).orElseThrow();
    }

    /**
     * The named part, if this call is entitled to look at it.
     *
     * <p>Answers with nothing rather than refusing when the identifier matches no part at all — the
     * handler says that better, in the one sentence that also says what to do about it.
     */
    private AffectedRecords selectPart(ToolInvocation invocation) {
        String         partId = invocation.requiredString(PART_ARGUMENT);
        Optional<Part> found  = inventory.part(partId);

        if (found.isEmpty()) {
            return AffectedRecords.none();
        }

        Part part = found.get();

        ScopeConfinement.require(
                invocation, part.workshopId().equals(invocation.scopeId()), "part", partId);

        return AffectedRecords.of(List.of(
                AffectedRecords.Record.of(part.id(), describe(part), "part")));
    }

    private static String describe(Part part) {
        return part.name() + " (shelf " + part.shelf() + ")";
    }
}
