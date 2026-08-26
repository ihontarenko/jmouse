package org.jmouse.mapper.el.examples;

import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.Mappers;
import org.jmouse.mapper.el.JmmReader;
import org.jmouse.mapper.el.loader.JmmLoader;
import org.jmouse.mapper.el.loader.JmmSource;
import org.jmouse.mapper.el.loader.JmmSources;
import org.jmouse.mapper.el.parser.JmmSyntaxException;

import java.util.List;

/**
 * {@code .jmm} documents found on the classpath and applied, with nothing wired by hand. 📚
 *
 * <h2>⚠️ The check that matters is what is ABSENT from this file</h2>
 *
 * <p>{@link JmmReader} is named once, to build the loader. No document is pasted into a string, no
 * file name is typed, and no rule source is assembled. That was the state of the language until this
 * ticket: every caller in the repository was a driver holding a document in a text block, which meant
 * a product wanting {@code .jmm} had to write the wiring itself — a language that was finished and
 * unreachable.</p>
 *
 * <p>The documents are under {@code examples/mapping} on this module's own classpath, so what is being
 * proved is the ordinary case rather than a path arranged for the test.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class LoadedMappingProbe {

    private static int checked;
    private static int failures;

    private LoadedMappingProbe() {
    }

    public static void main(String... arguments) {
        verifyTwoDocumentsAreFoundAndApplied();
        verifyTwoFilesClaimingOneTargetRefuseTheLoad();
        verifyALocationNamingNothingIsNotAnError();
        verifyACommentChangesNothing();

        report();
    }

    /**
     * ⚠️ A comment in every position the reference document's §14 example uses, and none of them
     * changes what the mapping does.
     *
     * <p>Compared against the same document with the comments stripped rather than asserted against a
     * literal, because the failure to guard against is not "it did not parse" — it is a trailing
     * comment being sliced into the rule's value, compiled, and quietly written to the property. That
     * produces a mapping that loads clean and is wrong, which no parse check would catch.</p>
     */
    private static void verifyACommentChangesNothing() {
        String commented = """
                # ── a header block, the way §14 opens ────────────────────────────
                # Two lines of it, in fact.

                mapping "commented" {

                    # above an import
                    use org.jmouse.mapper.el.examples.LoadedMappingProbe$OrderRequest
                    use org.jmouse.mapper.el.examples.LoadedMappingProbe$Order

                    target Order {          # trailing, on a block header

                        # above a from
                        from OrderRequest {
                            reference : reference | trim | upper   # trailing, on a rule
                            secret    : ignore                     # and on an ignore
                        }
                    }
                }
                """;

        String bare = """
                mapping "bare" {
                    use org.jmouse.mapper.el.examples.LoadedMappingProbe$OrderRequest
                    use org.jmouse.mapper.el.examples.LoadedMappingProbe$Order

                    target Order {
                        from OrderRequest {
                            reference : reference | trim | upper
                            secret    : ignore
                        }
                    }
                }
                """;

        OrderRequest request = new OrderRequest();

        request.setReference("  ord-9  ");
        request.setSecret("do not carry");

        Order withComments = mapperReading(commented).map(request, Order.class);
        Order without      = mapperReading(bare).map(request, Order.class);

        equal("a commented document maps the same as a bare one",
              without.getReference(), withComments.getReference());
        equal("and the trailing comment did not reach the value", "ORD-9", withComments.getReference());
        equal("an ignore with a trailing comment is still an ignore", null, withComments.getSecret());
    }

    /**
     * A mapper reading one document held in a string.
     *
     * @param document the file
     * @return the mapper
     */
    private static Mapper mapperReading(String document) {
        return Mappers.builder()
                .rules(builder -> builder.ruleSource(new JmmReader().read(document, "inline.jmm")))
                .build();
    }

    /**
     * Two files, two targets, both found by walking a classpath directory and both in effect.
     */
    private static void verifyTwoDocumentsAreFoundAndApplied() {
        Mapper mapper = Mappers.builder()
                .rules(builder -> builder.ruleSource(
                        new JmmLoader(JmmSources.ofClasspath(), new JmmReader())
                                .load(List.of("classpath:examples/mapping"))))
                .build();

        OrderRequest request = new OrderRequest();

        request.setReference("  ord-1  ");
        request.setBuyer("  John  ");
        request.setSecret("do not carry");

        Order order = mapper.map(request, Order.class);

        equal("a document on the classpath is found and applied", "ORD-1", order.getReference());
        equal("and its 'ignore' is honoured", null, order.getSecret());

        Receipt receipt = mapper.map(request, Receipt.class);

        equal("a second document, for a different target, applies too", "John", receipt.getWho());
    }

    /**
     * ⚠️ §13 of the reference document, enforced at load: a target belongs to one file.
     *
     * <p>The refusal names <strong>both</strong> files, because one name leaves whoever reads it
     * hunting for the other.</p>
     */
    private static void verifyTwoFilesClaimingOneTargetRefuseTheLoad() {
        try {
            new JmmLoader(JmmSources.ofClasspath(), new JmmReader())
                    .load(List.of("classpath:examples/clashing"));
            fail("two files claiming one target refuse the load", "nothing was thrown");
        } catch (JmmSyntaxException refused) {
            System.out.printf("  > %s%n", refused.getMessage());
            equal("two files claiming one target refuse the load", true,
                  refused.getMessage().contains("first.jmm")
                  && refused.getMessage().contains("second.jmm"));
        }
    }

    /**
     * A location naming nothing is empty, not broken — a module that ships no mappings is ordinary.
     */
    private static void verifyALocationNamingNothingIsNotAnError() {
        List<JmmSource> found = JmmSources.ofClasspath().at("classpath:examples/nothing-here");

        equal("a location naming nothing is empty rather than a failure", true, found.isEmpty());

        equal("and loading it produces an empty rule source", 0,
              new JmmLoader(JmmSources.ofClasspath(), new JmmReader())
                      .load(List.of("classpath:examples/nothing-here")).size());
    }

    private static void equal(String what, Object expected, Object actual) {
        checked++;

        if (!java.util.Objects.equals(expected, actual)) {
            failures++;
            System.out.printf("  x %s: expected '%s', got '%s'%n", what, expected, actual);

            return;
        }

        System.out.printf("  + %s%n", what);
    }

    private static void fail(String what, String why) {
        checked++;
        failures++;
        System.out.printf("  x %s: %s%n", what, why);
    }

    private static void report() {
        if (failures == 0) {
            System.out.printf("%n%d checks, ALL PASS%n", checked);

            return;
        }

        System.out.printf("%n%d checks, %d failed%n", checked, failures);
        System.exit(1);
    }

    /** The source both documents read from. */
    public static class OrderRequest {

        private String reference;
        private String buyer;
        private String secret;

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }

        public String getBuyer() {
            return buyer;
        }

        public void setBuyer(String buyer) {
            this.buyer = buyer;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    /** What {@code orders.jmm} builds. */
    public static class Order {

        private String reference;
        private String secret;

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    /** What {@code receipts.jmm} builds. */
    public static class Receipt {

        private String who;

        public String getWho() {
            return who;
        }

        public void setWho(String who) {
            this.who = who;
        }
    }
}
