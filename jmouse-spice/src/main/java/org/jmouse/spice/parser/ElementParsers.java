package org.jmouse.spice.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.spice.SpiceParseException;
import org.jmouse.spice.lexer.SpiceTokenKind;
import org.jmouse.spice.node.SpiceArgument;
import org.jmouse.spice.node.SpiceElement;
import org.jmouse.spice.node.SpiceParameter;
import org.jmouse.spice.node.SpiceValue;
import org.jmouse.spice.node.SpiceWaveform;
import org.jmouse.spice.parser.CardReader.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** The parsers of element lines, one per kind, each told its cards by the letter of their name. */
final class ElementParsers {

    /** The letters a parser of its own reads; any other letter is a {@link GenericParser}'s. */
    private static final String DEDICATED = "RCLVIDQMXBEFGHK";

    private ElementParsers() {
    }

    /** {@code R}, {@code C}, {@code L}: two nodes and a value. */
    static final class PassiveParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isElement(head, "RCL");
        }

        @Override
        SpiceElement statement(Card card, TokenCursor cursor, ParserContext context) {
            card.requireAtLeast(3, "a resistor, capacitor or inductor is 'name node node value'");
            List<String> nodes = card.words(0, 2, "its nodes are names");
            List<SpiceArgument> arguments = card.arguments();
            return new SpiceElement.Passive(card.head().value(), nodes.get(0), nodes.get(1),
                    CardReader.value(arguments.get(2), card.head().value() + "'s value"),
                    arguments.subList(3, arguments.size()), card.position());
        }
    }

    /** {@code V}, {@code I}: two nodes, a DC value, a transient function, and what else it says. */
    static final class SourceParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isElement(head, "VI");
        }

        @Override
        SpiceElement statement(Card card, TokenCursor cursor, ParserContext context) {
            card.requireAtLeast(2, "a source is 'name node node [DC value] [SIN(…)|PULSE(…)|…]'");
            List<String> nodes = card.words(0, 2, "its nodes are names");
            List<SpiceArgument> arguments = card.arguments();
            String name = card.head().value();
            SpiceValue direct = null;
            boolean directWord = false;
            SpiceWaveform waveform = null;
            List<SpiceArgument> rest = new ArrayList<>();
            int index = 2;
            while (index < arguments.size()) {
                SpiceArgument argument = arguments.get(index);
                if (argument instanceof SpiceArgument.Word word && word.text().equalsIgnoreCase("dc")
                        && index + 1 < arguments.size()) {
                    direct = CardReader.value(arguments.get(index + 1), name + "'s DC value");
                    directWord = true;
                    index += 2;
                    continue;
                }
                Optional<SpiceWaveform.Kind> kind = argument instanceof SpiceArgument.Group group
                        ? SpiceWaveform.Kind.of(group.name()) : Optional.empty();
                if (kind.isPresent() && waveform == null) {
                    SpiceArgument.Group group = (SpiceArgument.Group) argument;
                    List<SpiceValue> values = new ArrayList<>();
                    for (SpiceArgument value : group.arguments()) {
                        values.add(CardReader.value(value, name + "'s " + kind.get() + " value"));
                    }
                    waveform = new SpiceWaveform(kind.get(), values, group.position());
                } else if (index == 2 && direct == null && !isKeyword(argument)) {
                    direct = CardReader.value(argument, name + "'s value");
                } else {
                    rest.add(argument);
                }
                index++;
            }
            return new SpiceElement.Source(name, nodes.get(0), nodes.get(1), direct, directWord, waveform, rest,
                    card.position());
        }

        private static boolean isKeyword(SpiceArgument argument) {
            return argument instanceof SpiceArgument.Word word
                    && Set.of("ac", "dc", "distof1", "distof2").contains(word.text().toLowerCase(Locale.ROOT));
        }
    }

    /** {@code D anode cathode model [area] [OFF]}. */
    static final class DiodeParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isElement(head, "D");
        }

        @Override
        SpiceElement statement(Card card, TokenCursor cursor, ParserContext context) {
            card.requireAtLeast(3, "a diode is 'name anode cathode model'");
            List<String> words = card.words(0, 3, "a diode is 'name anode cathode model'");
            List<SpiceArgument> arguments = card.arguments();
            return new SpiceElement.Diode(card.head().value(), words.get(0), words.get(1), words.get(2),
                    arguments.subList(3, arguments.size()), card.position());
        }
    }

    /**
     * {@code Q collector base emitter [substrate] model [area]}: whether the fourth word is a substrate or the model is
     * told by the deck's model names, as SPICE tells it.
     */
    static final class BipolarParser extends CardParser {

        private final Set<String> models;

        /** @param models the deck's model names, in lower case */
        BipolarParser(Set<String> models) {
            this.models = models;
        }

        @Override
        boolean accepts(Token head) {
            return isElement(head, "Q");
        }

        @Override
        SpiceElement statement(Card card, TokenCursor cursor, ParserContext context) {
            String shape = "a bipolar transistor is 'name collector base emitter [substrate] model'";
            card.requireAtLeast(4, shape);
            List<SpiceArgument> arguments = card.arguments();
            boolean substrate = arguments.size() >= 5 && !isModel(arguments.get(3)) && isModel(arguments.get(4));
            int modelAt = substrate ? 4 : 3;
            List<String> words = card.words(0, modelAt + 1, shape);
            return new SpiceElement.Bipolar(card.head().value(), words.get(0), words.get(1), words.get(2),
                    substrate ? words.get(3) : null, words.get(modelAt), arguments.subList(modelAt + 1, arguments.size()),
                    card.position());
        }

        private boolean isModel(SpiceArgument argument) {
            return argument instanceof SpiceArgument.Word word && models.contains(word.text().toLowerCase(Locale.ROOT));
        }
    }

    /** {@code M drain gate source bulk model [L=… W=…]}. */
    static final class MosfetParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isElement(head, "M");
        }

        @Override
        SpiceElement statement(Card card, TokenCursor cursor, ParserContext context) {
            String shape = "a MOSFET is 'name drain gate source bulk model [L=… W=…]'";
            card.requireAtLeast(5, shape);
            List<String> words = card.words(0, 5, shape);
            List<SpiceArgument> arguments = card.arguments();
            return new SpiceElement.Mosfet(card.head().value(), words.get(0), words.get(1), words.get(2), words.get(3),
                    words.get(4), arguments.subList(5, arguments.size()), card.position());
        }
    }

    /** {@code X nodes… subcircuit [params:] [name=value …]}. */
    static final class InstanceParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isElement(head, "X");
        }

        @Override
        SpiceElement statement(Card card, TokenCursor cursor, ParserContext context) {
            String shape = "a subcircuit instance is 'name nodes… subcircuit [name=value …]'";
            List<String> words = new ArrayList<>();
            List<SpiceArgument> assignments = new ArrayList<>();
            for (SpiceArgument argument : card.arguments()) {
                switch (argument) {
                    case SpiceArgument.Word word when word.text().equalsIgnoreCase("params:") -> {
                    }
                    case SpiceArgument.Word word when assignments.isEmpty() -> words.add(word.text());
                    case SpiceArgument.Assignment assignment -> assignments.add(assignment);
                    default -> throw new SpiceParseException(argument.position(), shape);
                }
            }
            if (words.size() < 2) {
                throw new SpiceParseException(card.position(), card.head().value() + ": " + shape);
            }
            List<SpiceParameter> parameters = CardReader.parameters(assignments, shape);
            return new SpiceElement.Instance(card.head().value(), words.subList(0, words.size() - 1), words.getLast(),
                    parameters, card.position());
        }
    }

    /** {@code B n+ n- V=expression} or {@code I=expression}: the expression kept as written. */
    static final class BehaviouralParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isElement(head, "B");
        }

        @Override
        SpiceElement statement(Card card, TokenCursor cursor, ParserContext context) {
            String shape = "a behavioural source is 'name node node V=expression' or '… I=expression'";
            List<Token> tokens = card.tokens();
            if (tokens.size() < 5 || !SpiceTokenKind.EQUALS.matches(tokens.get(3))
                    || !SpiceTokenKind.isWord(tokens.get(2), "v") && !SpiceTokenKind.isWord(tokens.get(2), "i")) {
                throw new SpiceParseException(card.position(), card.head().value() + ": " + shape);
            }
            List<String> nodes = card.words(0, 2, shape);
            return new SpiceElement.Behavioural(card.head().value(), nodes.get(0), nodes.get(1),
                    SpiceTokenKind.isWord(tokens.get(2), "v"), card.textFrom(4), card.position());
        }
    }

    /** {@code E}, {@code F}, {@code G}, {@code H}: two output nodes and the control as written. */
    static final class ControlledParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isElement(head, "EFGH");
        }

        @Override
        SpiceElement statement(Card card, TokenCursor cursor, ParserContext context) {
            String shape = "a controlled source is 'name node node control…'";
            card.requireAtLeast(3, shape);
            List<String> nodes = card.words(0, 2, shape);
            List<SpiceArgument> arguments = card.arguments();
            return new SpiceElement.Controlled(card.head().value(), nodes.get(0), nodes.get(1),
                    arguments.subList(2, arguments.size()), card.position());
        }
    }

    /** {@code K L1 L2 … coefficient}. */
    static final class CouplingParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return isElement(head, "K");
        }

        @Override
        SpiceElement statement(Card card, TokenCursor cursor, ParserContext context) {
            String shape = "a coupling is 'name inductor inductor… coefficient'";
            card.requireAtLeast(3, shape);
            List<SpiceArgument> arguments = card.arguments();
            List<String> inductors = card.words(0, arguments.size() - 1, shape);
            return new SpiceElement.Coupling(card.head().value(), inductors,
                    CardReader.value(arguments.getLast(), card.head().value() + "'s coefficient"), card.position());
        }
    }

    /** Any element whose letter has no parser of its own: its arguments kept as written. */
    static final class GenericParser extends CardParser {
        @Override
        boolean accepts(Token head) {
            return !head.value().startsWith(".")
                    && DEDICATED.indexOf(Character.toUpperCase(head.value().charAt(0))) < 0;
        }

        @Override
        SpiceElement statement(Card card, TokenCursor cursor, ParserContext context) {
            return new SpiceElement.Generic(card.head().value(), card.arguments(), card.position());
        }
    }
}
