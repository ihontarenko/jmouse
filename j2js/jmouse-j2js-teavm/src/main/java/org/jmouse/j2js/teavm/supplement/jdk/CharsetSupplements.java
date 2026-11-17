package org.jmouse.j2js.teavm.supplement.jdk;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * 🧩 What TeaVM's {@code java.nio.charset.Charset} lacks.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(Charset.class)
public final class CharsetSupplements {

    private CharsetSupplements() {
    }

    /** The charset of a name, or {@code fallback} where none is available by it; an illegal name is still refused. */
    @Supplement(SupplementKind.STATIC)
    public static Charset forName(String charsetName, Charset fallback) {
        try {
            return Charset.forName(charsetName);
        } catch (UnsupportedCharsetException exception) {
            return fallback;
        }
    }
}
