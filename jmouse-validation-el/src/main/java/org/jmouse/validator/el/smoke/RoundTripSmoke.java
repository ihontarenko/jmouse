package org.jmouse.validator.el.smoke;

import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.validator.el.JmvReader;
import org.jmouse.validator.el.node.ValidationDocumentNode;
import org.jmouse.validator.el.runtime.CompiledValidation;
import org.jmouse.validator.el.translate.JmvCapability;
import org.jmouse.validator.el.translate.JmvCompiler;
import org.jmouse.validator.el.translate.JmvWriter;
import org.jmouse.validator.el.translate.ValidationTranslator;
import org.jmouse.el.node.Node;
import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.Capabilities;

import java.util.Map;

/**
 * Read, write, read again — and a translator that refuses what it cannot honour. 🔁
 *
 * <p>⚠️ The claim being checked is <strong>not</strong> "the writer produces something that parses".
 * It is that writing is a fixed point: render, re-read, render again, and the two renderings are the
 * same text. A writer that loses a message or reorders a check would still produce parseable output,
 * and only comparing the second pass with the first catches it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class RoundTripSmoke {

    private static final String DOCUMENT = """
            validation "smoke/round-trip" {
                gate {
                    form_version : pattern('^2[.].*') : 'This form has moved on'
                }

                always {
                    part_number : required stop, notBlank, size(3, 32),
                                  pattern('^[A-Z]{2}-[0-9]{4}$')
                                : 'A part number looks like AB-1234'
                    datasheet   : optional, url(host: 'mouser.com') : 'A Mouser link'
                }

                when mount_type == 'SMD' {
                    resistor_package : required, oneOf('0805', '0603')
                } otherwise {
                    lead_spacing : required : 'A through-hole part needs its lead spacing'
                }

                invariant min_stock_threshold <= quantity : 'The threshold cannot exceed stock'
            }
            """;

    public static int run() {
        SmokeReport report = new SmokeReport("Round trip - the writer is a fixed point");

        JmvReader reader = new JmvReader();
        JmvWriter writer = new JmvWriter();

        ValidationDocumentNode first   = reader.parse(DOCUMENT, "round-trip.jmv");
        String                 written = writer.translate(first);

        ValidationDocumentNode second    = reader.parse(written, "written.jmv");
        String                 rewritten = writer.translate(second);

        report.expect("writing is idempotent", written, rewritten);
        report.expect("and the name survives", "smoke/round-trip", second.getName());

        report.expect("stop survives", true, written.contains("required stop"));
        report.expect("a named argument survives", true, written.contains("url(host: 'mouser.com')"));
        report.expect("a collecting check survives", true, written.contains("oneOf('0805', '0603')"));
        report.expect("otherwise survives", true, written.contains("} otherwise {"));
        // ⚠️ Asserted as "a line of its own opening with ':'", not by counting the indent. The indent is
        // presentation and will be tuned; that the message is not inline is the grammar.
        report.expect("the line message stays a continuation line", true,
                      written.lines().anyMatch(
                              rendered -> rendered.strip().startsWith(": 'A part number looks like")));

        CompiledValidation before = new JmvCompiler().translate(first);
        CompiledValidation after  = new JmvCompiler().translate(second);

        Map<String, Object> record = SmokeReport.record(
                "form_version", "2.0", "part_number", "ab", "mount_type", "THT",
                "lead_spacing", null, "min_stock_threshold", 10, "quantity", 5);

        report.expect("and both compile to the same judgement",
                      before.validate(record).toString(), after.validate(record).toString());

        report.expect("a translator refuses what it cannot honour", "refused", refusal(first));

        return report.failures();
    }

    /**
     * A destination that honours everything except an invariant, offered a document containing one.
     *
     * <p>⚠️ Written as a translator of its own rather than as a narrowed {@link JmvWriter}, because the
     * thing being checked is the <em>seam</em>: any destination declaring less than the document asks
     * for must refuse, and a browser-side renderer — which genuinely cannot evaluate a cross-field
     * assertion — is the case this exists for.</p>
     *
     * @param document the document
     * @return {@code "refused"} when it was, else what happened instead
     */
    private static String refusal(ValidationDocumentNode document) {
        ValidationTranslator<String> narrow = new ValidationTranslator<>() {

            @Override
            public Capabilities capabilities() {
                return Capabilities.of("no-invariants", JmvCapability.GATE, JmvCapability.GUARD,
                                       JmvCapability.STOP, JmvCapability.MESSAGE);
            }

            @Override
            public String translate(Node node, Bindings bindings) {
                requireSupport(node);

                return "";
            }
        };

        try {
            narrow.translate(document);

            return "accepted";
        } catch (TranslationRefusedException refused) {
            return refused.getMessage().contains("invariant") ? "refused" : "refused the wrong thing";
        }
    }

    public static void main(String[] arguments) {
        System.exit(run());
    }

    private RoundTripSmoke() {
    }
}
