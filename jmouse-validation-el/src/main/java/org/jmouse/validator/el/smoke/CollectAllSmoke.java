package org.jmouse.validator.el.smoke;

import org.jmouse.validator.el.runtime.CompiledValidation;
import org.jmouse.validator.el.runtime.ValidationOutcome;

import java.util.List;

/**
 * Everything wrong with a record, in one answer — the default behaviour. 🧾
 *
 * <p>Also the invariant, which belongs to no field and therefore appears under {@code null}.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class CollectAllSmoke {

    private static final String DOCUMENT = """
            validation "smoke/collect-all" {
                always {
                    part_number : notBlank : 'A part number is needed',
                                  size(3, 32) : 'Three to thirty-two characters',
                                  pattern('^[A-Z]{2}-[0-9]{4}$') : 'Looks like AB-1234'
                    quantity    : min(0) : 'Cannot be negative'
                    mount_type  : oneOf('SMD', 'THT') : 'SMD or THT'
                    datasheet   : optional, url(host: 'mouser.com') : 'A Mouser link'
                }

                invariant min_stock_threshold <= quantity : 'The threshold cannot exceed stock'
            }
            """;

    public static int run() {
        SmokeReport        report     = new SmokeReport("Collect all - every complaint, in one answer");
        CompiledValidation validation = SmokeReport.compile(DOCUMENT);

        ValidationOutcome everything = validation.validate(SmokeReport.record(
                "part_number", "ab",
                "quantity", -5,
                "mount_type", "BGA",
                "datasheet", "https://example.com/x.pdf",
                "min_stock_threshold", 10));

        report.expectErrors("one bad record produces five complaints, in document order",
                            List.of("Three to thirty-two characters",
                                    "Looks like AB-1234",
                                    "Cannot be negative",
                                    "SMD or THT",
                                    "A Mouser link",
                                    "The threshold cannot exceed stock"),
                            everything);

        report.expect("nothing was gated", false, everything.gated());
        report.expect("the invariant is filed under no field",
                      true, everything.byField().containsKey(null));

        ValidationOutcome fine = validation.validate(SmokeReport.record(
                "part_number", "AB-1234",
                "quantity", 20,
                "mount_type", "SMD",
                "datasheet", null,
                "min_stock_threshold", 5));

        report.expect("a good record passes, absent optional included", true, fine.isValid());

        ValidationOutcome blank = validation.validate(SmokeReport.record(
                "part_number", "   ",
                "quantity", 1,
                "mount_type", "SMD",
                "min_stock_threshold", 0));

        report.expectErrors("a blank value fails notBlank and pattern, and keeps going",
                            List.of("A part number is needed", "Looks like AB-1234"), blank);

        return report.failures();
    }

    public static void main(String[] arguments) {
        System.exit(run());
    }

    private CollectAllSmoke() {
    }
}
