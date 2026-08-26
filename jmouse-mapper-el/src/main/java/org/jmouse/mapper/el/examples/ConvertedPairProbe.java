package org.jmouse.mapper.el.examples;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.convert.StandardConversion;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.Mappers;
import org.jmouse.mapper.el.JmmBinder;
import org.jmouse.mapper.el.JmmReader;
import org.jmouse.mapper.el.parser.JmmSyntaxException;
import org.jmouse.mapper.errors.MappingException;

import java.math.BigDecimal;

/**
 * {@code from X : <expression>} — a pair converted whole, and the compositions it settles. 🎁
 *
 * <h2>⚠️ The case the reference document opens with</h2>
 *
 * <p>§8 of <em>The .jmm mapping declaration</em> is three lines long and was, until this ticket, the
 * only construct in the whole document that parsed, validated, and then died at bind time. The first
 * check below is that example, run as written.</p>
 *
 * <h2>⚠️ Why most of this probe is about what does NOT happen</h2>
 *
 * <p>A whole-pair conversion is not a smaller version of a property mapping — it removes the target
 * construction, the property loop and the write. Four constructs are defined against things that no
 * longer exist, and each of them has an answer that reads as obvious and a second that reads as obvious
 * to somebody else. Written down here so the next reader does not have to guess which was chosen.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ConvertedPairProbe {

    private static int checked;
    private static int failures;

    /** §8, as written in the reference document. */
    private static final String REFERENCE = """
            mapping "money" {

                use java.math.BigDecimal
                use org.jmouse.mapper.el.examples.ConvertedPairProbe$Money

                target Money {
                    from BigDecimal : via("money")
                }
            }
            """;

    /** The same thing with an ordinary expression, so it works without a registered converter. */
    private static final String EXPRESSION = """
            mapping "money" {

                use java.math.BigDecimal
                use org.jmouse.mapper.el.examples.ConvertedPairProbe$Money

                target Money {
                    from BigDecimal : source | via("money")
                }
            }
            """;

    private static final String WITH_REFUSALS = """
            mapping "guarded" {

                use java.math.BigDecimal
                use org.jmouse.mapper.el.examples.ConvertedPairProbe$Money

                target Money {

                    refuse target after {
                        printed is null : "a money value was produced with nothing printed"
                    }

                    from BigDecimal : via("money")
                }
            }
            """;

    private static final String TARGET_BEFORE = """
            mapping "impossible" {

                use java.math.BigDecimal
                use org.jmouse.mapper.el.examples.ConvertedPairProbe$Money

                target Money {

                    refuse target before {
                        printed is null : "never runs"
                    }

                    from BigDecimal : via("money")
                }
            }
            """;

    private static final String UNMAPPED_FAIL = """
            mapping "impossible" {

                use java.math.BigDecimal
                use org.jmouse.mapper.el.examples.ConvertedPairProbe$Money

                target Money {
                    unmapped fail
                    from BigDecimal : via("money")
                }
            }
            """;

    private ConvertedPairProbe() {
    }

    public static void main(String... arguments) {
        verifyTheReferenceExampleRuns();
        verifyAnOrdinaryExpressionWorksToo();
        verifyRefuseTargetAfterRuns();
        verifyWhatIsRefusedWhenTheFileIsRead();
        verifyACallerSuppliedInstanceIsDiscarded();

        report();
    }

    /**
     * §8, run as written: {@code from BigDecimal : via("money")}.
     */
    private static void verifyTheReferenceExampleRuns() {
        Money money = mapperReading(REFERENCE).map(new BigDecimal("120.50"), Money.class);

        equal("the reference document's own example runs", "UAH 120.50", money.printed());
    }

    /**
     * ⚠️ {@code via} is not special to this form. The right-hand side is an ordinary expression and
     * every filter composes with it, which is the whole reason §8 needed no clause of its own.
     */
    private static void verifyAnOrdinaryExpressionWorksToo() {
        Money money = mapperReading(EXPRESSION).map(new BigDecimal("7.00"), Money.class);

        equal("any expression works, not only a bare via()", "UAH 7.00", money.printed());
    }

    /**
     * The one refusal phase that still has something to check: the object the expression produced.
     */
    private static void verifyRefuseTargetAfterRuns() {
        Mapper mapper = mapperReading(WITH_REFUSALS);

        equal("a converted pair still honours 'refuse target after'",
              "UAH 3.00", mapper.map(new BigDecimal("3.00"), Money.class).printed());

        try {
            mapper.map(new BigDecimal("-1"), Money.class);
            fail("and it refuses when the assertion holds", "nothing was thrown");
        } catch (MappingException refused) {
            System.out.printf("  ↯ %s%n", refused.getMessage());
            equal("and it refuses when the assertion holds",
                  true, refused.getMessage().contains("nothing printed"));
        }
    }

    /**
     * ⚠️ Two constructs are refused when the file is read rather than ignored at runtime — a statement
     * the file makes that the engine silently does not honour is the failure this language exists
     * against, and both of these would otherwise be exactly that.
     */
    private static void verifyWhatIsRefusedWhenTheFileIsRead() {
        equal("'refuse target before' is refused, because it could never run", true,
              refusesToLoad(TARGET_BEFORE, "can never run"));

        equal("'unmapped fail' is refused, because nothing is filled property by property", true,
              refusesToLoad(UNMAPPED_FAIL, "no unmapped property"));
    }

    /**
     * ⚠️ The expression produces the object, so an instance the caller brought has nowhere to go.
     * Checked rather than assumed, because "the mapper quietly returned a different object" is not
     * something a caller would notice until much later.
     */
    private static void verifyACallerSuppliedInstanceIsDiscarded() {
        Money supplied = new Money("SUPPLIED");
        Money produced = mapperReading(REFERENCE).map(new BigDecimal("9.00"), supplied);

        equal("a caller-supplied instance is discarded", "UAH 9.00", produced.printed());
        equal("and it really is a different object", false, produced == supplied);
    }

    /**
     * A mapper reading one document, with the {@code money} converter registered.
     *
     * @param file the document
     * @return the mapper
     */
    private static Mapper mapperReading(String file) {
        Conversion conversion = new StandardConversion();

        conversion.registerConverter("money", BigDecimal.class, Money.class,
                                     amount -> amount.signum() < 0
                                             ? new Money(null)
                                             : new Money("UAH " + amount.toPlainString()));

        JmmReader reader = new JmmReader(
                new JmmBinder(new ExpressionLanguage(),
                              Thread.currentThread().getContextClassLoader(),
                              conversion));

        return Mappers.builder()
                .rules(builder -> builder.ruleSource(reader.read(file, "probe.jmm")))
                .build();
    }

    /**
     * Whether a document is refused when it is read, for the stated reason.
     *
     * @param file     the document
     * @param expected a fragment the refusal must carry
     * @return {@code true} where it was refused with that reason
     */
    private static boolean refusesToLoad(String file, String expected) {
        try {
            mapperReading(file);
            return false;
        } catch (JmmSyntaxException refused) {
            System.out.printf("  ↯ %s%n", refused.getMessage());
            return refused.getMessage().contains(expected);
        }
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

    /** A value object built in one step rather than assembled from properties. */
    public static class Money {

        private String printed;

        public Money() {
        }

        public Money(String printed) {
            this.printed = printed;
        }

        public String getPrinted() {
            return printed;
        }

        public void setPrinted(String printed) {
            this.printed = printed;
        }

        public String printed() {
            return printed;
        }
    }
}
