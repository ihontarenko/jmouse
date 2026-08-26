package org.jmouse.access.el;

import org.jmouse.access.el.Smoke.Verification;
import org.jmouse.access.policy.PolicyDocuments;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.TranslationRefusedException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Every {@code .jmp} file four products actually ship, put through the translator.
 *
 * <h2>⚠️ Why a corpus rather than a fixture</h2>
 *
 * <p>{@link ExchangeSmoke} proves the round trip against one fixture written to exercise the grammar.
 * That is the right test for the grammar and the wrong one for a <strong>change to the writer</strong>:
 * a stored policy revision is source text an installation can revert to, so what matters is not that
 * some document survives but that <em>the documents that exist</em> come back character for character.
 * There are twenty-seven of them and none was written to be convenient.</p>
 *
 * <h2>⚠️ The check that makes it a regression test and not a round trip</h2>
 *
 * <p>{@code PolicyWriter.write(document)} now goes through {@link PolicySourceTranslator};
 * {@code PolicyWriter.toNode(document).toSource()} is what it did before, character for character.
 * Comparing the two is the only honest way to say the output did not move — a round trip would still
 * pass if the writer had started emitting something different that merely parsed back the same.</p>
 *
 * <p>Point it at a directory: {@code PolicyCorpusSmoke ../jMouseProjects}. A tree with no policy in
 * it is reported rather than passing silently, because a corpus check over nothing is the failure
 * that looks most like a success.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class PolicyCorpusSmoke {

    private static final String DEFAULT_ROOT = "../jMouseProjects";

    private PolicyCorpusSmoke() {
    }

    public static void main(String[] arguments) {
        Path                root         = Path.of(arguments.length > 0 ? arguments[0] : DEFAULT_ROOT);
        ExpressionEvaluator evaluator    = new ExpressionEvaluator();
        Verification        verification = new Verification();
        List<Path>          policies     = policiesUnder(root);

        System.out.printf("%d policy files under %s%n", policies.size(), root.toAbsolutePath());

        verification.section("corpus");
        verification.equal("the corpus is not empty", true, !policies.isEmpty());

        int translated = 0;

        for (Path policy : policies) {
            if (verifyOneFile(evaluator, verification, root, policy)) {
                translated++;
            }
        }

        verifyRefusesWhatItCannotWrite(evaluator, verification);

        System.out.printf("%d of %d files translated%n", translated, policies.size());
        verification.report();
    }

    /**
     * Puts one file through every check.
     *
     * <p>⚠️ <strong>A file that will not parse is reported and not counted as a failure.</strong> The
     * translator was never reached, so there is nothing it did wrong — counting it would make this
     * driver report red for a reason it cannot fix, which is how a gate stops being read. It is still
     * printed, loudly, because a policy in the tree that does not parse is worth somebody's attention;
     * it is simply somebody else's.</p>
     *
     * @param evaluator    the language
     * @param verification where results are recorded
     * @param root         what the reported name is relative to
     * @param policy       the file
     * @return {@code true} where the file could be read at all
     */
    private static boolean verifyOneFile(
            ExpressionEvaluator evaluator, Verification verification, Path root, Path policy) {
        String name = root.relativize(policy).toString().replace('\\', '/');

        verification.section(name);

        try {
            String         source   = Files.readString(policy);
            PolicyDocument document = evaluator.parse(source, name);

            verification.equal("the translator writes exactly what the tree wrote before",
                               PolicyWriter.toNode(document).toSource(), PolicyWriter.write(document));

            String written = PolicyWriter.write(document);

            verification.equal("what it wrote parses back to the same document",
                               PolicyDocuments.withoutSourcePositions(document),
                               PolicyDocuments.withoutSourcePositions(evaluator.parse(written, name)));

            verification.equal("writing what was written changes nothing",
                               written, PolicyWriter.write(evaluator.parse(written, name)));

            String rewritten = evaluator.rewrite(source);

            verification.equal("rewriting an already-rewritten file changes nothing",
                               rewritten, evaluator.rewrite(rewritten));

            return true;
        } catch (IOException | RuntimeException exception) {
            System.out.printf("  ⚠ [%s] does not parse, so nothing was translated: %s%n",
                              name, exception.getMessage());

            return false;
        }
    }

    /**
     * The three refusals, each of which would otherwise be a silent loss.
     *
     * <p>⚠️ The narrowed destination is the one that matters. A translator declaring every capability
     * can never refuse one, so a capability set nobody can narrow is a set that proves nothing — this
     * is where {@link PolicySourceTranslator#writing} earns being public.</p>
     *
     * @param evaluator    the language
     * @param verification where results are recorded
     */
    private static void verifyRefusesWhatItCannotWrite(
            ExpressionEvaluator evaluator, Verification verification) {
        verification.section("refusals");

        PolicyDocument document = evaluator.parse("""
                declare permissions {
                    form:read "read a form"
                }

                assign subject "u-42" {
                    @INSTALLATION  form:read
                }
                """, "narrow");

        verification.equal("a caller supplying values by name is told, not ignored",
                           true, refuses(() -> PolicySourceTranslator.INSTANCE.translate(
                                   PolicyWriter.toNode(document), Bindings.of("who", "u-42"))));

        verification.equal("a destination that does not write subjects refuses this document",
                           true, refuses(() -> PolicySourceTranslator
                                   .writing(PolicyCapability.PERMISSIONS)
                                   .translate(PolicyWriter.toNode(document))));

        verification.equal("and writes one it does declare",
                           true, PolicySourceTranslator
                                   .writing(PolicyCapability.PERMISSIONS, PolicyCapability.SUBJECTS)
                                   .translate(PolicyWriter.toNode(document))
                                   .contains("form:read"));
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

    /**
     * Every policy file under a directory, in a stable order.
     *
     * @param root where to look
     * @return the files, sorted so two runs report in the same order
     */
    private static List<Path> policiesUnder(Path root) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }

        try (Stream<Path> tree = Files.walk(root)) {
            List<Path> policies = new ArrayList<>(tree
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jmp"))
                    .filter(path -> !path.toString().contains("target"))
                    .toList());

            policies.sort(Comparator.comparing(Path::toString));

            return policies;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
