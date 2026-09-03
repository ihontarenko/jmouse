package org.jmouse.validator.el.smoke;

import org.jmouse.validator.el.runtime.CompiledValidation;
import org.jmouse.validator.el.runtime.ValidationOutcome;

import java.util.List;

/**
 * {@code when} / {@code otherwise}, nested — and the fact that a skipped field is not a passed one. 🌿
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class BranchingSmoke {

    private static final String DOCUMENT = """
            validation "smoke/branching" {
                always {
                    mount_type : required, oneOf('SMD', 'THT')
                }

                when mount_type == 'SMD' {
                    resistor_package : required : 'A surface part needs its package'

                    when quantity > 100 {
                        reel_code : required : 'A reel of this size needs its code'
                    }
                } otherwise {
                    lead_spacing : required : 'A through-hole part needs its lead spacing'
                }
            }
            """;

    public static int run() {
        SmokeReport        report     = new SmokeReport("Branching - when, otherwise, nesting");
        CompiledValidation validation = SmokeReport.compile(DOCUMENT);

        ValidationOutcome surface = validation.validate(
                SmokeReport.record("mount_type", "SMD", "quantity", 10, "resistor_package", null));

        report.expectErrors("SMD with no package is refused",
                            List.of("A surface part needs its package"), surface);
        // ⚠️ Two unasked fields, not one: the other branch's, AND the nested guard's, whose condition
        // did not hold either. Somebody reading only the outer `otherwise` would expect one.
        report.expect("neither the other branch's field nor the nested one is asked about",
                      "[reel_code, lead_spacing]", surface.skipped().toString());

        ValidationOutcome throughHole = validation.validate(
                SmokeReport.record("mount_type", "THT", "quantity", 10, "lead_spacing", null));

        report.expectErrors("THT with no lead spacing is refused",
                            List.of("A through-hole part needs its lead spacing"), throughHole);
        report.expect("and the guarded branch's fields are not asked about",
                      "[resistor_package, reel_code]", throughHole.skipped().toString());

        ValidationOutcome smallReel = validation.validate(
                SmokeReport.record("mount_type", "SMD", "quantity", 10, "resistor_package", "0805"));

        report.expect("a nested guard that does not hold leaves its field alone",
                      "[reel_code, lead_spacing]", smallReel.skipped().toString());
        report.expect("and the record is valid", true, smallReel.isValid());

        ValidationOutcome bigReel = validation.validate(
                SmokeReport.record("mount_type", "SMD", "quantity", 500, "resistor_package", "0805",
                                   "reel_code", null));

        report.expectErrors("a nested guard that holds does ask",
                            List.of("A reel of this size needs its code"), bigReel);

        return report.failures();
    }

    public static void main(String[] arguments) {
        System.exit(run());
    }

    private BranchingSmoke() {
    }
}
