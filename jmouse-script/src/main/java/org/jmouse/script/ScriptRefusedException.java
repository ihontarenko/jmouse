package org.jmouse.script;

/**
 * A rule objected to what was about to happen.
 *
 * <h2>⚠️ NOT AN ERROR, AND THE DISTINCTION IS THE WHOLE POINT</h2>
 *
 * <p>jMS has no {@code refuse} keyword — a rule refuses by calling {@code @decision.refuse('…')}, which
 * throws this. So the dispatcher sees two kinds of exception coming out of a rule and must not treat
 * them alike:</p>
 *
 * <ul>
 *   <li><strong>This one</strong> is an objection. The rule worked exactly as written, and its message
 *       is a phrase a person will read.</li>
 *   <li><strong>Anything else</strong> is the rule being broken — a facade that failed, a budget
 *       exceeded, a null nobody expected. Reading that as "you may not do this" would turn a defect into
 *       a business rule and leave nobody able to tell the two apart.</li>
 * </ul>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptRefusedException extends RuntimeException {

    public ScriptRefusedException(String reason) {
        super(reason);
    }

}
