package org.jmouse.el.grammar;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * The library's own kinds of value, each read by the engine's parsers or cut out as its author wrote it. A language adds
 * kinds of its own by implementing {@link ValueKind}; these are the ones every line-oriented language needs.
 */
public final class Values {

    private static final String ASSIGNMENT = "=";

    /** A list's item ends at its comma. */
    private static final BoundedTokenCursor.Boundary COMMA = (token, next, depth) -> depth == 0 && token.type() == BasicToken.T_COMMA;

    /** A range's low end ends at its {@code ..}. */
    private static final BoundedTokenCursor.Boundary DOTS = (token, next, depth) -> depth == 0 && token.type() == BasicToken.T_DOUBLE_DOT;

    /** The {@code x} of a size: {@code 1 x 2}. */
    private static final String BY = "x";

    /** Nothing: the clause's word says it all — {@code filled}. */
    public static final ValueKind FLAG = reading -> null;

    /** One word, hyphens and all: {@code through-hole}; one of the slot's allowed words when it names them. */
    public static final ValueKind WORD = reading -> {
        TokenCursor cursor = reading.cursor();
        if (Lines.lineEnd().matches(cursor)) {
            throw reading.refusal();
        }
        Token written = cursor.current();
        String word = Lines.word(cursor);
        if (!reading.allowed().isEmpty() && !reading.allowed().contains(word)) {
            throw GrammarRefusal.at(cursor, written, "'" + word + "' is not one of " + reading.allowed().stream().sorted().toList());
        }
        return new WordNode(word, false);
    };

    /** A name, an index written against it and all: {@code R1}, {@code q[0..7]}, {@code memory[256]}. */
    public static final ValueKind NAME = Values::name;

    /** A text in quotes. */
    public static final ValueKind TEXT = new ValueKind() {
        @Override
        public boolean standsAt(TokenCursor cursor, Set<String> words, Set<String> allowed) {
            return cursor.isCurrent(BasicToken.T_STRING);
        }

        @Override
        public Expression read(ValueReading reading) {
            return reading.quoted();
        }

        /** In quotes, or bare where a term writes it as its name: {@code pad:1(…)}. */
        @Override
        public Expression readTerm(ValueReading reading) {
            if (reading.cursor().isCurrent(BasicToken.T_STRING)) {
                return reading.quoted();
            }
            return new WordNode(Lines.word(reading.cursor()), true);
        }
    };

    /** A number with at most its unit. */
    public static final ValueKind QUANTITY = reading -> QuantityValueParser.read(reading.bounded(), reading.context());

    /** {@code x, y}; a term writes {@code x y}, a comma being its arguments'. */
    public static final ValueKind POINT = new ValueKind() {
        @Override
        public Expression read(ValueReading reading) {
            return PointParser.read(reading.bounded(), reading.context());
        }

        @Override
        public Expression readTerm(ValueReading reading) {
            // The first coordinate ends where the second begins: a number, or a minus before one, after the first's own.
            Token first = reading.cursor().current();
            int own = first.type() == BasicToken.T_MINUS ? first.ordinal() + 1 : first.ordinal();
            QuantityLiteralNode x = QuantityValueParser.read(reading.bounded((token, next, depth) -> token.ordinal() > own
                    && (Lines.number(token) || token.type() == BasicToken.T_MINUS && Lines.number(next))), reading.context());
            QuantityLiteralNode y = QuantityValueParser.read(reading.bounded(), reading.context());
            return new PointNode(x, y);
        }
    };

    /** {@code width x height}. */
    public static final ValueKind EXTENT = reading -> ExtentParser.read(reading.bounded(), reading.context());

    /** {@code width x height}, or one number for both: a drill. */
    public static final ValueKind EXTENT_OR_QUANTITY = reading -> holdsBy(reading.bounded())
            ? ExtentParser.read(reading.bounded(), reading.context())
            : QuantityValueParser.read(reading.bounded(), reading.context());

    /** Words between commas: {@code vcc, 0}. */
    public static final ValueKind NAMES = new ValueKind() {
        @Override
        public Expression read(ValueReading reading) {
            return names(reading);
        }

        /** {@code [a, b]}, or one name alone. */
        @Override
        public Expression readTerm(ValueReading reading) {
            return listed(reading, item -> new WordNode(Lines.word(item.cursor()), false));
        }
    };

    private static ListNode names(ValueReading reading) {
        TokenCursor cursor = reading.cursor();
        List<Expression> names = new ArrayList<>();
        while (reading.bounded().hasNext()) {
            if (cursor.isCurrent(BasicToken.T_COMMA)) {
                cursor.next();
                continue;
            }
            names.add(new WordNode(Lines.word(cursor), false));
            if (reading.bounded().hasNext() && !cursor.isCurrent(BasicToken.T_COMMA)) {
                throw GrammarRefusal.here(cursor, "names stand between commas: '" + cursor.current().value() + "' follows one without");
            }
        }
        if (names.isEmpty()) {
            throw reading.refusal();
        }
        return new ListNode(names);
    }

    /** Names between commas, each with the words that mark it: {@code clock edge, cs bubble, data}. */
    public static final ValueKind ITEMS = new ValueKind() {
        @Override
        public Expression read(ValueReading reading) {
            return groups(reading, cursor -> true, true);
        }

        /** {@code [clock edge, cs bubble]}, or one item alone: {@code data}. */
        @Override
        public Expression readTerm(ValueReading reading) {
            return listed(reading, item -> group(item, cursor -> true));
        }
    };

    /** Numbers between commas: {@code 0.25, 0.4}, each with at most its unit. */
    public static final ValueKind QUANTITIES = new ValueKind() {
        @Override
        public Expression read(ValueReading reading) {
            List<Expression> quantities = new ArrayList<>();
            quantities.add(QuantityValueParser.read(reading.bounded(COMMA), reading.context()));
            while (reading.cursor().isCurrent(BasicToken.T_COMMA)) {
                reading.cursor().next();
                quantities.add(QuantityValueParser.read(reading.bounded(COMMA), reading.context()));
            }
            return new ListNode(quantities);
        }

        @Override
        public Expression readTerm(ValueReading reading) {
            return listed(reading, item -> QuantityValueParser.read(item.bounded(), item.context()));
        }
    };

    /** Numbers in groups — a comma between groups, a space inside one —: {@code 1 7}, {@code 1120 320, 1120 480}. */
    public static final ValueKind NUMBERS = new ValueKind() {
        @Override
        public boolean standsAt(TokenCursor cursor, Set<String> words, Set<String> allowed) {
            return Lines.signedNumber(cursor);
        }

        @Override
        public Expression read(ValueReading reading) {
            return groups(reading, Lines::signedNumber, false);
        }

        @Override
        public boolean standsAtTerm(TokenCursor cursor, Set<String> words, Set<String> allowed) {
            return Lines.signedNumber(cursor) || cursor.isCurrent(BasicToken.T_OPEN_BRACKET);
        }

        /** {@code 1 7} one group, {@code [15, 1, 2]} three, {@code [1120 320, 1120 480]} two. */
        @Override
        public Expression readTerm(ValueReading reading) {
            return listed(reading, item -> group(item, Lines::signedNumber));
        }
    };

    /** An expression, kept as its author wrote it, for jMF to read. */
    public static final ValueKind FORMULA = ValueReading::formula;

    /** {@code +- amount}: how far a value may stand from its mark — {@code ± 1%}, the sign kept as written. */
    public static final ValueKind TOLERANCE = new ValueKind() {
        @Override
        public boolean standsAt(TokenCursor cursor, Set<String> words, Set<String> allowed) {
            return LineBoundaries.plusMinus(cursor.current(), cursor.lookAt(1));
        }

        @Override
        public Expression read(ValueReading reading) {
            TokenCursor cursor = reading.cursor();
            String mark = AuthoredText.of(cursor).written(cursor.current(), cursor.lookAt(1));
            cursor.shift(2);
            return new ToleranceNode(mark, reading.formula());
        }
    };

    /** {@code = expression} or {@code = "text"}: what a name is given. */
    public static final ValueKind ASSIGNED = new ValueKind() {
        @Override
        public boolean standsAt(TokenCursor cursor, Set<String> words, Set<String> allowed) {
            return cursor.current().value().equals(ASSIGNMENT);
        }

        /**
         * A text, or one expression — or several between commas, each read on its own, as a value of two coordinates is
         * written: {@code move = 0, between(0, 14, …)}. A comma inside a call's parentheses separates its arguments, not
         * the values.
         */
        @Override
        public Expression read(ValueReading reading) {
            TokenCursor cursor = reading.cursor();
            cursor.next();
            if (cursor.isCurrent(BasicToken.T_STRING)) {
                return new AssignedNode(reading.quoted());
            }
            List<Expression> values = new ArrayList<>();
            values.add(reading.formula(reading.bounded(COMMA)));
            while (cursor.isCurrent(BasicToken.T_COMMA)) {
                cursor.next();
                values.add(reading.formula(reading.bounded(COMMA)));
            }
            return new AssignedNode(values.size() == 1 ? values.getFirst() : new ListNode(values));
        }

        @Override
        public boolean standsAtTerm(TokenCursor cursor, Set<String> words, Set<String> allowed) {
            return true;
        }

        /** The value alone: a term's argument is what the name is given. */
        @Override
        public Expression readTerm(ValueReading reading) {
            return new AssignedNode(reading.cursor().isCurrent(BasicToken.T_STRING) ? reading.quoted() : reading.formula());
        }
    };

    /** {@code low .. high}, each an expression, or a list in brackets: {@code [60, 61]}. */
    public static final ValueKind RANGE = reading -> {
        TokenCursor cursor = reading.cursor();
        if (cursor.isCurrent(BasicToken.T_OPEN_BRACKET)) {
            return RangeNode.listing(reading.formula());
        }
        FormulaNode low = reading.formula(reading.bounded(DOTS));
        if (!cursor.isCurrent(BasicToken.T_DOUBLE_DOT)) {
            throw GrammarRefusal.here(cursor, "a range is 'low .. high', or a list in brackets");
        }
        cursor.next();
        return RangeNode.between(low, reading.formula());
    };

    /** What a part is connected to: {@code (in, out)}, or {@code (positive = vcc, negative = 0)}. */
    public static final ValueKind CONNECTIONS = new ValueKind() {
        @Override
        public boolean standsAt(TokenCursor cursor, Set<String> words, Set<String> allowed) {
            return cursor.isCurrent(BasicToken.T_OPEN_PAREN);
        }

        @Override
        public Expression read(ValueReading reading) {
            return connections(reading.cursor());
        }

        @Override
        public boolean standsAtTerm(TokenCursor cursor, Set<String> words, Set<String> allowed) {
            return cursor.isCurrent(BasicToken.T_OPEN_BRACKET);
        }

        /** {@code [in, out]}, or {@code [positive: vcc, negative: 0]}. */
        @Override
        public Expression readTerm(ValueReading reading) {
            TokenCursor cursor = reading.cursor();
            Token open = cursor.next();
            List<ConnectionsNode.Connection> connections = new ArrayList<>();
            while (!cursor.isCurrent(BasicToken.T_CLOSE_BRACKET)) {
                if (Lines.lineEnd().matches(cursor)) {
                    throw GrammarRefusal.at(cursor, open, "what a part is connected to closes with ']'");
                }
                if (cursor.isCurrent(BasicToken.T_COMMA)) {
                    cursor.next();
                    continue;
                }
                String first = Lines.word(cursor);
                if (cursor.isCurrent(BasicToken.T_COLON)) {
                    cursor.next();
                    connections.add(new ConnectionsNode.Connection(first, Lines.word(cursor)));
                } else {
                    connections.add(new ConnectionsNode.Connection(null, first));
                }
            }
            cursor.next();
            return new ConnectionsNode(connections);
        }
    };

    /**
     * One named value's value, as a list of settings reads it: words in quotes, or a formula up to the next comma —
     * {@code A}, {@code "8 0"}. What a program reads a setting given on its own with, so it cannot carry a second one.
     */
    public static final ValueKind SETTING = reading -> reading.cursor().isCurrent(BasicToken.T_STRING) ? reading.quoted()
            : reading.formula(reading.bounded(COMMA));

    /** Names given values between commas — {@code amplitude = A, bytes = "8 0"} —, or named alone: {@code closed}. */
    public static final ValueKind SETTINGS = new ValueKind() {
        @Override
        public Expression read(ValueReading reading) {
            return settings(reading);
        }

        /** {@code [flipped: 1, inverted: 1]}: a list of named values, a name alone a flag. */
        @Override
        public Expression readTerm(ValueReading reading) {
            TokenCursor cursor = reading.cursor();
            if (!cursor.isCurrent(BasicToken.T_OPEN_BRACKET)) {
                throw reading.refusal();
            }
            Token open = cursor.next();
            List<SettingsNode.Setting> settings = new ArrayList<>();
            while (!cursor.isCurrent(BasicToken.T_CLOSE_BRACKET)) {
                if (Lines.lineEnd().matches(cursor)) {
                    throw GrammarRefusal.at(cursor, open, "settings close with ']'");
                }
                if (cursor.isCurrent(BasicToken.T_COMMA)) {
                    cursor.next();
                    continue;
                }
                String name = Lines.word(cursor);
                Expression value = null;
                if (cursor.isCurrent(BasicToken.T_COLON)) {
                    cursor.next();
                    ValueReading item = new ValueReading(cursor, new BoundedTokenCursor(cursor, ITEM), reading.context(),
                            reading.stops(), Set.of(), reading.at(), reading.line());
                    value = cursor.isCurrent(BasicToken.T_STRING) ? item.quoted() : item.formula();
                }
                settings.add(new SettingsNode.Setting(name, value));
            }
            cursor.next();
            return new SettingsNode(settings);
        }
    };

    /** The rest of the value, kept as written, for a grammar of its own another reads. */
    public static final ValueKind WRITTEN = reading -> {
        TokenCursor cursor = reading.cursor();
        Token first = cursor.current();
        Token last = first;
        while (reading.bounded().hasNext()) {
            last = cursor.next();
        }
        return new WrittenLineNode(AuthoredText.of(cursor).written(first, last));
    };

    /** A list's item ends at its comma or at the bracket that closes the list. */
    private static final BoundedTokenCursor.Boundary ITEM = (token, next, depth) -> depth == 0
            && (token.type() == BasicToken.T_COMMA || token.type() == BasicToken.T_CLOSE_BRACKET);

    private Values() {
    }

    /**
     * A list as a term writes it: {@code [one, two]} between brackets, or one item alone; each item read by {@code item}
     * within its own bound.
     */
    private static ListNode listed(ValueReading reading, Function<ValueReading, Expression> item) {
        TokenCursor cursor = reading.cursor();
        if (!cursor.isCurrent(BasicToken.T_OPEN_BRACKET)) {
            return new ListNode(List.of(item.apply(reading)));
        }
        Token open = cursor.next();
        List<Expression> items = new ArrayList<>();
        while (!cursor.isCurrent(BasicToken.T_CLOSE_BRACKET)) {
            if (Lines.lineEnd().matches(cursor)) {
                throw GrammarRefusal.at(cursor, open, "a list closes with ']'");
            }
            if (cursor.isCurrent(BasicToken.T_COMMA)) {
                cursor.next();
                continue;
            }
            items.add(item.apply(new ValueReading(cursor, new BoundedTokenCursor(cursor, ITEM), reading.context(),
                    reading.stops(), reading.allowed(), reading.at(), reading.line())));
        }
        cursor.next();
        return new ListNode(items);
    }

    /** One group of words a space apart, to where its bound ends: {@code 1 7}, {@code clock edge}. */
    private static ListNode group(ValueReading reading, Function<TokenCursor, Boolean> belongs) {
        List<Expression> words = new ArrayList<>();
        while (reading.bounded().hasNext() && belongs.apply(reading.cursor())) {
            words.add(new WordNode(Lines.word(reading.cursor()), false));
        }
        if (words.isEmpty()) {
            throw reading.refusal();
        }
        return new ListNode(words, false);
    }

    private static NameNode name(ValueReading reading) {
        TokenCursor cursor = reading.cursor();
        Token first = cursor.current();
        SourceReading.touchingWord(cursor, Lines.WORD_SEPARATORS);
        Token last = cursor.lookAt(-1);
        AuthoredText authored = AuthoredText.of(cursor);
        String base = authored.written(first, last);
        if (!cursor.isCurrent(BasicToken.T_OPEN_BRACKET) || !SourceReading.touches(cursor, last, cursor.current())) {
            return new NameNode(base, base, null, null);
        }
        Token open = cursor.next();
        int low = index(cursor, open);
        Integer high = null;
        if (cursor.isCurrent(BasicToken.T_DOUBLE_DOT)) {
            cursor.next();
            high = index(cursor, open);
        }
        if (!cursor.isCurrent(BasicToken.T_CLOSE_BRACKET)) {
            throw GrammarRefusal.at(cursor, open, "an index is '[count]' or '[first..last]': 'memory[256]', 'q[0..7]'");
        }
        Token close = cursor.next();
        if (high != null && high < low) {
            throw GrammarRefusal.at(cursor, open, "an index runs from its first to a later last: '[0..7]'");
        }
        return new NameNode(authored.written(first, close), base, low, high);
    }

    /** A whole number inside an index's brackets. */
    private static int index(TokenCursor cursor, Token open) {
        Token token = cursor.current();
        if (!Lines.number(token) || !token.value().chars().allMatch(Character::isDigit)) {
            throw GrammarRefusal.at(cursor, open, "an index is '[count]' or '[first..last]', each a whole number");
        }
        cursor.next();
        return Integer.parseInt(token.value());
    }

    /**
     * Groups of words between commas, the words of a group between spaces, each word one that {@code belongs} lets in.
     *
     * @param required whether an empty last group is refused
     */
    private static ListNode groups(ValueReading reading, Function<TokenCursor, Boolean> belongs, boolean required) {
        TokenCursor cursor = reading.cursor();
        List<Expression> groups = new ArrayList<>();
        List<Expression> group = new ArrayList<>();
        while (reading.bounded().hasNext() && (belongs.apply(cursor) || cursor.isCurrent(BasicToken.T_COMMA))) {
            if (cursor.isCurrent(BasicToken.T_COMMA)) {
                groups.add(new ListNode(group, false));
                group = new ArrayList<>();
                cursor.next();
                continue;
            }
            group.add(new WordNode(Lines.word(cursor), false));
        }
        if (required && group.isEmpty()) {
            throw reading.refusal();
        }
        groups.add(new ListNode(group, false));
        return new ListNode(groups);
    }

    private static ConnectionsNode connections(TokenCursor cursor) {
        Token open = cursor.next();
        List<ConnectionsNode.Connection> connections = new ArrayList<>();
        while (!cursor.isCurrent(BasicToken.T_CLOSE_PAREN)) {
            if (Lines.lineEnd().matches(cursor)) {
                throw GrammarRefusal.at(cursor, open, "what a part is connected to closes with ')'");
            }
            if (cursor.isCurrent(BasicToken.T_COMMA)) {
                cursor.next();
                continue;
            }
            String first = Lines.word(cursor);
            if (cursor.isCurrent(BasicToken.T_EQ) && cursor.current().value().equals(ASSIGNMENT)) {
                cursor.next();
                connections.add(new ConnectionsNode.Connection(first, Lines.word(cursor)));
            } else {
                connections.add(new ConnectionsNode.Connection(null, first));
            }
        }
        cursor.next();
        return new ConnectionsNode(connections);
    }

    private static SettingsNode settings(ValueReading reading) {
        TokenCursor cursor = reading.cursor();
        List<SettingsNode.Setting> settings = new ArrayList<>();
        while (reading.bounded().hasNext()) {
            if (cursor.isCurrent(BasicToken.T_COMMA)) {
                cursor.next();
                continue;
            }
            String name = Lines.word(cursor);
            Expression value = null;
            if (cursor.current().value().equals(ASSIGNMENT)) {
                cursor.next();
                value = SETTING.read(reading);
            }
            settings.add(new SettingsNode.Setting(name, value));
        }
        return new SettingsNode(settings);
    }

    /** Whether a value ahead is a size — {@code w x h} —: an {@code x} stands in it, outside parentheses. */
    private static boolean holdsBy(TokenCursor bounded) {
        int depth = 0;
        for (int offset = 0; offset < bounded.remaining() && !bounded.lookAt(offset).type().equals(BasicToken.T_EOL); offset++) {
            Token token = bounded.lookAt(offset);
            if (token.type() == BasicToken.T_OPEN_PAREN) {
                depth++;
            } else if (token.type() == BasicToken.T_CLOSE_PAREN) {
                depth--;
            } else if (depth == 0 && token.type() != BasicToken.T_STRING && token.value().equals(BY)) {
                return true;
            }
        }
        return false;
    }
}
