package org.jmouse.parallax.script;

import org.jmouse.parallax.script.writer.DocumentWriter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A document written from what it holds reads back to the same document: for every text of {@link CatalogueTexts},
 * read, written and read again, the two hold the same — positions apart —, and writing what was read back gives the
 * same text again.
 *
 * <p>Run it over a product's catalogue too: {@code -Djmx.catalogues=…/Parallax/BE/src/main/resources/catalogue}.</p>
 */
class JmxWritingRoundTripTest {

    @Test
    void everyDocumentWrittenBackReadsTheSame() {
        Map<String, String> documents = CatalogueTexts.documents();
        assertFalse(documents.isEmpty());
        List<String> failures = new ArrayList<>();
        documents.forEach((name, text) -> {
            List<ExperienceDocument> read;
            try {
                read = ExperienceReader.read(text, name);
            } catch (ExperienceParseException refused) {
                // A text kept to be refused — a test's broken document — has nothing to write back.
                return;
            }
            String written = DocumentWriter.write(read);
            List<ExperienceDocument> again;
            try {
                again = ExperienceReader.read(written, name);
            } catch (ExperienceParseException refused) {
                failures.add(name + " is refused once written: " + refused.getMessage() + "\n" + numbered(written));
                return;
            }
            String held = DocumentSnapshot.withoutPositions(read);
            String heldAgain = DocumentSnapshot.withoutPositions(again);
            if (!held.equals(heldAgain)) {
                failures.add(name + " says something else once written: " + firstDifference(held, heldAgain) + "\n"
                        + numbered(written));
                return;
            }
            String writtenAgain = DocumentWriter.write(again);
            if (!writtenAgain.equals(written)) {
                failures.add(name + " is written differently the second time: "
                        + firstDifference(written, writtenAgain));
            }
        });
        assertEquals(List.of(), failures.stream().map(failure -> failure.substring(0, failure.indexOf(' '))).toList(),
                () -> failures.isEmpty() ? "" : failures.size() + " documents; the first:\n" + failures.getFirst());
    }

    private static String firstDifference(String expected, String actual) {
        String[] one = expected.split("\n");
        String[] other = actual.split("\n");
        for (int index = 0; index < Math.max(one.length, other.length); index++) {
            String first = index < one.length ? one[index] : "<end>";
            String second = index < other.length ? other[index] : "<end>";
            if (!first.equals(second)) {
                return "line " + (index + 1) + "\n  was:   " + first + "\n  now:   " + second;
            }
        }
        return "nowhere";
    }

    private static String numbered(String text) {
        StringBuilder numbered = new StringBuilder();
        String[] lines = text.split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            numbered.append(String.format("%4d  ", index + 1)).append(lines[index]).append('\n');
        }
        return numbered.toString();
    }
}
