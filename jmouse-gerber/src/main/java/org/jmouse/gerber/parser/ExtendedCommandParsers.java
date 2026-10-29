package org.jmouse.gerber.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.gerber.GerberParseException;
import org.jmouse.gerber.GerberPosition;
import org.jmouse.gerber.GerberShape;
import org.jmouse.gerber.lexer.GerberTokenKind;
import org.jmouse.gerber.node.ApertureDefinition;
import org.jmouse.gerber.node.Attribute;
import org.jmouse.gerber.node.AttributeScope;
import org.jmouse.gerber.node.ExtendedBlock;
import org.jmouse.gerber.node.FormatSpecification;
import org.jmouse.gerber.node.GerberCommand;
import org.jmouse.gerber.node.LoadPolarity;
import org.jmouse.gerber.node.UnitMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The parsers of extended commands — {@code %…*%} of one block —, one per kind. */
final class ExtendedCommandParsers {

    private ExtendedCommandParsers() {
    }

    /** An extended command of one block, told by its body. */
    abstract static class SingleBlockParser extends CommandParser {

        /** Whether a command with this body is this parser's. */
        abstract boolean accepts(String body);

        /** The command the body is. */
        abstract GerberCommand command(String body, GerberPosition position);

        @Override
        public boolean supports(TokenCursor cursor) {
            return cursor.checkAt(0, GerberTokenKind.PERCENT) && cursor.checkAt(1, GerberTokenKind.DATA)
                    && cursor.checkAt(2, GerberTokenKind.END_OF_BLOCK) && cursor.checkAt(3, GerberTokenKind.PERCENT)
                    && accepts(cursor.valueAt(1));
        }

        @Override
        GerberCommand command(TokenCursor cursor, ParserContext context) {
            GerberPosition position = position(cursor);
            cursor.next();
            String body = cursor.next().value();
            cursor.next();
            cursor.next();
            return command(body, position);
        }
    }

    /** {@code %FSLAX46Y46*%}: absolute coordinates, leading zeros omitted — the only form the format still has. */
    static final class FormatParser extends SingleBlockParser {

        private static final Pattern FORMAT = Pattern.compile("FSLAX(\\d)(\\d)Y(\\d)(\\d)");

        @Override
        boolean accepts(String body) {
            return FORMAT.matcher(body).matches();
        }

        @Override
        GerberCommand command(String body, GerberPosition position) {
            Matcher matcher = FORMAT.matcher(body);
            matcher.matches();
            if (!matcher.group(1).equals(matcher.group(3)) || !matcher.group(2).equals(matcher.group(4))) {
                throw new GerberParseException(position, "X and Y are written with the same digits: " + body);
            }
            return new FormatSpecification(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), position);
        }
    }

    /** {@code %MOMM*%} or {@code %MOIN*%}. */
    static final class UnitParser extends SingleBlockParser {

        @Override
        boolean accepts(String body) {
            return body.equals("MOMM") || body.equals("MOIN");
        }

        @Override
        GerberCommand command(String body, GerberPosition position) {
            return new UnitMode(body.equals("MOMM"), position);
        }
    }

    /** {@code %ADD10C,0.25*%}. */
    static final class ApertureParser extends SingleBlockParser {

        private static final Pattern APERTURE = Pattern.compile("ADD(\\d+)(.+)");

        @Override
        boolean accepts(String body) {
            return APERTURE.matcher(body).matches();
        }

        @Override
        GerberCommand command(String body, GerberPosition position) {
            Matcher matcher = APERTURE.matcher(body);
            matcher.matches();
            return new ApertureDefinition(Integer.parseInt(matcher.group(1)), GerberShape.of(matcher.group(2)), position);
        }
    }

    /** {@code %LPD*%} or {@code %LPC*%}. */
    static final class PolarityParser extends SingleBlockParser {

        @Override
        boolean accepts(String body) {
            return body.equals("LPD") || body.equals("LPC");
        }

        @Override
        GerberCommand command(String body, GerberPosition position) {
            return new LoadPolarity(body.equals("LPD"), position);
        }
    }

    /** {@code %TF.FileFunction,Copper,L1,Top*%}, {@code %TA…*%}, {@code %TO…*%}, {@code %TD*%}. */
    static final class AttributeParser extends SingleBlockParser {

        @Override
        boolean accepts(String body) {
            return AttributeScope.ofBody(body).isPresent();
        }

        @Override
        GerberCommand command(String body, GerberPosition position) {
            AttributeScope scope = AttributeScope.ofBody(body).orElseThrow();
            List<String> fields = Arrays.asList(body.substring(scope.command().length()).split(",", -1));
            return new Attribute(scope, fields.getFirst(), fields.subList(1, fields.size()), position);
        }
    }

    /**
     * Any other extended command — a macro, a step and repeat, an old image command — kept as its blocks. Not one of the
     * context's parsers: what none of them supports falls to it.
     */
    static final class OtherExtendedParser extends CommandParser {

        @Override
        public boolean supports(TokenCursor cursor) {
            return cursor.checkAt(0, GerberTokenKind.PERCENT);
        }

        @Override
        GerberCommand command(TokenCursor cursor, ParserContext context) {
            GerberPosition position = position(cursor);
            cursor.next();
            List<String> blocks = new ArrayList<>();
            while (!GerberTokenKind.PERCENT.matches(cursor.peek())) {
                blocks.add(expect(cursor, GerberTokenKind.DATA, "an extended command holds blocks").value());
                expect(cursor, GerberTokenKind.END_OF_BLOCK, "a block ends with '*'");
            }
            cursor.next();
            return new ExtendedBlock(blocks, position);
        }
    }
}
