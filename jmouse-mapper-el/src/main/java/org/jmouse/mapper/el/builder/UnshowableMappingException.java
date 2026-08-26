package org.jmouse.mapper.el.builder;

/**
 * A document holds something the form has no row for. 🚧
 *
 * <h2>⚠️ Refusing is the whole feature, not a gap in it</h2>
 *
 * <p>A builder meets documents it did not write — somebody edits the text tab, or opens a file written
 * by hand. Some of those hold a {@code fragment}, an {@code include}, a {@code refuse} block or a
 * whole-pair conversion, and none of that is a row.</p>
 *
 * <p>The tempting behaviour is to show what the form understands and leave the rest out of view. That
 * form then <strong>saves</strong>, and the save deletes what it never showed. The loss is invisible,
 * it is permanent, and it lands on whoever trusted the tool most.</p>
 *
 * <p>So the form goes read-only for that document and says which construct put it there. A person can
 * then edit the text, which is where that construct lives anyway.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class UnshowableMappingException extends RuntimeException {

    private final String construct;

    /**
     * Says what cannot be shown, and why it is not a row.
     *
     * @param construct what the document holds, spelled the way a file writes it
     * @param because   why it has no row — ⚠️ phrased about the construct rather than about the form,
     *                  because "the builder does not support it" tells a reader nothing they can act on
     */
    public UnshowableMappingException(String construct, String because) {
        super(("this mapping cannot be shown as a form because it uses '%s': %s. Edit it as text — the "
               + "form would have to leave it out, and saving would then delete it")
                .formatted(construct, because));
        this.construct = construct;
    }

    /**
     * What the document holds that has no row.
     *
     * <p>Named separately from the message so a screen can say it in its own words, and highlight the
     * line, without parsing a sentence.</p>
     *
     * @return the construct
     */
    public String construct() {
        return construct;
    }
}
