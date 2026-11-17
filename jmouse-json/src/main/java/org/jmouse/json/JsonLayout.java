package org.jmouse.json;

import org.jmouse.el.writer.SourceLayout;

/**
 * 🧾 How a JSON text is laid out: on one line with nothing between its marks, or a member and an element a line, indented.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum JsonLayout {

    /** {@code {"x":1,"items":[1,2]}} — what crosses a wire. */
    COMPACT(SourceLayout.GLUED),

    /** A member and an element a line, four spaces a level, a space after each colon — what a person reads. */
    INDENTED(SourceLayout.WORDS);

    private final SourceLayout sourceLayout;

    JsonLayout(SourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout;
    }

    /** The engine's layout this one writes with. */
    public SourceLayout sourceLayout() {
        return sourceLayout;
    }
}
