package org.jmouse.j2js.teavm.supplement.jdk;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;

/**
 * 🧩 What TeaVM's {@code java.lang.Character} lacks.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(Character.class)
public final class CharacterSupplements {

    private CharacterSupplements() {
    }

    /** The text of one code point — two chars for one beyond the basic plane. */
    @Supplement(SupplementKind.STATIC)
    public static String toString(int codePoint) {
        return new String(Character.toChars(codePoint));
    }
}
