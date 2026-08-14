package org.jmouse.ai.sandbox;

import org.jmouse.ai.ArgumentSchema;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolDefinition;
import org.jmouse.ai.ToolInvocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The trivial path: read-only, no scope, no arguments.
 *
 * <p>Proves nothing is mandatory that should not be — an action can exist with a permission, a schema
 * with no properties in it, and nothing else.
 *
 * <p>⚠️ And it is the one action that reads {@link ToolInvocation#callerId()} rather than
 * {@link ToolInvocation#actingSubject()}, because <em>"which workshops can I reach"</em> is a question
 * about the caller. In the implementation this library learned from that was a comment naming "the one
 * deliberate exception"; here it is one accessor chosen over another, which is what the rehearsal
 * concluded the exception had always been.
 */
public final class WorkshopTool implements ToolDefinition {

    private final WorkshopInventory inventory;

    public WorkshopTool(WorkshopInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public String toolName() {
        return "workshops";
    }

    @Override
    public List<ToolAction> actions() {
        return List.of(list());
    }

    private ToolAction list() {
        return ToolAction.builder()
                .toolName(toolName())
                .name("list")
                .title("List workshops")
                .description("Every workshop this caller can act in. Call this first to learn the names "
                           + "the 'scope' argument of other actions expects.")
                .inputSchema(ArgumentSchema.none())
                .requiredPermission(SandboxPermissions.WORKSHOPS_READ)
                .readOnly()
                .handler(this::describeVisible)
                .build();
    }

    private Object describeVisible(ToolInvocation invocation) {
        return inventory.workshopsVisibleTo(invocation.callerId()).stream()
                .map(WorkshopTool::describe)
                .toList();
    }

    private static Map<String, Object> describe(Workshop workshop) {
        Map<String, Object> described = new LinkedHashMap<>();

        described.put("id",   workshop.id());
        described.put("name", workshop.name());

        return described;
    }
}
