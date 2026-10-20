package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor.Boundary;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block.Criterion;
import org.jmouse.parallax.script.Block.CriterionKind;

import java.util.Optional;

/** {@code near 6.86 kHz +- 5%}, {@code within 26 V .. 30 V}, {@code is 1}: what an expectation holds its measure to. */
final class CriterionLineParser extends JmxLineParser<Criterion> {

    /** The typical value of {@code near} ends where its tolerance's {@code +-} begins. */
    private static final Boundary TOLERANCE = (token, after, depth) -> token.value().equals("+") && after.value().equals("-");

    private final Reading reading;
    private final JmxValues values;

    CriterionLineParser(Reading reading, JmxValues values) {
        this.reading = reading;
        this.values = values;
    }

    @Override
    Criterion read() {
        TokenCursor cursor = reading.cursor;
        TokenCursor line = reading.lineView();
        Token first = cursor.current();
        String kind = line.valueAt(0);
        switch (kind) {
            case "near" -> {
                int plus = -1;
                for (int offset = 1; offset + 1 < line.remaining(); offset++) {
                    if (line.checkValueAt(offset, "+") && line.checkValueAt(offset + 1, "-")) {
                        plus = offset;
                        break;
                    }
                }
                if (plus < 0) {
                    throw reading.refusal(first, "'near' is 'near typical +- tolerance'");
                }
                boolean relative = line.checkValueAt(line.remaining() - 1, "%");
                cursor.next();
                JmxValues.Written typical = values.written(TOLERANCE);
                cursor.shift(2);
                JmxValues.Written tolerance = values.written(JmxValues.LINE);
                return end(new Criterion(CriterionKind.NEAR, Optional.of(typical.amount()), Optional.of(tolerance.amount()),
                        relative, Optional.empty(), Optional.empty(), null));
            }
            case "within" -> {
                int dots = line.offsetOf(1, "..");
                if (dots < 0) {
                    throw reading.refusal(first, "'within' is 'within minimum .. maximum'");
                }
                cursor.next();
                Optional<JmxValues.Written> minimum = dots == 1 ? Optional.empty() : Optional.of(values.written(JmxValues.LINE));
                cursor.ensure(BasicToken.T_DOUBLE_DOT);
                Optional<JmxValues.Written> maximum = reading.atLineEnd() ? Optional.empty()
                        : Optional.of(values.written(JmxValues.LINE));
                return end(new Criterion(CriterionKind.WITHIN, Optional.empty(), Optional.empty(), false,
                        minimum.map(JmxValues.Written::amount), maximum.map(JmxValues.Written::amount), null));
            }
            case "is" -> {
                StringBuilder words = new StringBuilder();
                cursor.next();
                while (!reading.atLineEnd()) {
                    Token token = cursor.next();
                    words.append(reading.isString(token) ? reading.literal(token) : token.value());
                }
                return new Criterion(CriterionKind.IS, Optional.empty(), Optional.empty(), false, Optional.empty(),
                        Optional.empty(), words.toString());
            }
            default -> throw reading.refusal(first, "'" + kind + "' is not a criterion: near, within or is");
        }
    }

    private Criterion end(Criterion criterion) {
        reading.requireLineEnd();
        return criterion;
    }
}
