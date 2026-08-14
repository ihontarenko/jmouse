package org.jmouse.ai.sandbox;

import org.jmouse.ai.AffectedRecords;
import org.jmouse.ai.ArgumentSchema;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolDefinition;
import org.jmouse.ai.ToolInvocation;

import java.util.List;
import java.util.Map;

/**
 * Records that belong to a subject rather than to a place.
 *
 * <p>Which makes {@code notes.delete} the case the rehearsal found to be legal and unwritten-about: a
 * <strong>destructive action with no scope</strong>. Every refusal on that path has a scopeless half —
 * the empty-destruction guard has nowhere to suggest looking instead, and
 * {@link org.jmouse.ai.ScopeConfinement} has no other place to name — and a sandbox whose every action
 * was confined to a workshop would leave all of them unexercised.
 *
 * <p>⚠️ Both actions read {@link ToolInvocation#actingSubject()}, which is the ordinary shape and the
 * exact opposite of {@link WorkshopTool}. Reading the caller here would show a service credential its
 * own empty set of notes and report that its owner has none.
 */
public final class NoteTool implements ToolDefinition {

    static final String CONTAINING_ARGUMENT = "containing";

    private final WorkshopInventory inventory;

    public NoteTool(WorkshopInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public String toolName() {
        return "notes";
    }

    @Override
    public List<ToolAction> actions() {
        return List.of(list(), delete());
    }

    private ToolAction list() {
        return ToolAction.builder()
                .toolName(toolName())
                .name("list")
                .title("List notes")
                .description("Every note kept by the person this call is acting for. Notes are not "
                           + "filed in a workshop, so this action takes no scope.")
                .inputSchema(ArgumentSchema.builder().limit(20))
                .requiredPermission(SandboxPermissions.NOTES_READ)
                .readOnly()
                .handler(this::listNotes)
                .build();
    }

    private Object listNotes(ToolInvocation invocation) {
        return inventory.notesOf(invocation.actingSubject()).stream()
                .limit(invocation.limitArgument(20))
                .map(NoteTool::describe)
                .toList();
    }

    private ToolAction delete() {
        return ToolAction.builder()
                .toolName(toolName())
                .name("delete")
                .title("Delete notes")
                .description("Removes every note whose text contains a phrase. There is no undo.")
                .inputSchema(ArgumentSchema.builder()
                        .requiredString(CONTAINING_ARGUMENT, "A phrase the note's text contains.")
                        .confirm())
                .requiredPermission(SandboxPermissions.NOTES_WRITE)
                .destructive()
                .traceAttribute("event", "note.delete")
                .affectedRecords(this::selectNotes)
                .handler(this::deleteNotes)
                .build();
    }

    private Object deleteNotes(ToolInvocation invocation) {
        List<String> deleted = invocation.confirmedRecordIdentifiers();
        deleted.forEach(inventory::deleteNote);

        return Map.of("deleted", deleted.size());
    }

    /** Whatever the phrase matches, with the text kept — it is about to be the only copy there was. */
    private AffectedRecords selectNotes(ToolInvocation invocation) {
        String phrase = invocation.requiredString(CONTAINING_ARGUMENT);

        return AffectedRecords.of(inventory.notesOf(invocation.actingSubject()).stream()
                .filter(note -> note.text().toLowerCase().contains(phrase.toLowerCase()))
                .map(note -> AffectedRecords.Record.of(
                        note.id(), note.text(), "note", Map.of("text", note.text())))
                .toList());
    }

    private static Map<String, Object> describe(Note note) {
        return Map.of("id", note.id(), "text", note.text());
    }
}
