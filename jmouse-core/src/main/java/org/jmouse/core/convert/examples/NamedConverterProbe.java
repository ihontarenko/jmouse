package org.jmouse.core.convert.examples;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.convert.ConverterName;
import org.jmouse.core.convert.ConverterNotFound;
import org.jmouse.core.convert.GenericConverter;
import org.jmouse.core.convert.StandardConversion;

import java.math.BigDecimal;

/**
 * A converter reached by name, and the three things that must stay true when it is. 🏷️
 *
 * <h2>⚠️ The case the whole mechanism exists for</h2>
 *
 * <p>{@code BigDecimal → Money} as an <strong>amount</strong> and as a <strong>rate</strong> are both
 * legal and are different converters. A type pair cannot tell them apart — it has one answer by
 * construction — so before this, one of the two simply could not be registered. The probe below
 * registers both and shows each one being reached.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class NamedConverterProbe {

    private static int checked;
    private static int failures;

    private NamedConverterProbe() {
    }

    public static void main(String... arguments) {
        verifyOnePairTwoConverters();
        verifyNamingDoesNotTouchPairLookup();
        verifyANameIsClaimedOnce();
        verifyAnUnknownNameSaysWhatWouldHaveWorked();
        verifyTheNamingRule();

        report();
    }

    /**
     * Two converters for one pair, told apart by name — which is what a pair cannot do.
     */
    private static void verifyOnePairTwoConverters() {
        Conversion conversion = new StandardConversion();

        conversion.registerConverter("money", BigDecimal.class, Money.class,
                                     amount -> new Money(amount, "amount"));
        conversion.registerConverter(ConverterName.named("shop", "rate").name(),
                                     BigDecimal.class, Money.class,
                                     amount -> new Money(amount, "rate"));

        GenericConverter<BigDecimal, Money> asAmount = conversion.requireConverter("money");
        GenericConverter<BigDecimal, Money> asRate   = conversion.requireConverter("shop.rate");

        equal("one pair, reached as an amount",
              "amount", asAmount.convert(BigDecimal.ONE, Money.class).kind());
        equal("the same pair, reached as a rate",
              "rate", asRate.convert(BigDecimal.ONE, Money.class).kind());
        equal("both names are listed", 2, conversion.converterNames().size());
    }

    /**
     * ⚠️ The rule that makes the two registries worth keeping apart.
     *
     * <p>A named converter answering a pair lookup would mean the rate converter above could be handed
     * to a caller who asked the generic question — silently, and correctly-looking.</p>
     */
    private static void verifyNamingDoesNotTouchPairLookup() {
        Conversion conversion = new StandardConversion();

        conversion.registerConverter("money", BigDecimal.class, Money.class,
                                     amount -> new Money(amount, "amount"));

        equal("naming a converter does not register it for the pair",
              false, conversion.hasConverter(BigDecimal.class, Money.class));
        equal("nor does it put the pair on the type graph",
              false, conversion.hasAnyConverter(BigDecimal.class, Money.class));
        equal("and the name still reaches it",
              true, conversion.getConverter("money") != null);

        // Registering for the pair as well is a second, visible call — never a side effect of the first.
        conversion.registerConverter(BigDecimal.class, Money.class, amount -> new Money(amount, "pair"));

        equal("registering for the pair as well is deliberate and works",
              true, conversion.hasConverter(BigDecimal.class, Money.class));
        equal("and it did not disturb the named one",
              "amount", conversion.<BigDecimal, Money>requireConverter("money")
                      .convert(BigDecimal.ONE, Money.class).kind());
    }

    /**
     * A name is claimed once — the second claim is refused rather than overwriting the first.
     */
    private static void verifyANameIsClaimedOnce() {
        Conversion conversion = new StandardConversion();

        conversion.registerConverter("money", BigDecimal.class, Money.class,
                                     amount -> new Money(amount, "first"));

        equal("a second converter under one name is refused", true, refuses(() ->
                conversion.registerConverter("money", BigDecimal.class, Money.class,
                                             amount -> new Money(amount, "second"))));

        equal("and the first one is untouched",
              "first", conversion.<BigDecimal, Money>requireConverter("money")
                      .convert(BigDecimal.ONE, Money.class).kind());
    }

    /**
     * ⚠️ A name reaches this lookup from a text file somebody typed, so the refusal has to be readable
     * rather than merely correct.
     */
    private static void verifyAnUnknownNameSaysWhatWouldHaveWorked() {
        Conversion empty = new StandardConversion();

        try {
            empty.requireConverter("money");
            fail("an unknown name is refused", "nothing was thrown");
        } catch (ConverterNotFound refused) {
            equal("with nothing registered, the refusal says so",
                  true, refused.getMessage().contains("none is named at all"));
        }

        Conversion conversion = new StandardConversion();

        conversion.registerConverter("money", BigDecimal.class, Money.class,
                                     amount -> new Money(amount, "amount"));
        conversion.registerConverter("shop.address", BigDecimal.class, Money.class,
                                     amount -> new Money(amount, "address"));

        try {
            conversion.requireConverter("monye");
            fail("a typo is refused", "nothing was thrown");
        } catch (ConverterNotFound refused) {
            System.out.printf("  ↯ %s%n", refused.getMessage());
            equal("the refusal lists what would have worked",
                  true, refused.getMessage().contains("money")
                        && refused.getMessage().contains("shop.address"));
        }

        equal("a name nothing claims reads as absent rather than throwing",
              null, conversion.getConverter("monye"));
    }

    /**
     * The naming rule itself — the same one a capability follows, for the same reason.
     */
    private static void verifyTheNamingRule() {
        equal("a bare name belongs to the framework",
              false, ConverterName.of("money").isQualified());
        equal("a dotted name belongs to whoever registered it",
              "shop", ConverterName.named("shop", "address").namespace());
        equal("and it spells out as written",
              "shop.address", ConverterName.named("shop", "address").toString());

        equal("a blank name is refused", true, refuses(() -> ConverterName.of("  ")));
        equal("a leading dot is refused", true, refuses(() -> ConverterName.of(".address")));
        equal("a trailing dot is refused", true, refuses(() -> ConverterName.of("shop.")));
        equal("an untrimmed name is refused", true, refuses(() -> ConverterName.of(" money")));
    }

    private static boolean refuses(Runnable attempt) {
        try {
            attempt.run();
            return false;
        } catch (IllegalArgumentException refused) {
            return true;
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

    /** A value object with two legitimate readings of the same source type. */
    public record Money(BigDecimal value, String kind) {
    }
}
