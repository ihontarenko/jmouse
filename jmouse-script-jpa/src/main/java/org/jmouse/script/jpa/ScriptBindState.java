package org.jmouse.script.jpa;

/**
 * Whether the host accepted a rule's text the last time it was asked.
 *
 * <h2>⚠️ TWO STATES, AND A REFUSED DOCUMENT IS STILL A DOCUMENT</h2>
 *
 * <p>A rule that no longer binds is kept, marked and shown. The reason it usually stops binding is not a
 * typo — it is the build changing underneath a rule that was fine yesterday, which is a different thing
 * to go and fix and a much worse thing to delete.</p>
 *
 * <p>⚠️ Not to be confused with {@code ScriptBinding.Stage}, which says <em>how far</em> a document got
 * before it was refused — parsing, or binding names. This says only whether it runs.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum ScriptBindState {

    /** The host parsed it and every name in it is one this build declares. */
    BOUND,

    /** ⚠️ It did not load. It is kept, it does not run, and the reason is beside it. */
    REFUSED

}
