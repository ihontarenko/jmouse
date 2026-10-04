package org.jmouse.script;

import org.jmouse.script.el.host.BoundScript;

/**
 * What came back from asking the host to load a document: a script, or a sentence saying why not.
 *
 * <h2>⚠️ TWO KINDS OF REFUSAL, KEPT APART</h2>
 *
 * <p>The dialect distinguishes them and so does this, because they send somebody to fix two different
 * things:</p>
 *
 * <ul>
 *   <li><strong>parse</strong> — the text is not a script. A body that never closes, a handler with no
 *       {@code do}. This is a typo, and the position is where to look.</li>
 *   <li><strong>bind</strong> — it <em>is</em> a script, and it names something this build did not
 *       declare. Usually the document is fine and the build changed underneath it: a stage renamed, a
 *       facade withdrawn. Somebody sent looking for a missing bracket will not find one.</li>
 * </ul>
 *
 * <p>Flattening both into "invalid" is what makes the second look like the first.</p>
 *
 * @param script  the bound script, or {@code null} when it was refused
 * @param stage   which stage of loading refused it, or {@code null} when nothing did
 * @param problem the host's own sentence, with its position in front of it, or {@code null}
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record ScriptBinding(BoundScript script, Stage stage, String problem) {

    /** How far a document got before it was refused. */
    public enum Stage {

        /** The text is not a script. */
        PARSE,

        /** It is a script, and it names something this build did not declare. */
        BIND
    }

    /**
     * A document the host accepted.
     *
     * @param script the bound script
     * @return the binding
     */
    public static ScriptBinding accepted(BoundScript script) {
        return new ScriptBinding(script, null, null);
    }

    /**
     * A document the host refused.
     *
     * @param stage   which stage refused it
     * @param problem the host's own sentence
     * @return the binding
     */
    public static ScriptBinding refused(Stage stage, String problem) {
        return new ScriptBinding(null, stage, problem);
    }

    /**
     * Whether the host accepted it.
     *
     * <p>⚠️ Named so it cannot be mistaken for an accessor of a stored field — this record travels into
     * a DTO, and {@code isAccepted()} would appear in JSON as a component nobody declared.</p>
     *
     * @return true when there is a script to run
     */
    public boolean wasAccepted() {
        return script != null;
    }

}
