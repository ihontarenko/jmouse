package org.jmouse.spice.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.spice.SpiceParseException;
import org.jmouse.spice.SpicePosition;
import org.jmouse.spice.lexer.SpiceTokenKind;
import org.jmouse.spice.node.SpiceArgument;
import org.jmouse.spice.node.SpiceCard;
import org.jmouse.spice.node.SpiceParameter;
import org.jmouse.spice.node.SpiceValue;
import org.jmouse.spice.parser.CardReader.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** The parsers of dot cards, one per kind. */
final class CardParsers {

    /** The dot cards a parser of their own reads; any other is a {@link DirectiveParser}'s. */
    private static final Set<String> DEDICATED = Set.of(".model", ".subckt", ".param", ".include", ".inc", ".lib",
            ".tran", ".ac", ".dc", ".op", ".control", ".end", ".ends", ".endc");

    private CardParsers() {
    }

    /** {@code .model name type(name=value …)} or {@code .model name type name=value …}. */
    static final class ModelParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isDirective(head, ".model");
        }

        @Override
        SpiceCard statement(Card card, TokenCursor cursor, ParserContext context) {
            String shape = "a model is '.model name type(name=value …)'";
            card.requireAtLeast(2, shape);
            List<SpiceArgument> arguments = card.arguments();
            String name = CardReader.word(arguments.getFirst(), "a model's name");
            String type;
            List<SpiceArgument> values = new ArrayList<>();
            if (arguments.get(1) instanceof SpiceArgument.Group group) {
                type = group.name();
                values.addAll(group.arguments());
            } else {
                type = CardReader.word(arguments.get(1), "a model's type");
            }
            for (SpiceArgument argument : arguments.subList(2, arguments.size())) {
                if (argument instanceof SpiceArgument.Group group && group.name().isEmpty()) {
                    values.addAll(group.arguments());
                } else {
                    values.add(argument);
                }
            }
            return new SpiceCard.Model(name, type, CardReader.parameters(values, "a model's parameters are 'name=value'"),
                    card.position());
        }
    }

    /** {@code .subckt name pins… [params: name=value …]} … {@code .ends}: its body read as a deck's is. */
    static final class SubcircuitParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isDirective(head, ".subckt");
        }

        @Override
        SpiceCard statement(Card card, TokenCursor cursor, ParserContext context) {
            String shape = "a subcircuit is '.subckt name pins… [params: name=value …]'";
            card.requireAtLeast(1, shape);
            List<SpiceArgument> arguments = card.arguments();
            String name = CardReader.word(arguments.getFirst(), "a subcircuit's name");
            List<String> pins = new ArrayList<>();
            List<SpiceArgument> assignments = new ArrayList<>();
            for (SpiceArgument argument : arguments.subList(1, arguments.size())) {
                switch (argument) {
                    case SpiceArgument.Word word when word.text().equalsIgnoreCase("params:") -> {
                    }
                    case SpiceArgument.Word word when assignments.isEmpty() -> pins.add(word.text());
                    case SpiceArgument.Assignment assignment -> assignments.add(assignment);
                    default -> throw new SpiceParseException(argument.position(), shape);
                }
            }
            SpiceCard.Subcircuit subcircuit = new SpiceCard.Subcircuit(name, pins,
                    CardReader.parameters(assignments, shape), card.position());
            StatementSequence.read(cursor, subcircuit, context, ".ends");
            return subcircuit;
        }
    }

    /** {@code .param name=value …}. */
    static final class ParametersParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isDirective(head, ".param");
        }

        @Override
        SpiceCard statement(Card card, TokenCursor cursor, ParserContext context) {
            List<SpiceParameter> parameters = CardReader.parameters(card.arguments(), "a parameter is 'name=value'");
            return new SpiceCard.Parameters(parameters, card.position());
        }
    }

    /** {@code .include file}, {@code .lib file [section]}. */
    static final class IncludeParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isDirective(head, ".include", ".inc", ".lib");
        }

        @Override
        SpiceCard statement(Card card, TokenCursor cursor, ParserContext context) {
            card.requireAtLeast(1, "it names a file");
            String path = switch (card.arguments().getFirst()) {
                case SpiceArgument.Text text -> text.text();
                case SpiceArgument.Word word -> word.text();
                default -> throw new SpiceParseException(card.arguments().getFirst().position(), "a file is named by a "
                        + "word or a text in quotes");
            };
            String section = card.arguments().size() > 1 ? CardReader.word(card.arguments().get(1), "a section") : null;
            return new SpiceCard.Include(SpiceTokenKind.lower(card.head()).substring(1), path, section, card.position());
        }
    }

    /** {@code .tran step stop [start [largest step]] [uic]}. */
    static final class TransientParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isDirective(head, ".tran");
        }

        @Override
        SpiceCard statement(Card card, TokenCursor cursor, ParserContext context) {
            List<SpiceArgument> arguments = new ArrayList<>(card.arguments());
            boolean initialConditions = !arguments.isEmpty() && arguments.getLast() instanceof SpiceArgument.Word last
                    && last.text().equalsIgnoreCase("uic");
            if (initialConditions) {
                arguments.removeLast();
            }
            if (arguments.size() < 2 || arguments.size() > 4) {
                throw new SpiceParseException(card.position(), "a transient is '.tran step stop [start [largest step]] [uic]'");
            }
            List<SpiceValue> values = new ArrayList<>();
            for (SpiceArgument argument : arguments) {
                values.add(CardReader.value(argument, "a transient's time"));
            }
            return new SpiceCard.Transient(values.get(0), values.get(1), values.size() > 2 ? values.get(2) : null,
                    values.size() > 3 ? values.get(3) : null, initialConditions, card.position());
        }
    }

    /** {@code .ac dec|oct|lin points start stop}. */
    static final class AcParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isDirective(head, ".ac");
        }

        @Override
        SpiceCard statement(Card card, TokenCursor cursor, ParserContext context) {
            card.requireAtLeast(4, "an AC sweep is '.ac dec|oct|lin points start stop'");
            List<SpiceArgument> arguments = card.arguments();
            return new SpiceCard.Ac(CardReader.word(arguments.get(0), "a sweep's variation"),
                    CardReader.value(arguments.get(1), "a sweep's points"), CardReader.value(arguments.get(2), "a sweep's start"),
                    CardReader.value(arguments.get(3), "a sweep's stop"), card.position());
        }
    }

    /** {@code .dc …}: its sweeps as written. */
    static final class DcParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isDirective(head, ".dc");
        }

        @Override
        SpiceCard statement(Card card, TokenCursor cursor, ParserContext context) {
            return new SpiceCard.Dc(card.arguments(), card.position());
        }
    }

    /** {@code .op}. */
    static final class OperatingPointParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isDirective(head, ".op");
        }

        @Override
        SpiceCard statement(Card card, TokenCursor cursor, ParserContext context) {
            return new SpiceCard.OperatingPoint(card.position());
        }
    }

    /** {@code .control} … {@code .endc}: the lines between them as written. */
    static final class ControlParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isDirective(head, ".control");
        }

        @Override
        SpiceCard statement(Card card, TokenCursor cursor, ParserContext context) {
            List<String> lines = new ArrayList<>();
            while (SpiceTokenKind.CONTROL_LINE.matches(cursor.peek())) {
                lines.add(cursor.next().value());
            }
            if (!SpiceTokenKind.isWord(cursor.peek(), ".endc")) {
                throw new SpiceParseException(card.position(), "'.control' is never closed by '.endc'");
            }
            CardReader.read(cursor);
            return new SpiceCard.Control(lines, card.position());
        }
    }

    /** {@code .end}. */
    static final class EndParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isDirective(head, ".end");
        }

        @Override
        SpiceCard statement(Card card, TokenCursor cursor, ParserContext context) {
            return new SpiceCard.End(card.position());
        }
    }

    /** Any other dot card: its word and its arguments as written. */
    static final class DirectiveParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return head.value().startsWith(".") && !DEDICATED.contains(SpiceTokenKind.lower(head));
        }

        @Override
        SpiceCard statement(Card card, TokenCursor cursor, ParserContext context) {
            return new SpiceCard.Directive(SpiceTokenKind.lower(card.head()).substring(1), card.arguments(),
                    card.position());
        }
    }

    /** A {@code .ends} or {@code .endc} where nothing is open: refused, since what it closes is missing. */
    static SpiceParseException stray(Token head, SpicePosition position) {
        return new SpiceParseException(position, "'" + head.value() + "' closes nothing that is open");
    }
}
