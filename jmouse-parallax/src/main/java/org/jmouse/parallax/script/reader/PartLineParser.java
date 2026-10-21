package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor.Boundary;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block.Part;
import org.jmouse.parallax.script.SourceText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * {@code R1 resistor (in, out) resistance = R}, {@code U1 lm358@3 (A.non_inverting_input = ref, A.output = out,
 * Vcc = vcc, GND = 0)} — a unit's pin named with its unit —, {@code SW1 switch (a, b) closed},
 * {@code R2 resistor (a, b) resistance = R symbol resistor-iec} — drawn with another of the component's symbols —,
 * {@code U4 ne5532 (…) footprint dip-8-footprint} — sitting on another package than its component's.
 */
final class PartLineParser extends JmxLineParser<Part> {

    private static final String SHAPE = "a part is 'ID component (connections) [settings] [place x, y]'";

    /** The clauses that close a part line, each once, in either order: {@code symbol r2 footprint dip-8-footprint}. */
    private static final Set<String> CLOSING_WORDS = Set.of("symbol", "footprint");

    /**
     * Where a setting's value ends: a comma, the next {@code name =} — settings need no comma between them —, or a
     * closing clause: the {@code symbol} the part is drawn with, the {@code footprint} it sits on.
     */
    private static final Boundary SETTING = (token, after, depth) -> depth == 0
            && (token.type() == BasicToken.T_COMMA
                || (token.type() == BasicToken.T_IDENTIFIER && after.value().equals("="))
                || (token.type() != BasicToken.T_STRING && CLOSING_WORDS.contains(token.value())));

    private final Reading reading;
    private final JmxValues values;

    PartLineParser(Reading reading, JmxValues values) {
        this.reading = reading;
        this.values = values;
    }

    @Override
    Part read() {
        TokenCursor cursor = reading.cursor;
        TokenCursor line = reading.lineView();
        Token first = cursor.current();
        if (line.remaining() < 2) {
            throw reading.refusal(first, SHAPE);
        }
        String id = cursor.next().value();
        String reference = reading.joinedWord();
        if (cursor.checkValueAt(0, "unit") && !reading.atLineEnd()) {
            // It was read and then dropped, which left every pin it named unconnected and the part silently dead.
            throw reading.refusal(cursor.current(), "name a unit's pins with the unit: "
                    + "'(A.output = out, A.inverting_input = out, Vcc = vcc)'");
        }
        List<String> positional = new ArrayList<>();
        Map<String, String> named = new LinkedHashMap<>();
        if (!reading.atLineEnd() && cursor.isCurrent(BasicToken.T_OPEN_PAREN)) {
            connections(positional, named);
        }
        TokenCursor rest = reading.lineView();
        int placeAt = rest.offsetOf(0, "place");
        if (placeAt >= 0) {
            throw reading.refusalAhead(placeAt, "a part is placed in the 'layout' block: 'layout { " + id + " at 240, 200 }'");
        }
        Map<String, SourceText> settings = settings();
        Map<String, String> closing = new LinkedHashMap<>();
        while (!reading.atLineEnd()) {
            Token word = cursor.next();
            if (!CLOSING_WORDS.contains(word.value())) {
                throw reading.refusal(word, "a part line ends with 'symbol resistor-iec' and 'footprint dip-8-footprint'");
            }
            if (reading.atLineEnd() || CLOSING_WORDS.contains(cursor.current().value())) {
                throw reading.refusal(word, "'" + word.value() + "' names one: '" + word.value() + " "
                        + ("symbol".equals(word.value()) ? "resistor-iec" : "dip-8-footprint") + "'");
            }
            if (closing.putIfAbsent(word.value(), reading.joinedWord()) != null) {
                throw reading.refusal(word, "'" + word.value() + "' is written twice on one part");
            }
        }
        return new Part(id, reference, Optional.empty(), positional, named, settings,
                Optional.ofNullable(closing.get("symbol")), Optional.ofNullable(closing.get("footprint")),
                reading.lineOf(first));
    }

    /** {@code (a, b)} by pin order, or {@code (pin = net, …)} by name — never both. */
    private void connections(List<String> positional, Map<String, String> named) {
        TokenCursor cursor = reading.cursor;
        Token open = cursor.next();
        List<List<Token>> spans = new ArrayList<>();
        List<Token> span = new ArrayList<>();
        int depth = 0;
        while (true) {
            if (reading.atLineEnd()) {
                throw reading.refusal(open, "'(' is never closed on this line");
            }
            Token token = cursor.next();
            if (token.type() == BasicToken.T_OPEN_PAREN) {
                depth++;
            } else if (token.type() == BasicToken.T_CLOSE_PAREN) {
                if (depth == 0) {
                    break;
                }
                depth--;
            } else if (depth == 0 && token.type() == BasicToken.T_COMMA) {
                spans.add(span);
                span = new ArrayList<>();
                continue;
            }
            span.add(token);
        }
        spans.add(span);
        for (List<Token> written : spans) {
            if (written.isEmpty()) {
                continue;
            }
            int equals = -1;
            for (int at = 0; at < written.size(); at++) {
                if (written.get(at).value().equals("=")) {
                    equals = at;
                    break;
                }
            }
            if (equals < 0) {
                positional.add(words(written, 0, written.size()));
            } else {
                named.put(words(written, 0, equals), words(written, equals + 1, written.size()));
            }
        }
        if (!positional.isEmpty() && !named.isEmpty()) {
            throw reading.refusal(open, "connect a part either by pin order '(a, b)' or by name '(pin = net)', not both");
        }
    }

    /** The words of some tokens written together, texts in quotes without their quotes: a pin's or a net's name. */
    private String words(List<Token> tokens, int from, int to) {
        StringBuilder words = new StringBuilder();
        for (Token token : tokens.subList(from, to)) {
            words.append(reading.isString(token) ? reading.literal(token) : token.value());
        }
        return words.toString();
    }

    /**
     * {@code resistance = R, waveform = step} or bare words such as {@code closed}: each setting as written; a value is
     * an expression, or a text in quotes. Commas between settings are optional.
     */
    private Map<String, SourceText> settings() {
        TokenCursor cursor = reading.cursor;
        Map<String, SourceText> settings = new LinkedHashMap<>();
        while (!reading.atLineEnd() && !CLOSING_WORDS.contains(cursor.current().value())) {
            if (cursor.isCurrent(BasicToken.T_COMMA)) {
                cursor.next();
                continue;
            }
            Token name = cursor.next();
            if (cursor.checkValueAt(0, "=") && !reading.atLineEnd()) {
                cursor.next();
                settings.put(name.value(), value());
            } else {
                settings.put(name.value(), reading.text(name.offset(), name.offset()));
            }
        }
        return settings;
    }

    /** A setting's value: a text in quotes as written, or an expression to where the next setting begins. */
    private SourceText value() {
        TokenCursor cursor = reading.cursor;
        if (cursor.isCurrent(BasicToken.T_STRING)) {
            Token text = cursor.next();
            return reading.text(text.offset(), cursor.endOfRead());
        }
        return values.expression(SETTING);
    }
}
