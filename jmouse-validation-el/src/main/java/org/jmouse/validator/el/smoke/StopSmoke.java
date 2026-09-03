package org.jmouse.validator.el.smoke;

import org.jmouse.validator.el.runtime.CompiledValidation;
import org.jmouse.validator.el.runtime.ValidationOutcome;

import java.util.List;

/**
 * The two things that stop early, and how far each of them reaches. ⛔
 *
 * <p>⚠️ The point of this file is the <em>difference</em>. {@code stop} ends one field's list and its
 * siblings still answer; a {@code gate} ends the document. Two mechanisms that both "fail fast" and
 * stop different amounts is exactly the pair a reader will assume is one, so both are checked against
 * the same record.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class StopSmoke {

    private static final String WITH_STOP = """
            validation "smoke/stop" {
                always {
                    part_number : notBlank stop : 'A part number is needed',
                                  size(3, 32) : 'Three to thirty-two characters',
                                  pattern('^[A-Z]{2}-[0-9]{4}$') : 'Looks like AB-1234'
                    quantity    : min(0) : 'Cannot be negative'
                }
            }
            """;

    private static final String WITHOUT_STOP = """
            validation "smoke/no-stop" {
                always {
                    part_number : notBlank : 'A part number is needed',
                                  size(3, 32) : 'Three to thirty-two characters',
                                  pattern('^[A-Z]{2}-[0-9]{4}$') : 'Looks like AB-1234'
                    quantity    : min(0) : 'Cannot be negative'
                }
            }
            """;

    private static final String GATED = """
            validation "smoke/gate" {
                gate {
                    form_version : pattern('^2[.].*') : 'This form has moved on - reopen it'
                }

                always {
                    part_number : notBlank : 'A part number is needed'
                    quantity    : min(0) : 'Cannot be negative'
                }
            }
            """;

    public static int run() {
        SmokeReport report = new SmokeReport("Stopping - per field, and per document");

        CompiledValidation stopping = SmokeReport.compile(WITH_STOP);
        CompiledValidation plain    = SmokeReport.compile(WITHOUT_STOP);

        ValidationOutcome stopped = stopping.validate(
                SmokeReport.record("part_number", " ", "quantity", -1));

        report.expectErrors("stop silences the rest of ITS OWN field, and nothing else",
                            List.of("A part number is needed", "Cannot be negative"), stopped);

        ValidationOutcome unstopped = plain.validate(
                SmokeReport.record("part_number", " ", "quantity", -1));

        report.expectErrors("without it, every check on that field still answers",
                            List.of("A part number is needed", "Three to thirty-two characters",
                                    "Looks like AB-1234", "Cannot be negative"), unstopped);

        report.expect("so stop removed exactly the noise and kept the sibling",
                      2, unstopped.errors().size() - stopped.errors().size());

        CompiledValidation gated = SmokeReport.compile(GATED);

        ValidationOutcome refused = gated.validate(
                SmokeReport.record("form_version", "1.4", "part_number", "   ", "quantity", -1));

        report.expectErrors("a failed gate is the WHOLE answer - the body never ran",
                            List.of("This form has moved on - reopen it"), refused);
        report.expect("and it says so", true, refused.gated());

        ValidationOutcome passed = gated.validate(
                SmokeReport.record("form_version", "2.1", "part_number", "   ", "quantity", -1));

        report.expectErrors("a gate that lets the record through changes nothing else",
                            List.of("A part number is needed", "Cannot be negative"), passed);
        report.expect("and does not claim to have gated", false, passed.gated());

        return report.failures();
    }

    public static void main(String[] arguments) {
        System.exit(run());
    }

    private StopSmoke() {
    }
}
