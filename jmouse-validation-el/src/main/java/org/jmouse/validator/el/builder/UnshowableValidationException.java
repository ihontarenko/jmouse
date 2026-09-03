package org.jmouse.validator.el.builder;

/**
 * A document the form cannot show, said in a way a screen can act on. 🚫
 *
 * <h2>⚠️ The construct travels beside the sentence, not inside it</h2>
 *
 * <p>A screen has to name what it could not show — to offer the text editor for that one thing, to log
 * which construct is outrunning the form, to decide whether to warn or refuse. A message is written for
 * a person, and a screen keying on one breaks the day the wording improves. So the construct is its own
 * field.</p>
 *
 * <h2>⚠️ Refusing is the whole point</h2>
 *
 * <p>The alternative — show what the form understands, drop the rest — produces a form that saves, and
 * the save deletes what somebody wrote. Invisible, permanent, and it happens to whoever trusted the
 * tool most.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class UnshowableValidationException extends RuntimeException {

    private final String construct;
    private final String detail;

    /**
     * @param construct what the document holds that no row carries — {@code when}, {@code gate}
     * @param detail    what a person should do about it
     */
    public UnshowableValidationException(String construct, String detail) {
        super("'%s' cannot be shown as rows: %s".formatted(construct, detail));
        this.construct = construct;
        this.detail = detail;
    }

    /** @return the construct, for a screen to name without reading the sentence */
    public String construct() {
        return construct;
    }

    /** @return what to do about it */
    public String detail() {
        return detail;
    }
}
