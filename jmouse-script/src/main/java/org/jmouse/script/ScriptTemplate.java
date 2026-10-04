package org.jmouse.script;

import org.jmouse.script.spi.ScriptStage;
import org.jmouse.script.stage.StageTiming;

import java.util.Locale;

/**
 * The body a new rule starts from.
 *
 * <h2>⚠️ A NEW RULE STARTS NARROW, AND THAT IS THE WHOLE POINT OF A TEMPLATE</h2>
 *
 * <p>A rule created with an empty body is a rule that runs on everything — every item in every walk —
 * until its author remembers to narrow it. Starting from the stage's own guard makes the cheap filter
 * the default and the expensive one a decision somebody takes.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptTemplate {

    /**
     * A starter document for one moment.
     *
     * @param name  what the rule is called
     * @param stage the moment it will run at
     * @return jMS text
     */
    public String forStage(String name, ScriptStage<?> stage) {
        StringBuilder body = new StringBuilder();

        body.append("# ").append(stage.detail()).append('\n');
        body.append("#\n");
        body.append("# What a handler is handed here:\n");
        body.append("#   ").append(String.join(", ", stage.names())).append('\n');
        body.append("#\n");
        body.append("# The facades this installation offers are on the Help panel beside the editor.\n");
        body.append('\n');
        body.append("script \"").append(escape(name)).append("\" {\n");
        body.append('\n');
        body.append("    # The guard runs BEFORE the body, and a false answer costs nothing further.\n");
        body.append("    # Say here what this rule is not interested in — narrow it, do not widen it.\n");
        body.append("    on ").append(stage.name()).append(" when ").append(stage.guard()).append(" do\n");
        body.append('\n');
        body.append("        @log.info('").append(escape(name)).append(" ran')\n");
        body.append(bodyFor(stage));
        body.append("    end\n");
        body.append("}\n");

        return body.toString();
    }

    /**
     * A name a rule can carry, derived from what somebody typed.
     *
     * @param typed what they wrote in the title box
     * @return a name, never empty
     */
    public String nameFrom(String typed) {
        String cleaned = (typed == null ? "" : typed)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        return cleaned.isEmpty() ? "new-rule" : cleaned;
    }

    /**
     * ⚠️ EACH TIMING GETS A DIFFERENT STARTER BODY, and that is not decoration.
     *
     * <p>Handing an observation a {@code refuse} example teaches somebody to write a refusal at a moment
     * that cannot refuse — which runs, is logged as too late, and leaves them believing they stopped
     * something that had already happened. The template is the first thing they read about what this
     * moment is <b>for</b>.</p>
     *
     * <p>⚠️ The amendment example names no facade, because a library cannot know a product's nouns. The
     * shape is what it teaches; the Help panel carries the names.</p>
     */
    private static String bodyFor(ScriptStage<?> stage) {
        if (stage.timing() == StageTiming.AMENDMENT) {
            return """

                            # A rule here CHANGES the outcome, and says why — the reason is shown beside
                            # the change wherever the outcome is displayed. Every amendment is attributed
                            # to this rule by name, so nothing a rule does is silent.
                            #
                            #   @<facade>.<what>(<value>, 'why this, in your own words')

                    """;
        }

        if (stage.timing() == StageTiming.DECISION) {
            return """

                            # Refusing stops what is about to happen. The reason is shown to a person, so
                            # write it as one: it is the only answer they will get.
                            @decision.refuse('a rule declined this — say here why')

                    """;
        }

        return "\n";
    }

    private static String escape(String name) {
        return name.replace("\"", "");
    }

}
