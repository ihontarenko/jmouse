package org.jmouse.gerber.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.gerber.GerberParseException;
import org.jmouse.gerber.GerberPosition;
import org.jmouse.gerber.lexer.GerberTokenKind;
import org.jmouse.gerber.node.ApertureDefinition;
import org.jmouse.gerber.node.Comment;
import org.jmouse.gerber.node.EndOfFile;
import org.jmouse.gerber.node.GerberCommand;
import org.jmouse.gerber.node.GerberWords;
import org.jmouse.gerber.node.Interpolation;
import org.jmouse.gerber.node.InterpolationMode;
import org.jmouse.gerber.node.Operation;
import org.jmouse.gerber.node.OperationKind;
import org.jmouse.gerber.node.QuadrantMode;
import org.jmouse.gerber.node.Region;
import org.jmouse.gerber.node.SelectAperture;
import org.jmouse.gerber.node.WordBlock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/** The parsers of word commands — a block of words ending with {@code *} —, one per kind. */
final class WordCommandParsers {

    private WordCommandParsers() {
    }

    /** A word command, told by its words. */
    abstract static class WordParser extends CommandParser {

        /** Whether a block of these words is this parser's. */
        abstract boolean accepts(List<String> words);

        @Override
        public boolean supports(TokenCursor cursor) {
            return cursor.checkAt(0, GerberTokenKind.WORD) && accepts(wordsAhead(cursor));
        }
    }

    /** A word command of one word, this code. */
    abstract static class SingleWordParser extends WordParser {

        abstract boolean accepts(String word);

        abstract GerberCommand command(String word, GerberPosition position);

        @Override
        boolean accepts(List<String> words) {
            return words.size() == 1 && accepts(words.getFirst());
        }

        @Override
        GerberCommand command(TokenCursor cursor, ParserContext context) {
            GerberPosition position = position(cursor);
            return command(readWords(cursor).getFirst(), position);
        }
    }

    /** {@code G04 text*}. */
    static final class CommentParser extends WordParser {

        @Override
        boolean accepts(List<String> words) {
            return GerberWords.sameCode(words.getFirst(), "G04");
        }

        @Override
        GerberCommand command(TokenCursor cursor, ParserContext context) {
            GerberPosition position = position(cursor);
            List<String> words = readWords(cursor);
            return new Comment(words.size() > 1 ? words.get(1) : "", position);
        }
    }

    /** {@code G01*}, {@code G02*}, {@code G03*}. */
    static final class InterpolationParser extends SingleWordParser {

        @Override
        boolean accepts(String word) {
            return Interpolation.ofWord(word).isPresent();
        }

        @Override
        GerberCommand command(String word, GerberPosition position) {
            return new InterpolationMode(Interpolation.ofWord(word).orElseThrow(), position);
        }
    }

    /** {@code G75*} or {@code G74*}. */
    static final class QuadrantParser extends SingleWordParser {

        @Override
        boolean accepts(String word) {
            return GerberWords.sameCode(word, "G75") || GerberWords.sameCode(word, "G74");
        }

        @Override
        GerberCommand command(String word, GerberPosition position) {
            return new QuadrantMode(GerberWords.sameCode(word, "G75"), position);
        }
    }

    /** {@code M02*}. */
    static final class EndOfFileParser extends SingleWordParser {

        @Override
        boolean accepts(String word) {
            return GerberWords.sameCode(word, "M02");
        }

        @Override
        GerberCommand command(String word, GerberPosition position) {
            return new EndOfFile(position);
        }
    }

    /** {@code D10*}: an aperture's code, alone. */
    static final class SelectApertureParser extends SingleWordParser {

        @Override
        boolean accepts(String word) {
            return GerberWords.letterOf(word) == 'D' && isCode(word) && GerberWords.numberOf(word) >= ApertureDefinition.FIRST_CODE;
        }

        @Override
        GerberCommand command(String word, GerberPosition position) {
            return new SelectAperture((int) GerberWords.numberOf(word), position);
        }
    }

    /** {@code G36*} … {@code G37*}: its commands read as a file's are, up to the end. */
    static final class RegionParser extends SingleWordParser {

        @Override
        boolean accepts(String word) {
            return GerberWords.sameCode(word, "G36");
        }

        @Override
        GerberCommand command(String word, GerberPosition position) {
            throw new IllegalStateException("A region is read with its commands");
        }

        @Override
        GerberCommand command(TokenCursor cursor, ParserContext context) {
            GerberPosition position = position(cursor);
            readWords(cursor);
            Region region = new Region(position);
            CommandSequence.read(cursor, region, context, RegionParser::closes);
            return region;
        }

        private static boolean closes(List<String> words) {
            return words.size() == 1 && GerberWords.sameCode(words.getFirst(), "G37");
        }
    }

    /** {@code [G0n]X…Y…I…J…D0n*}: coordinates, each at most once, then what to do there. */
    static final class OperationParser extends WordParser {

        @Override
        boolean accepts(List<String> words) {
            if (OperationKind.ofWord(words.getLast()).isEmpty()) {
                return false;
            }
            int first = Interpolation.ofWord(words.getFirst()).isPresent() ? 1 : 0;
            String seen = "";
            for (String word : words.subList(first, words.size() - 1)) {
                char letter = GerberWords.letterOf(word);
                if ("XYIJ".indexOf(letter) < 0 || seen.indexOf(letter) >= 0 || !isCode(word)) {
                    return false;
                }
                seen += letter;
            }
            return true;
        }

        @Override
        GerberCommand command(TokenCursor cursor, ParserContext context) {
            GerberPosition position = position(cursor);
            List<String> words = readWords(cursor);
            Interpolation interpolation = Interpolation.ofWord(words.getFirst()).orElse(null);
            Map<Character, Long> coordinates = new HashMap<>();
            for (String word : words.subList(interpolation == null ? 0 : 1, words.size() - 1)) {
                coordinates.put(GerberWords.letterOf(word), GerberWords.numberOf(word));
            }
            return new Operation(interpolation, coordinate(coordinates, 'X'), coordinate(coordinates, 'Y'),
                    coordinate(coordinates, 'I'), coordinate(coordinates, 'J'),
                    OperationKind.ofWord(words.getLast()).orElseThrow(), position);
        }

        private static OptionalLong coordinate(Map<Character, Long> coordinates, char letter) {
            Long value = coordinates.get(letter);
            return value == null ? OptionalLong.empty() : OptionalLong.of(value);
        }
    }

    /**
     * Any other word command — {@code G54D10*}, {@code M00*} — kept as its words. Not one of the context's parsers:
     * what none of them supports falls to it.
     */
    static final class OtherWordParser extends WordParser {

        @Override
        boolean accepts(List<String> words) {
            return true;
        }

        @Override
        GerberCommand command(TokenCursor cursor, ParserContext context) {
            GerberPosition position = position(cursor);
            return new WordBlock(readWords(cursor), position);
        }
    }

    /** Whether a word's number is a whole one, as a code's and a coordinate's are. */
    private static boolean isCode(String word) {
        try {
            GerberWords.numberOf(word);
            return true;
        } catch (NumberFormatException notWhole) {
            return false;
        }
    }

    static GerberParseException unexpected(TokenCursor cursor) {
        return new GerberParseException(CommandParser.position(cursor),
                "'" + cursor.peek().value() + "' begins no command Gerber has");
    }
}
