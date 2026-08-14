package org.jmouse.ai.sandbox;

import org.jmouse.ai.AffectedRecords;
import org.jmouse.ai.ArgumentSchema;
import org.jmouse.ai.Arguments;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolDefinition;
import org.jmouse.ai.ToolInvocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The four hard cases, one action each.
 *
 * <table>
 *   <caption>What each action is here to exercise</caption>
 *   <tr><th>Action</th><th>The case</th></tr>
 *   <tr><td>{@code list}</td><td>a scope-confined read — resolution, defaulting, and the echo</td></tr>
 *   <tr><td>{@code receive}</td><td>nested object-list arguments, and a write whose reach is not
 *       knowable in advance</td></tr>
 *   <tr><td>{@code restock}</td><td>a write over the confirmation threshold — preview, token,
 *       redemption, frozen record set — and, with a wider filter, the ceiling</td></tr>
 *   <tr><td>{@code discard}</td><td>destructive with a record resolver, {@code previousState} capture,
 *       and — with a filter matching nothing — the empty-destruction refusal</td></tr>
 * </table>
 *
 * <p>Every one of them acts on {@link ToolInvocation#actingSubject()}'s data through a scope resolved
 * from the caller's own visibility. That is the ordinary shape, and {@code workshops.list} is the one
 * that is not.
 */
public final class PartTool implements ToolDefinition {

    static final String SHELF_ARGUMENT = "shelf";

    private final WorkshopInventory inventory;

    public PartTool(WorkshopInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public String toolName() {
        return PartsNamespace.NAME;
    }

    @Override
    public List<ToolAction> actions() {
        return List.of(list(), receive(), restock(), discard());
    }

    // ── A scope-confined read ────────────────────────────────────────────────────

    private ToolAction list() {
        return ToolAction.builder()
                .toolName(toolName())
                .name("list")
                .title("List parts")
                .description("Every part in one workshop, newest shelf order, with quantities.")
                .inputSchema(ArgumentSchema.builder()
                        .scope(Workshop.KIND, "workshops_list")
                        .optionalString(SHELF_ARGUMENT, "Restrict to one shelf, e.g. 'A'.")
                        .limit(20))
                .requiredPermission(SandboxPermissions.PARTS_READ)
                .readOnly()
                .scopeConfined()
                .handler(this::listParts)
                .build();
    }

    private Object listParts(ToolInvocation invocation) {
        String shelf = invocation.optionalString(SHELF_ARGUMENT).orElse(null);

        return inventory.partsMatching(invocation.scopeId(), shelf).stream()
                .limit(invocation.limitArgument(20))
                .map(PartTool::describe)
                .toList();
    }

    // ── Nested object-list arguments ─────────────────────────────────────────────

    /**
     * The stretched case for {@link ArgumentSchema} and {@link Arguments} both.
     *
     * <p>A list of objects, each with its own required members, described in the same words a
     * top-level argument would be — and refused, when it is wrong, with a message naming
     * <em>which</em> element was wrong. Describing this as "a list of objects" and leaving the members
     * to prose is how a model learns the shape by trying it.
     *
     * <p>No {@code affectedRecords}: a create reaches nothing that exists yet, so neither the ceiling
     * nor the confirmation threshold has anything to fire on. Deduplication still applies, which is
     * the point of running it last.
     */
    private ToolAction receive() {
        ArgumentSchema item = ArgumentSchema.builder()
                .requiredString("name",  "What the part is called, e.g. 'M6 bolt'.")
                .requiredString("shelf", "Which shelf it goes on, e.g. 'A'.")
                .requiredNumber("quantity", "How many arrived. Must be positive.");

        return ToolAction.builder()
                .toolName(toolName())
                .name("receive")
                .title("Receive parts")
                .description("Adds newly arrived parts to a workshop. Send one entry per distinct part.")
                .inputSchema(ArgumentSchema.builder()
                        .scope(Workshop.KIND, "workshops_list")
                        .requiredObjectList("items", "The parts that arrived.", item)
                        .allowDuplicate())
                .requiredPermission(SandboxPermissions.PARTS_WRITE)
                .scopeConfined()
                .traceAttribute("event", "part.receive")
                .handler(this::receiveParts)
                .build();
    }

    private Object receiveParts(ToolInvocation invocation) {
        // Each entry already knows it is 'items[2]', which is what makes a refusal about the third one
        // say so. Composing that label by hand is what this sandbox did first, and disliked twice.
        List<Arguments> items = invocation.each("items");

        if (items.isEmpty()) {
            throw invocation.reader().refuse("items", "must hold at least one part");
        }

        // ⚠️ Every entry is read and checked before any of it is written. Validating as it went would
        // refuse the third entry having already created the first two — and every refusal in this
        // library promises that nothing was changed.
        List<Arrival> arrivals = new ArrayList<>();

        for (Arguments entry : items) {
            int quantity = (int) entry.requiredNumber("quantity");

            if (quantity <= 0) {
                throw entry.refuse("quantity", "must be a positive number, but " + quantity + " was sent");
            }

            arrivals.add(new Arrival(entry.requiredString("shelf"), entry.requiredString("name"), quantity));
        }

        List<Map<String, Object>> received = arrivals.stream()
                .map(arrival -> describe(inventory.addPart(
                        invocation.scopeId(), arrival.shelf(), arrival.name(), arrival.quantity())))
                .toList();

        return Map.of("received", received);
    }

    /** One checked entry, waiting for every other entry to be checked too. */
    private record Arrival(String shelf, String name, int quantity) {
    }

    // ── A write over the confirmation threshold, and the ceiling ─────────────────

    private ToolAction restock() {
        return ToolAction.builder()
                .toolName(toolName())
                .name("restock")
                .title("Restock parts")
                .description("Adds the same quantity to every part matching a shelf. Omit the shelf to "
                           + "reach the whole workshop — which is usually more than a ceiling allows.")
                .inputSchema(ArgumentSchema.builder()
                        .scope(Workshop.KIND, "workshops_list")
                        .optionalString(SHELF_ARGUMENT, "Restrict to one shelf, e.g. 'A'.")
                        .requiredNumber("by", "How many to add to each matching part.")
                        .confirm())
                .requiredPermission(SandboxPermissions.PARTS_WRITE)
                .scopeConfined()
                .traceAttribute("event", "part.restock")
                .affectedRecords(this::selectParts)
                .handler(this::restockParts)
                .build();
    }

    private Object restockParts(ToolInvocation invocation) {
        int by = (int) invocation.reader().requiredNumber("by");

        // The frozen set, never the filter. A handler that re-ran its own query on the confirming call
        // could touch records the preview never showed.
        List<String> restocked = invocation.confirmedRecordIdentifiers();
        restocked.forEach(partId -> inventory.adjustQuantity(partId, by));

        return Map.of("restocked", restocked.size(), "by", by);
    }

    // ── Destructive, with a resolver and previous state ──────────────────────────

    private ToolAction discard() {
        return ToolAction.builder()
                .toolName(toolName())
                .name("discard")
                .title("Discard parts")
                .description("Removes every part matching a shelf. There is no undo.")
                .inputSchema(ArgumentSchema.builder()
                        .scope(Workshop.KIND, "workshops_list")
                        .requiredString(SHELF_ARGUMENT, "Which shelf to clear, e.g. 'B'.")
                        .confirm())
                .requiredPermission(SandboxPermissions.PARTS_DELETE)
                .destructive()
                .scopeConfined()
                .traceAttribute("event", "part.discard")
                .affectedRecords(this::selectParts)
                .handler(this::discardParts)
                .build();
    }

    private Object discardParts(ToolInvocation invocation) {
        List<String> discarded = invocation.confirmedRecordIdentifiers();
        discarded.forEach(inventory::discard);

        return Map.of("discarded", discarded.size());
    }

    // ── What both writes would touch ─────────────────────────────────────────────

    /**
     * Resolved once, before the work, feeding the ceiling, the empty-destruction check and the preview.
     *
     * <p>Captures each part's state here rather than at the point of removal, because here is where the
     * rows are already loaded — and because by the time the work runs, the only honest moment to have
     * read them has passed.
     */
    private AffectedRecords selectParts(ToolInvocation invocation) {
        String shelf = invocation.optionalString(SHELF_ARGUMENT).orElse(null);

        return AffectedRecords.of(inventory.partsMatching(invocation.scopeId(), shelf).stream()
                .map(part -> AffectedRecords.Record.of(
                        part.id(),
                        part.name() + " (shelf " + part.shelf() + ", " + part.quantity() + ")",
                        "part",
                        part.state()))
                .toList());
    }

    static Map<String, Object> describe(Part part) {
        Map<String, Object> described = new LinkedHashMap<>();

        described.put("id",       part.id());
        described.put("name",     part.name());
        described.put("shelf",    part.shelf());
        described.put("quantity", part.quantity());

        return described;
    }
}
