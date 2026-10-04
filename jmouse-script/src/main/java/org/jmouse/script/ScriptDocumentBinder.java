package org.jmouse.script;

import org.jmouse.script.el.ScriptParseException;
import org.jmouse.script.el.SourceSpan;
import org.jmouse.script.el.budget.ScriptBudget;
import org.jmouse.script.el.host.ScriptBindException;
import org.jmouse.script.el.host.ScriptHost;
import org.jmouse.script.spi.ScriptStage;

/**
 * Turning a document's text into something runnable, or into a sentence somebody can act on.
 *
 * <h2>⚠️ ONE BOUND SCRIPT PER STAGE, NOT ONE PER DOCUMENT</h2>
 *
 * <p>A budget belongs to a {@code BoundScript} and is fixed when it is loaded. Ceilings are per stage —
 * fifty milliseconds inside a walk, five seconds after an import — so a document assigned to two stages
 * must be bound twice, each under the ceiling of the moment it will run at. Binding it once and
 * dispatching it everywhere would silently give every stage whichever ceiling happened to be used first,
 * and the symptom would be a walk that got slower for no visible reason.</p>
 *
 * <p>Parsing the same text twice costs nothing worth optimising: it happens at startup and after a save,
 * never on the dispatch path.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptDocumentBinder {

    private final ScriptHost host;

    public ScriptDocumentBinder(ScriptHost host) {
        this.host = host;
    }

    /**
     * Binds a document for one stage, under that stage's ceiling.
     *
     * @param name   what the document is called; it is what every refusal quotes back
     * @param source the jMS text
     * @param stage  the moment it will run at, for its ceiling
     * @return a bound script, or the reason there is none
     */
    public ScriptBinding bind(String name, String source, ScriptStage<?> stage) {
        return load(name, source, stage.ceiling());
    }

    /**
     * Checks a document without a stage in hand — for the editor's "does this load" button.
     *
     * <p>⚠️ Loaded under the widest ceiling, so this answers whether the TEXT is valid and never whether
     * it would fit inside a particular moment's budget. A rule can only exceed a budget by running, and
     * running it to find out is not something a validate button may do.</p>
     *
     * @param name   what the document is called
     * @param source the jMS text
     * @return a bound script, or the reason there is none
     */
    public ScriptBinding check(String name, String source) {
        return load(name, source, ScriptBudgets.BACKGROUND);
    }

    /**
     * ⚠️ Both refusals are caught and returned rather than thrown. This is called while rebuilding every
     * rule in the installation, and one rule with a typo must not stop the other nine from loading —
     * which is exactly what an escaping exception would do.
     */
    private ScriptBinding load(String name, String source, ScriptBudget budget) {
        try {
            return ScriptBinding.accepted(host.load(name, source, budget));

        } catch (ScriptParseException notAScript) {
            return ScriptBinding.refused(ScriptBinding.Stage.PARSE,
                                         describe(notAScript.at(), notAScript.detail()));

        } catch (ScriptBindException notThisHost) {
            return ScriptBinding.refused(ScriptBinding.Stage.BIND,
                                         describe(notThisHost.at(), notThisHost.detail()));
        }
    }

    /**
     * ⚠️ The position is put in front of the sentence rather than glued into it. The dialect's own words
     * are the part that says what to type instead, and they are passed through untouched.
     */
    private static String describe(SourceSpan at, String detail) {
        if (at == null || !at.isKnown()) {
            return detail;
        }

        return "line %d, column %d — %s".formatted(at.line(), at.column(), detail);
    }

}
