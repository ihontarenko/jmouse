package org.jmouse.validator.el.smoke;

import org.jmouse.validator.el.runtime.CompiledValidation;

import java.util.List;
import java.util.Map;

/**
 * Deep and flat say the same thing — the claim the language makes, checked. 🪞
 *
 * <p>⚠️ Two documents, four records, and the two answers compared for every one. Asserting that the
 * deep form works would prove nothing: the claim is not that both are legal, it is that <em>nesting is
 * conjunction</em>, and only comparing them says so.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ShapeSmoke {

    private static final String DEEP = """
            validation "smoke/deep" {
                when mount_type == 'SMD' {
                    when quantity > 100 {
                        reel_code : required : 'A reel needs its code'
                    }
                }
            }
            """;

    private static final String FLAT = """
            validation "smoke/flat" {
                when mount_type == 'SMD' and quantity > 100 {
                    reel_code : required : 'A reel needs its code'
                }
            }
            """;

    private static final List<Map<String, Object>> RECORDS = List.of(
            SmokeReport.record("mount_type", "SMD", "quantity", 500, "reel_code", null),
            SmokeReport.record("mount_type", "SMD", "quantity", 500, "reel_code", "R-1"),
            SmokeReport.record("mount_type", "SMD", "quantity", 10, "reel_code", null),
            SmokeReport.record("mount_type", "THT", "quantity", 500, "reel_code", null));

    public static int run() {
        SmokeReport        report = new SmokeReport("Shape - a deep document and a flat one agree");
        CompiledValidation deep   = SmokeReport.compile(DEEP);
        CompiledReel       flat   = new CompiledReel(SmokeReport.compile(FLAT));

        for (Map<String, Object> record : RECORDS) {
            report.expect("deep == flat for " + record,
                          deep.validate(record).toString(), flat.answer(record));
        }

        return report.failures();
    }

    /**
     * The flat document's answer as text, so the two are compared on what they said rather than on
     * object identity.
     */
    private record CompiledReel(CompiledValidation validation) {

        String answer(Map<String, Object> record) {
            return validation.validate(record).toString();
        }
    }

    public static void main(String[] arguments) {
        System.exit(run());
    }

    private ShapeSmoke() {
    }
}
