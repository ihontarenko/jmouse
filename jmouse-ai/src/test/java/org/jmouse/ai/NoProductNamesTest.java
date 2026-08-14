package org.jmouse.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No product's name appears anywhere in the library.
 *
 * <p>The rule that cannot be expressed as an import, because the way it is broken is a sentence. This
 * library was drawn from three existing implementations, and the residue of that shows up in a javadoc
 * explaining why something is the way it is <em>in one particular product</em> — which reads as
 * documentation and is actually a dependency on knowledge nobody outside that product has. A reader of
 * the published artifact cannot look any of it up.
 *
 * <p><strong>Source rather than bytecode</strong>, deliberately: comments are where this happens, and
 * comments are exactly what bytecode does not carry. So this walks the source trees of every module in
 * the effort and greps them, which is also what the ticket said to do.
 *
 * <p>⚠️ It is scoped to {@code src/main/java}. A build file naming an artifact called
 * {@code central-publishing-maven-plugin} is not a product reference, and a rule that could not tell
 * those apart would be turned off within a week.
 */
class NoProductNamesTest {

    /**
     * The three that must never appear at all, and the one that has an ordinary English meaning.
     *
     * <p>The fourth is matched case-sensitively as a whole word, so that "nothing central is edited"
     * survives and a product name does not. A rule with false positives is a rule somebody deletes.
     */
    private static final List<Pattern> FORBIDDEN = List.of(
            Pattern.compile("innoventa", Pattern.CASE_INSENSITIVE),
            Pattern.compile("moneta",    Pattern.CASE_INSENSITIVE),
            Pattern.compile("tessera",   Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bCentral\\b"));

    /** Every module of the effort, relative to the repository root. */
    private static final List<String> MODULES = List.of(
            "jmouse-ai",
            "jmouse-ai-provider",
            "jmouse-ai-conversation",
            "jmouse-ai-mcp",
            "jmouse-ai-access",
            "jmouse-ai-jpa",
            "jmouse-spring/jmouse-ai-spring-boot",
            "jmouse-spring/jmouse-ai-management");

    @Test
    @DisplayName("no product name appears in any published module")
    void noProductNameAppears() {
        Path         root      = repositoryRoot();
        List<String> offending = new ArrayList<>();

        MODULES.stream()
                .map(module -> root.resolve(module).resolve("src/main/java"))
                .filter(Files::isDirectory)
                .forEach(sources -> collectOffences(sources, offending));

        assertThat(offending)
                .describedAs("A product's name in a published library is a dependency on knowledge "
                           + "nobody outside that product has, and a reader of the artifact cannot "
                           + "look it up. Say what the rule is instead of who needed it")
                .isEmpty();
    }

    /**
     * ⚠️ The rule has to be able to fail, or it proves only that the test ran.
     *
     * <p>A deliberate violation, checked against the same matcher the sweep uses.
     */
    @Test
    @DisplayName("the rule fails on a deliberate violation")
    void theRuleCanFail() {
        assertThat(mentionsAProduct("This is how Innoventa did it.")).isTrue();
        assertThat(mentionsAProduct("Central owns the translations.")).isTrue();
        assertThat(mentionsAProduct("nothing central is edited")).isFalse();
        assertThat(mentionsAProduct("central-publishing-maven-plugin")).isFalse();
    }

    // ── The sweep ────────────────────────────────────────────────────────────────

    private static void collectOffences(Path sources, List<String> offending) {
        try (Stream<Path> files = Files.walk(sources)) {
            files.filter(file -> file.toString().endsWith(".java")).forEach(file -> {
                List<String> lines = readLines(file);

                for (int number = 0; number < lines.size(); number++) {
                    if (mentionsAProduct(lines.get(number))) {
                        offending.add(file + ":" + (number + 1) + "  " + lines.get(number).trim());
                    }
                }
            });

        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private static boolean mentionsAProduct(String line) {
        return FORBIDDEN.stream().anyMatch(forbidden -> forbidden.matcher(line).find());
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);

        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    /**
     * The repository root, found by climbing until the sibling modules are visible.
     *
     * <p>Surefire runs with the module directory as the working directory, and this test is about
     * modules other than its own — so climbing is the only honest way to reach them, and stopping at
     * the first directory that holds them is what keeps it from wandering into the filesystem.
     */
    private static Path repositoryRoot() {
        Path candidate = Path.of("").toAbsolutePath();

        while (candidate != null && !Files.isDirectory(candidate.resolve("jmouse-ai/src/main/java"))) {
            candidate = candidate.getParent();
        }

        assertThat(candidate)
                .describedAs("the repository root, climbing from " + Path.of("").toAbsolutePath())
                .isNotNull();

        return candidate;
    }
}
