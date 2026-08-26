package org.jmouse.mapper.el.examples;

import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.mapper.el.JmmReader;
import org.jmouse.mapper.el.node.MappingDocumentNode;
import org.jmouse.mapper.el.translate.JmmCapability;
import org.jmouse.mapper.el.translate.JmmJsonTranslator;
import org.jmouse.mapper.el.translate.JmmSourceTranslator;

/**
 * A {@code .jmm} document out through both destinations, and back in. 🔁
 *
 * <h2>⚠️ What the round trip actually proves, and what it does not</h2>
 *
 * <p>Mapping with a file proves the rules that <strong>fired</strong> were understood. It says nothing
 * about the {@code when} whose condition was false, the {@code refuse} that never tripped, the
 * {@code : via(…)} the engine refuses anyway, or the {@code unmapped fail} on a document that happened
 * to be complete. Every one of those is a construct the parser could be quietly mis-reading, and the
 * only cheap way to see it is to ask the writer to say the document back.</p>
 *
 * <p>So the fixture below is deliberately <em>not</em> a mapping anybody would write. It is every
 * construct the language has, in one file, exercised by being written down.</p>
 *
 * <h2>⚠️ Sameness is checked through the structured form, not the source</h2>
 *
 * <p>{@link MappingDocumentNode} has no {@code equals}, and comparing rendered source to rendered
 * source only proves the <em>writer</em> is deterministic. Comparing the JSON of the original document
 * against the JSON of the re-parsed one compares what the two documents hold, field by field — which
 * is the second destination earning its place rather than merely existing.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmmTranslatorSmoke {

    private static int checked;
    private static int failures;

    /** Every construct the language has, in one file. */
    private static final String EVERYTHING = """
            mapping "shop/everything" {

                use org.jmouse.mapper.el.examples.JmmSmoke$OrderRequest
                use org.jmouse.mapper.el.examples.JmmSmoke$Order

                fragment auditing {
                    auditNote : ignore
                }

                target Order {

                    unmapped fail

                    always {
                        include auditing
                        status : "CREATED"
                    }

                    refuse target before {
                        status == "LOCKED" : "a locked order cannot be remapped"
                    }

                    refuse target after {
                        comment is null : "an order was produced with no comment"
                    }

                    from OrderRequest {

                        refuse source before {
                            reference is null : "a request with no reference cannot be mapped"
                            firstName is null : "a request with no buyer cannot be mapped"
                        }

                        let full = firstName ~ " " ~ lastName

                        reference : reference | trim | upper
                        buyerName : full
                        total     : amount
                        comment   : comment | default("none") when reference | trim == "big"
                        secret    : ignore
                    }
                }
            }
            """;

    /** ⚠️ Parses and is refused by the engine (JMF-193) — which is exactly why it must still write. */
    private static final String CONVERTED = """
            mapping "conversion" {

                use org.jmouse.mapper.el.examples.JmmSmoke$OrderRequest
                use org.jmouse.mapper.el.examples.JmmSmoke$Order

                target Order {

                    from OrderRequest : via("money")
                }
            }
            """;

    /** The file the engine is actually driven with, so one fixture is known to map. */
    private static final String WORKING = """
            mapping "shop/checkout" {

                use org.jmouse.mapper.el.examples.JmmSmoke$OrderRequest
                use org.jmouse.mapper.el.examples.JmmSmoke$Order

                target Order {

                    from OrderRequest {
                        reference : reference | trim | upper
                        secret    : ignore
                    }
                }
            }
            """;

    private JmmTranslatorSmoke() {
    }

    public static void main(String... arguments) {
        roundTrip("everything", EVERYTHING);
        roundTrip("conversion", CONVERTED);
        roundTrip("working", WORKING);

        verifyEveryConstructIsWritten();
        verifyWhatItWritesStillMaps();
        verifyRefusals();

        report();
    }

    /**
     * Writes a document, reads it back, and checks the two hold the same thing.
     *
     * @param name   what to call it in the report
     * @param source the file
     */
    private static void roundTrip(String name, String source) {
        JmmReader           reader   = new JmmReader();
        MappingDocumentNode first    = reader.parse(source, name + ".jmm");
        String              written  = JmmSourceTranslator.INSTANCE.translate(first);
        MappingDocumentNode second   = reader.parse(written, name + ".jmm");
        String              rewritten = JmmSourceTranslator.INSTANCE.translate(second);

        equal(name + ": what it wrote is valid .jmm and writes the same again", written, rewritten);
        equal(name + ": the document that came back holds the same thing",
              JmmJsonTranslator.INSTANCE.translate(first),
              JmmJsonTranslator.INSTANCE.translate(second));
    }

    /**
     * The five constructs a naive walker forgets, plus the one the engine refuses.
     *
     * <p>⚠️ Checked in <strong>both</strong> destinations. A construct that survives into the source
     * and not into the structured form is a construct one of the two walks does not know about, and the
     * whole point of the second destination is to be the walk that disagrees.</p>
     */
    private static void verifyEveryConstructIsWritten() {
        MappingDocumentNode document = new JmmReader().parse(EVERYTHING, "everything.jmm");
        String              source   = JmmSourceTranslator.INSTANCE.translate(document);
        String              json     = JmmJsonTranslator.INSTANCE.translate(document);

        holds("source keeps 'always'", source, "always {");
        holds("source keeps 'include'", source, "include auditing");
        holds("source keeps 'when'", source, "when reference | trim == \"big\"");
        holds("source keeps 'refuse'", source, "refuse source before {");
        holds("source keeps the target refusals", source, "refuse target after {");
        holds("source keeps 'ignore'", source, "secret : ignore");
        holds("source keeps 'unmapped fail'", source, "unmapped fail");
        holds("source keeps 'let'", source, "let full = firstName ~ \" \" ~ lastName");

        holds("json keeps 'always'", json, "\"always\": {");
        holds("json keeps 'include'", json, "\"includes\": [\n");
        holds("json keeps 'when'", json, "\"condition\": \"reference | trim == \\\"big\\\"\"");
        holds("json keeps 'refuse'", json, "\"subject\": \"SOURCE\"");
        holds("json keeps 'ignore' as a flag, not as a missing value", json, "\"ignored\": true");
        holds("json keeps 'unmapped'", json, "\"unmapped\": \"FAIL\"");
        holds("json keeps 'let'", json, "\"bindings\": [\n");

        String conversion = JmmJsonTranslator.INSTANCE.translate(
                new JmmReader().parse(CONVERTED, "conversion.jmm"));

        holds("source keeps ': via(…)'",
              JmmSourceTranslator.INSTANCE.translate(new JmmReader().parse(CONVERTED, "conversion.jmm")),
              "from OrderRequest : via(\"money\")");
        holds("json keeps the conversion", conversion, "\"conversion\": \"via(\\\"money\\\")\"");
    }

    /**
     * ⚠️ Valid-looking source is not the same as source the reader accepts and binds.
     *
     * <p>Re-reading through {@link JmmReader#read} runs the binder and the validator, which is what a
     * document rendered by an editor and saved back would meet.</p>
     */
    private static void verifyWhatItWritesStillMaps() {
        JmmReader reader  = new JmmReader();
        String    written = JmmSourceTranslator.INSTANCE.translate(reader.parse(WORKING, "working.jmm"));

        equal("what it wrote binds to the same number of rules",
              reader.read(WORKING, "working.jmm").size(),
              reader.read(written, "working.jmm").size());
    }

    /**
     * A destination refuses what it did not declare, and never quietly renders a subset.
     */
    private static void verifyRefusals() {
        MappingDocumentNode document = new JmmReader().parse(EVERYTHING, "everything.jmm");

        equal("bindings are refused rather than dropped", true,
              refuses(() -> JmmSourceTranslator.INSTANCE.translate(document, Bindings.of("who", "u-42"))));

        // ⚠️ Everything EXCEPT the one construct under test, so the refusal cannot come from something
        // else that happened to be missing too. A narrowed destination is easy to make refuse by
        // accident, and a check that passes for the wrong reason is worse than no check.
        equal("a destination that cannot guard refuses a document with a 'when'", true,
              refuses(() -> JmmSourceTranslator.writing(everythingExcept(JmmCapability.WHEN))
                      .translate(document)));

        equal("and one that cannot ignore refuses a document with an 'ignore'", true,
              refuses(() -> JmmSourceTranslator.writing(everythingExcept(JmmCapability.IGNORE))
                      .translate(document)));

        equal("the structured destination refuses on the same terms", true,
              refuses(() -> JmmJsonTranslator.writing(everythingExcept(JmmCapability.REFUSE))
                      .translate(document)));

        equal("and neither refuses when only the unused construct is withheld", false,
              refuses(() -> JmmSourceTranslator.writing(everythingExcept(JmmCapability.VIA))
                      .translate(document)));

        equal("a destination that declares everything writes it", true,
              JmmSourceTranslator.INSTANCE.translate(document).contains("mapping"));
    }

    /**
     * Every capability but one.
     *
     * @param withheld the one to leave out
     * @return the rest
     */
    private static org.jmouse.el.translate.Capability[] everythingExcept(
            org.jmouse.el.translate.Capability withheld) {
        return java.util.Arrays.stream(JmmCapability.every())
                .filter(capability -> !capability.equals(withheld))
                .toArray(org.jmouse.el.translate.Capability[]::new);
    }

    /**
     * Whether translating refuses.
     *
     * @param translation what to try
     * @return {@code true} where it was refused
     */
    private static boolean refuses(Runnable translation) {
        try {
            translation.run();
            return false;
        } catch (TranslationRefusedException refused) {
            System.out.printf("  ↯ %s%n", refused.getMessage());
            return true;
        }
    }

    private static void equal(String what, Object expected, Object actual) {
        checked++;

        if (!java.util.Objects.equals(expected, actual)) {
            failures++;
            System.out.printf("  ✗ %s%n     expected: %s%n     actual:   %s%n", what, expected, actual);
        }
    }

    private static void holds(String what, String written, String fragment) {
        checked++;

        if (!written.contains(fragment)) {
            failures++;
            System.out.printf("  ✗ %s — nothing matched '%s' in:%n%s%n", what, fragment, written);
        }
    }

    private static void report() {
        if (failures == 0) {
            System.out.printf("%d checks, ALL PASS%n", checked);
            return;
        }

        System.out.printf("%d checks, %d failed%n", checked, failures);
        System.exit(1);
    }
}
