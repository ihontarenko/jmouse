package org.jmouse.validator.el.smoke;

import org.jmouse.validator.el.JmvReader;
import org.jmouse.validator.el.node.ValidationDocumentNode;
import org.jmouse.validator.el.translate.JmvWriter;

/**
 * What a person wrote around the rules survives being read and written back. 🗒️
 *
 * <p>⚠️ The claim is not "comments are kept somewhere". It is that a document opened and saved comes
 * back <strong>as it was written</strong> — which is the difference between a builder somebody will
 * use on an existing file and one they will use once.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class TriviaSmoke {

    private static final String DOCUMENT = """
            # validation/part.jmv
            # What a part has to look like before it is filed.

            validation "innoventa/part" {

                # The gate: a record from an older form is not judged at all.
                gate {
                    form_version : pattern('^2[.].*') : 'This form has moved on'
                }

                always {
                    # A part number is the one field everything else is filed under.
                    part_number : required stop, notBlank   # blank is the common case

                    quantity : min(0) : 'Cannot be negative'
                }
            }
            """;

    public static int run() {
        SmokeReport report = new SmokeReport("Trivia - comments and blank lines survive a round trip");

        JmvReader              reader    = new JmvReader();
        JmvWriter              writer    = new JmvWriter();
        ValidationDocumentNode first     = reader.parse(DOCUMENT, "part.jmv");
        String                 written   = writer.translate(first);
        String                 rewritten = writer.translate(reader.parse(written, "again.jmv"));

        report.expect("a file's header comment survives",
                      true, written.contains("# What a part has to look like before it is filed."));
        report.expect("a comment above a block survives",
                      true, written.contains("# The gate: a record from an older form"));
        report.expect("a comment above a check line survives",
                      true, written.contains("# A part number is the one field"));
        report.expect("an aside at the end of a line stays on that line", true,
                      written.lines().anyMatch(line -> line.contains("part_number")
                                                       && line.contains("# blank is the common case")));

        // ⚠️ A blank line is trivia too. Without it a document's paragraphs collapse into one block,
        // losing no characters and every bit of the grouping that made it readable.
        report.expect("a blank line between two check lines survives", true,
                      written.contains("# blank is the common case" + System.lineSeparator()
                                       + System.lineSeparator()));

        report.expect("and writing what was written changes nothing", written, rewritten);

        // ⚠️ Saving must not grow the file. A writer that adds its own spacing on top of the author's
        // adds one line per save, and a document opened and saved ten times is ten lines taller.
        report.expect("no line was gained on the way through",
                      DOCUMENT.lines().count(), written.lines().count());

        return report.failures();
    }

    public static void main(String[] arguments) {
        System.exit(run());
    }

    private TriviaSmoke() {
    }
}
