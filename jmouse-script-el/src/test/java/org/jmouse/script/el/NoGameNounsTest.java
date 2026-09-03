package org.jmouse.script.el;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * No game's vocabulary appears anywhere in the library.
 *
 * <h2>Why this test exists at all</h2>
 *
 * <p>The dialect was written because a game needed it, its end-to-end suite is a mission file, and its
 * fake host declares {@code world}, {@code player}, {@code mission} and {@code orders}. Every one of
 * those is right where it is — a game exercises every construction at once, which makes it the cheapest
 * honest fixture there is — and every one of them is one careless copy away from {@code main}.</p>
 *
 * <p>⚠️ <strong>The way it would break is a javadoc, not an import.</strong> Nobody is going to add a
 * dependency on a game. What happens is that somebody explains a design decision by saying what it was
 * for — "so a harvester can…" — and that reads as documentation while actually being a dependency on
 * knowledge no other consumer has. A reader of the published artifact cannot look any of it up.</p>
 *
 * <p><strong>Source rather than bytecode</strong>, deliberately: comments are where this happens, and
 * comments are exactly what bytecode does not carry.</p>
 *
 * <p>Modelled on {@code jmouse-ai}'s {@code NoProductNamesTest}, which is the same rule about a
 * different kind of name — and is a better tool for this than ArchUnit, which reads types and would
 * never see the sentence.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
class NoGameNounsTest {

    /**
     * The words that must not appear in {@code main}.
     *
     * <p>⚠️ Chosen to be words with no other meaning in a scripting library. {@code state}, {@code map}
     * and {@code point} are deliberately absent however game-ish they sound — a rule with false
     * positives is a rule somebody switches off within a week.</p>
     *
     * <p>{@code unit} is matched as a whole word so that {@code unitOfWork} and {@code united} survive,
     * and because the fixture's own {@code unit} is the one most likely to be pasted.</p>
     */
    private static final List<Pattern> FORBIDDEN = List.of(
            Pattern.compile("\\bharvester\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bfaction\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bspice\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\brefinery\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bmission\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bunit\\b", Pattern.CASE_INSENSITIVE));

    @Test
    @DisplayName("no game noun appears in src/main/java")
    void noGameNounAppears() {
        List<String> offending = new ArrayList<>();

        try (Stream<Path> sources = Files.walk(Path.of("src/main/java"))) {
            sources.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> collect(path, offending));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }

        assertTrue(offending.isEmpty(),
                   "A game's vocabulary in a general-purpose dialect is a dependency on knowledge no "
                   + "other consumer has. Say what the rule is instead of who needed it:\n"
                   + String.join("\n", offending));
    }

    /**
     * ⚠️ The rule has to be able to fail, or it proves only that the test ran.
     */
    @Test
    @DisplayName("the rule fails on a deliberate violation")
    void theRuleCanFail() {
        assertTrue(mentionsAGame("so a harvester can find the nearest refinery"));
        assertTrue(mentionsAGame("A unit is handed to the handler."));
        assertFalse(mentionsAGame("the unitOfWork is committed by the host"));
        assertFalse(mentionsAGame("a united set of names"));
        assertFalse(mentionsAGame("a facade the host declared"));
    }

    private static void collect(Path source, List<String> offending) {
        try {
            List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);

            for (int index = 0; index < lines.size(); index++) {
                if (mentionsAGame(lines.get(index))) {
                    offending.add("%s:%d  %s".formatted(source, index + 1, lines.get(index).trim()));
                }
            }
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static boolean mentionsAGame(String line) {
        return FORBIDDEN.stream().anyMatch(pattern -> pattern.matcher(line).find());
    }
}
