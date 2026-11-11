package org.jmouse.cad.packages.board;

import org.jmouse.cad.board.BoardVocabulary;
import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.packages.PackageTable;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The ways a board can be made, read from a {@code board-rules} table: each with the least copper, gaps and holes it
 * makes, and the tracks and vias a board made that way is laid with. A board names one ({@code rules standard}); its
 * checks hold it to those values and its own changes to them. The values are the ones a board's {@code rules} line may
 * change — {@link BoardVocabulary#RULES} —, so the table and the line cannot name different things.
 *
 * @param usual    the way a board is made when it names none
 * @param profiles every way, in the order the table writes them
 */
public record BoardRules(String usual, List<Profile> profiles) {

    /** The table's own field naming the usual way: {@code usual "standard"}. */
    private static final String USUAL = "usual";

    public BoardRules {
        profiles = List.copyOf(profiles);
    }

    /**
     * The values a board is held to: those of the way it names, else of the usual way, with the board's own changes over
     * them — a change to a word no way has is not a rule, and is passed over.
     */
    public Map<String, Double> valuesOf(Optional<BoardDrawing.Rules> rules) {
        String chosen = rules.map(BoardDrawing.Rules::profile).orElse(usual);
        Profile profile = profiles.stream().filter(candidate -> candidate.id().equals(chosen)).findFirst()
                .or(() -> profiles.stream().filter(candidate -> candidate.id().equals(usual)).findFirst())
                .orElseThrow();
        Map<String, Double> values = new TreeMap<>(profile.values());
        rules.ifPresent(own -> own.changes().forEach((word, value) -> {
            if (values.containsKey(word)) {
                values.put(word, value);
            }
        }));
        return Collections.unmodifiableMap(values);
    }

    /**
     * One way of making boards.
     *
     * @param values every rule, by its word, in millimetres
     */
    public record Profile(String id, String title, Map<String, Double> values) {

        public Profile {
            values = Collections.unmodifiableMap(new TreeMap<>(values));
        }
    }

    /** The rules a {@code board-rules} table writes; refuses one whose usual way is none of its entries. */
    public static BoardRules of(Sheet sheet) {
        List<Profile> profiles = new ArrayList<>();
        for (SheetEntry entry : sheet.entries()) {
            PackageTable table = new PackageTable(entry);
            Map<String, Double> values = new TreeMap<>();
            for (String rule : BoardVocabulary.RULES) {
                values.put(rule, table.length(rule));
            }
            profiles.add(new Profile(entry.id(), entry.title().orElse(entry.id()), values));
        }
        String usual = sheet.field(USUAL)
                .map(field -> field.value() instanceof SheetValue.Text(String text) ? text : field.value().written())
                .orElseThrow(() -> new IllegalArgumentException("The board rules name no usual way: 'usual \"standard\"'"));
        if (profiles.stream().noneMatch(profile -> profile.id().equals(usual))) {
            throw new IllegalArgumentException("The board rules' usual way '" + usual + "' is not one of them");
        }
        return new BoardRules(usual, profiles);
    }
}
