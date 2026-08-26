package org.jmouse.mapper.el.examples;

import org.jmouse.core.reflection.ClassMatchers;
import org.jmouse.mapper.el.JmmReader;
import org.jmouse.mapper.el.builder.MappableTypes;
import org.jmouse.mapper.el.builder.MappingDraft;
import org.jmouse.mapper.el.builder.MappingDraft.MappingRow;
import org.jmouse.mapper.el.builder.MappingDraft.SourceDraft;
import org.jmouse.mapper.el.builder.MappingDraft.TargetDraft;
import org.jmouse.mapper.el.builder.MappingDrafts;
import org.jmouse.mapper.el.builder.UnshowableMappingException;
import org.jmouse.mapper.el.translate.JmmSourceTranslator;

import java.util.List;

/**
 * A form's rows into a document, and a document back into rows. 🔁
 *
 * <h2>⚠️ The check that matters is the round trip, not the rendering</h2>
 *
 * <p>That rows render as plausible `.jmm` proves the builder agrees with whoever wrote the test. What
 * has to hold is that <strong>a document rendered from rows parses back into the same rows</strong> —
 * because the two tabs of a builder are one thing seen twice, and a screen where switching tabs changes
 * the mapping is a screen nobody can trust with a file they care about.</p>
 *
 * <h2>⚠️ And the refusals, which are the feature rather than its edges</h2>
 *
 * <p>Four constructs have no row. Each of them makes the form say so and stop, because the alternative
 * — showing what it understands and leaving the rest out of view — produces a form that saves and a
 * save that deletes what it never showed.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class MappingBuilderProbe {

    private static int checked;
    private static int failures;

    private MappingBuilderProbe() {
    }

    public static void main(String... arguments) {
        verifyRowsBecomeADocument();
        verifyTheRoundTripHolds();
        verifyWhatTheFormCannotShow();
        verifyTheTypeCatalogue();

        report();
    }

    /**
     * The rows a form holds, rendered through the translator an editor saves through.
     */
    private static void verifyRowsBecomeADocument() {
        String rendered = render(draft());

        System.out.println("=== rendered from rows ===");
        System.out.println(rendered);

        holds("a rule row renders as a rule", rendered, "reference : reference | trim | upper");
        holds("a conditional row keeps its condition", rendered, "tier : \"gold\" when total > 1000");
        holds("an ignored row renders as ignore", rendered, "secret : ignore");
        holds("an always row lands in an always block", rendered, "always {");
        holds("and the imports are written", rendered, "use java.lang.String");
    }

    /**
     * ⚠️ Rows → document → text → document → rows, and the two ends must agree.
     */
    private static void verifyTheRoundTripHolds() {
        MappingDraft first  = draft();
        String       text   = render(first);
        MappingDraft second = MappingDrafts.toDraft(new JmmReader().parse(text, "builder.jmm"));

        equal("the document parses back into the same rows", first, second);

        // ⚠️ Rendered twice, because "the rows survived" and "the text is stable" are different claims
        // and a builder needs both — one for the form, one for whatever stores the file.
        equal("and rendering what was parsed changes nothing", text, render(second));
    }

    /**
     * ⚠️ The four constructs that have no row, each refused by name.
     */
    private static void verifyWhatTheFormCannotShow() {
        refuses("a fragment", """
                mapping "x" {
                    fragment auditing { auditNote : ignore }
                    target String { from String { } }
                }
                """, "fragment");

        refuses("an include", """
                mapping "x" {
                    target String { from String { include auditing } }
                }
                """, "include");

        refuses("a let", """
                mapping "x" {
                    target String { from String { let full = a } }
                }
                """, "let");

        refuses("a refusal", """
                mapping "x" {
                    target String {
                        refuse target after { a is null : "no" }
                        from String { }
                    }
                }
                """, "refuse");

        refuses("a whole-pair conversion", """
                mapping "x" {
                    target String { from String : via("money") }
                }
                """, "via");
    }

    /**
     * The two selects, and the escape hatch beside them.
     */
    private static void verifyTheTypeCatalogue() {
        List<MappableTypes.MappableType> offered = MappableTypes.offered(
                ClassMatchers.nameEnds("Probe").and(ClassMatchers.isPublic()),
                MappingBuilderProbe.class);

        equal("a product's own matcher decides what the select offers",
              true, offered.stream().anyMatch((type) -> type.simple().equals("MappingBuilderProbe")));

        // ⚠️ The scan is a listing, not a filter on what may be mapped — so a type it never offered
        // still has to be nameable, or the builder invents a restriction the engine does not have.
        MappableTypes.MappableShape named = MappableTypes.named(
                "org.jmouse.mapper.el.examples.JmmSmoke$Order");

        equal("and a type it did NOT offer can still be named", "Order", named.simple());
        equal("a target property is reported writable",
              true, named.properties().stream()
                      .anyMatch((property) -> property.name().equals("status") && property.writable()));

        // ⚠️ A RECORD, and this used to be a 500 rather than an answer. A record's components have no
        // setters, so asking each property `isWritable()` refused — and a product whose wire types are
        // records could not open a single one of the types its own select had just offered. What a
        // record can be given is its components, and every one of them is both readable and writable
        // because that is exactly what being built through a constructor means.
        MappableTypes.MappableShape asRecord = MappableTypes.named(
                "org.jmouse.mapper.el.examples.JmmSmoke$Receipt");

        equal("a RECORD can be named at all", "Receipt", asRecord.simple());
        equal("its component is offered as a target", true,
              asRecord.properties().stream()
                      .anyMatch((property) -> property.name().equals("who") && property.writable()));
        equal("...and as a source", true,
              asRecord.properties().stream()
                      .anyMatch((property) -> property.name().equals("who") && property.readable()));

        try {
            MappableTypes.named("net.nowhere.Missing");
            fail("a name nothing answers to is refused", "nothing was thrown");
        } catch (IllegalArgumentException refused) {
            System.out.printf("  > %s%n", refused.getMessage());
            equal("a name nothing answers to is refused, and says how a nested type is spelled",
                  true, refused.getMessage().contains("Outer$Inner"));
        }
    }

    /**
     * The draft a form would hold after somebody filled it in.
     *
     * @return the rows
     */
    private static MappingDraft draft() {
        return new MappingDraft(
                "builder/orders",
                List.of("java.lang.String", "java.math.BigDecimal"),
                List.of(new TargetDraft(
                        "Order",
                        List.of(MappingRow.of("status", "\"CREATED\"")),
                        List.of(new SourceDraft("OrderRequest", List.of(
                                MappingRow.of("reference", "reference | trim | upper"),
                                MappingRow.when("tier", "\"gold\"", "total > 1000"),
                                MappingRow.ignored("secret")))))));
    }

    /**
     * Rows to text, the way the server does it.
     *
     * <p>⚠️ Through {@code JmmSourceTranslator}, never by assembling a string. That is the one rule this
     * whole feature rests on: a browser that writes the language is a second implementation of it.</p>
     *
     * @param draft the rows
     * @return the document
     */
    private static String render(MappingDraft draft) {
        return JmmSourceTranslator.INSTANCE.translate(MappingDrafts.toDocument(draft));
    }

    /**
     * Whether a document is refused as unshowable, naming the construct.
     *
     * @param what      what is being checked, for the report
     * @param document  the file
     * @param construct what the refusal must name
     */
    private static void refuses(String what, String document, String construct) {
        try {
            MappingDrafts.toDraft(new JmmReader().parse(document, "probe.jmm"));
            fail(what + " has no row and is refused", "nothing was thrown");
        } catch (UnshowableMappingException refused) {
            checked++;

            if (!refused.construct().contains(construct)) {
                failures++;
                System.out.printf("  x %s: refused as '%s', expected it to name '%s'%n",
                                  what, refused.construct(), construct);

                return;
            }

            System.out.printf("  + %s has no row and is refused%n", what);
        }
    }

    private static void equal(String what, Object expected, Object actual) {
        checked++;

        if (!java.util.Objects.equals(expected, actual)) {
            failures++;
            System.out.printf("  x %s%n     expected: %s%n     actual:   %s%n", what, expected, actual);

            return;
        }

        System.out.printf("  + %s%n", what);
    }

    private static void holds(String what, String text, String fragment) {
        checked++;

        if (!text.contains(fragment)) {
            failures++;
            System.out.printf("  x %s — nothing matched '%s'%n", what, fragment);

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
}
