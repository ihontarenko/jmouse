package org.jmouse.validator.el.smoke;

import org.jmouse.validator.el.JmvReader;
import org.jmouse.validator.el.builder.ValidationDraft;
import org.jmouse.validator.el.builder.ValidationDrafts;
import org.jmouse.validator.el.node.ValidationDocumentNode;
import org.jmouse.validator.el.translate.JmvWriter;

import java.util.List;

/**
 * A document through the form and back — the trip a builder actually makes. 🧱
 *
 * <p>⚠️ The claim is <strong>text → rows → text is the identity</strong>. Anything less means the form
 * loses something on the way through, and what it loses it loses on save: silently, permanently, and to
 * whoever trusted it most. Checking only that rows can be rendered would prove nothing about that.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class BuilderSmoke {

    private static final String DOCUMENT = """
            # validation/part.jmv
            # A part, as the catalogue needs it.

            validation "innoventa/part" {

                # A record from an older form is not judged at all.
                gate {
                    form_version : pattern('^2[.].*') : 'This form has moved on'
                }

                always {
                    part_number : required stop, notBlank, size(3, 32)   # the common failure
                                : 'A part number looks like AB-1234'

                    datasheet : optional, url(host: 'mouser.com')
                }

                when mount_type == 'SMD' {
                    resistor_package : required, oneOf('0805', '0603')

                    when quantity > 100 {
                        reel_code : required : 'A reel of this size needs its code'
                    }
                } otherwise {
                    lead_spacing : required : 'A through-hole part needs its lead spacing'
                }

                invariant min_stock_threshold <= quantity : 'The threshold cannot exceed stock'
            }
            """;

    public static int run() {
        SmokeReport report = new SmokeReport("Builder - text through the form and back");

        JmvReader              reader   = new JmvReader();
        JmvWriter              writer   = new JmvWriter();
        ValidationDocumentNode parsed   = reader.parse(DOCUMENT, "part.jmv");
        String                 original = writer.translate(parsed);

        ValidationDraft draft = ValidationDrafts.toDraft(parsed);
        String          again = writer.translate(ValidationDrafts.toDocument(draft));

        report.expect("text through the form and back is the identity", original, again);
        report.expect("the document keeps its name", "innoventa/part", draft.name());
        // ⚠️ Three, not two: the blank line between the header and the document is trivia as well,
        // and dropping it would close the gap somebody left on every save.
        report.expect("the file header and the gap below it reach the form", 3, draft.comments().size());

        ValidationDraft.ItemDraft gate = draft.items().getFirst();

        report.expect("the gate is a row", "BLOCK", gate.kind().name());
        report.expect("and carries the comment above it", true,
                      gate.comments().contains("# A record from an older form is not judged at all."));

        ValidationDraft.ItemDraft always      = draft.items().get(1);
        ValidationDraft.ItemDraft partNumber  = always.items().getFirst();

        report.expect("a check line's checks are rows in order", "[required, notBlank, size]",
                      partNumber.checks().stream().map(ValidationDraft.CheckDraft::check).toList()
                              .toString());
        report.expect("stop is a flag on the check that carries it",
                      true, partNumber.checks().getFirst().stop());
        report.expect("an argument travels as written", "[3, 32]",
                      partNumber.checks().get(2).positional().toString());
        report.expect("an aside on the checks stays on the CHECKS, not on the message below them",
                      "# the common failure", partNumber.checksNote());

        ValidationDraft.ItemDraft guard = draft.items().get(2);

        report.expect("a guard is a row with two branches", "GUARD", guard.kind().name());
        report.expect("and its otherwise is present", 1, sizeOf(guard.otherwise()));
        report.expect("a nested guard survives as a guard", "GUARD",
                      guard.items().get(1).kind().name());

        ValidationDraft.ItemDraft datasheet = always.items().get(1);

        report.expect("a named argument travels by name", "{host='mouser.com'}",
                      datasheet.checks().get(1).named().toString());

        return report.failures();
    }

    private static int sizeOf(List<?> items) {
        return items == null ? -1 : items.size();
    }

    public static void main(String[] arguments) {
        System.exit(run());
    }

    private BuilderSmoke() {
    }
}
