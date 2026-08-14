package org.jmouse.ai.sandbox;

import org.jmouse.ai.ArgumentSchema;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolCatalog;
import org.jmouse.ai.ToolDefinition;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The definitions that must stop an application from starting.
 *
 * <p>Every one of {@link ToolCatalog}'s checks is an argument that <em>silence is the expensive
 * outcome</em>, and an argument of that kind is worth exactly as much as a demonstration. These are the
 * two whose absence is hardest to notice at runtime: a permission nobody can hold, which reads as a
 * broken tool rather than as a typo, and a deletion that cannot say what it would delete, which
 * confirms an empty preview immediately before destroying something.
 *
 * <p>Kept beside the working definitions rather than in a scratch branch nobody would run again. A
 * check that has never been seen to fire is a check nobody has any reason to believe in, and the
 * refusal messages are half of what is being tested — they are read at 2am and by a model at every
 * other hour.
 */
public final class CatalogueRefusals {

    private CatalogueRefusals() {
    }

    /**
     * A permission the vocabulary does not hold — {@code parts:raed}.
     *
     * <p>One transposed pair, and without this check the action starts cleanly, publishes itself to
     * every client, and is refused for every caller forever.
     */
    public static String misspelledPermission() {
        return refusalFrom(() -> ToolCatalog.of(
                List.of(definitionOf(ToolAction.builder()
                        .toolName(PartsNamespace.NAME)
                        .name("audit")
                        .title("Audit parts")
                        .description("Never reachable — its permission is one transposed pair away from "
                                   + "one that exists.")
                        .inputSchema(ArgumentSchema.none())
                        .requiredPermission("parts:raed")
                        .readOnly()
                        .handler(invocation -> List.of())
                        .build())),
                new SandboxPermissions()));
    }

    /** Destructive, with no way to resolve what it would destroy. */
    public static String destructionWithNothingToPreview() {
        return refusalFrom(() -> ToolCatalog.of(
                List.of(definitionOf(ToolAction.builder()
                        .toolName("notes")
                        .name("purge")
                        .title("Purge notes")
                        .description("Removes everything, and cannot say what everything is.")
                        .inputSchema(ArgumentSchema.builder().confirm())
                        .requiredPermission(SandboxPermissions.NOTES_WRITE)
                        .destructive()
                        .handler(invocation -> Map.of("purged", 0))
                        .build())),
                new SandboxPermissions()));
    }

    /**
     * The refusal, or the sentence that says there was not one.
     *
     * <p>A catalogue that accepted any of these is a finding rather than an exception, and it belongs in
     * the transcript where it will be read — not thrown from a driver whose remaining scenarios would
     * then never run.
     */
    private static String refusalFrom(Supplier<ToolCatalog> catalogue) {
        try {
            catalogue.get();
            return "NOT REFUSED — the catalogue accepted this, which it must not.";
        } catch (IllegalStateException refusal) {
            return refusal.getMessage();
        }
    }

    private static ToolDefinition definitionOf(ToolAction action) {
        return new ToolDefinition() {

            @Override
            public String toolName() {
                return action.toolName();
            }

            @Override
            public List<ToolAction> actions() {
                return List.of(action);
            }
        };
    }

    /** Each demonstration, named by what is wrong with it. */
    public static List<Refusal> all() {
        return List.of(
                new Refusal("a permission the vocabulary does not hold", misspelledPermission()),
                new Refusal("destructive with nothing to preview", destructionWithNothingToPreview()));
    }

    /** What was wrong, and what the catalogue said about it. */
    public record Refusal(String what, String message) {
    }
}
