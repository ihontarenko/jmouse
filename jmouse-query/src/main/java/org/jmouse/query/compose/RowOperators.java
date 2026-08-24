package org.jmouse.query.compose;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.ComparisonOperator;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.ArgumentsNode;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.el.node.expression.TestNode;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Every comparison a builder row can express.
 *
 * <h2>⚠️ Deliberately small, and deliberately not "every operator the language has"</h2>
 *
 * <p>jMQ can say a great deal more than this — ranges, membership, ternaries, null-safe fallbacks. What
 * belongs here is only what a <strong>row of three controls</strong> can honestly draw. Everything else
 * is written as text, and the panel says so rather than drawing an approximation.</p>
 *
 * <p>Growing this list is how a builder becomes a worse text editor. Adding to it should always answer
 * the question <em>can an attribute picker, an operator picker and one value box say this?</em></p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum RowOperators implements RowOperator {

    CONTAINS("contains", true, false, false) {
        @Override
        public Expression write(Expression left, Expression value) {
            return test("contains", false, left, value);
        }

        @Override
        public Optional<Reading> read(Expression node) {
            return readTest(node, "contains", false);
        }
    },

    NOT_CONTAINS("notContains", true, false, true) {
        @Override
        public Expression write(Expression left, Expression value) {
            return test("contains", true, left, value);
        }

        @Override
        public Optional<Reading> read(Expression node) {
            return readTest(node, "contains", true);
        }
    },

    STARTS("starts", true, false, false) {
        @Override
        public Expression write(Expression left, Expression value) {
            return test("starts", false, left, value);
        }

        @Override
        public Optional<Reading> read(Expression node) {
            return readTest(node, "starts", false);
        }
    },

    ENDS("ends", true, false, false) {
        @Override
        public Expression write(Expression left, Expression value) {
            return test("ends", false, left, value);
        }

        @Override
        public Optional<Reading> read(Expression node) {
            return readTest(node, "ends", false);
        }
    },

    EQUALS("equals", true, false, false) {
        @Override
        public Expression write(Expression left, Expression value) {
            return new BinaryOperation(left, ComparisonOperator.EQUAL, value);
        }

        @Override
        public Optional<Reading> read(Expression node) {
            return readComparison(node, ComparisonOperator.EQUAL);
        }
    },

    NOT_EQUALS("notEquals", true, false, true) {
        @Override
        public Expression write(Expression left, Expression value) {
            return new BinaryOperation(left, ComparisonOperator.NOT_EQUAL, value);
        }

        @Override
        public Optional<Reading> read(Expression node) {
            return readComparison(node, ComparisonOperator.NOT_EQUAL);
        }
    },

    GREATER("greater", true, true, false) {
        @Override
        public Expression write(Expression left, Expression value) {
            return new BinaryOperation(left, ComparisonOperator.GT, value);
        }

        @Override
        public Optional<Reading> read(Expression node) {
            return readComparison(node, ComparisonOperator.GT);
        }
    },

    GREATER_OR_EQUAL("greaterOrEqual", true, true, false) {
        @Override
        public Expression write(Expression left, Expression value) {
            return new BinaryOperation(left, ComparisonOperator.GTE, value);
        }

        @Override
        public Optional<Reading> read(Expression node) {
            return readComparison(node, ComparisonOperator.GTE);
        }
    },

    LESS("less", true, true, false) {
        @Override
        public Expression write(Expression left, Expression value) {
            return new BinaryOperation(left, ComparisonOperator.LT, value);
        }

        @Override
        public Optional<Reading> read(Expression node) {
            return readComparison(node, ComparisonOperator.LT);
        }
    },

    LESS_OR_EQUAL("lessOrEqual", true, true, false) {
        @Override
        public Expression write(Expression left, Expression value) {
            return new BinaryOperation(left, ComparisonOperator.LTE, value);
        }

        @Override
        public Optional<Reading> read(Expression node) {
            return readComparison(node, ComparisonOperator.LTE);
        }
    },

    /**
     * ⚠️ Written as {@code is null}, which in this language means <em>has no value</em> and covers the
     * attribute being absent entirely. That is why it is not itself offered the absence question.
     */
    EMPTY("empty", false, false, false) {
        @Override
        public Expression write(Expression left, Expression value) {
            return test("null", false, left, null);
        }

        @Override
        public Optional<Reading> read(Expression node) {
            return readTest(node, "null", false);
        }
    },

    NOT_EMPTY("notEmpty", false, false, false) {
        @Override
        public Expression write(Expression left, Expression value) {
            return test("null", true, left, null);
        }

        @Override
        public Optional<Reading> read(Expression node) {
            return readTest(node, "null", true);
        }
    };

    private static final Map<String, RowOperators> BY_SPELLING = Stream.of(values())
            .collect(Collectors.toMap(operator -> operator.spelling, Function.identity()));

    private final String  spelling;
    private final boolean needsValue;
    private final boolean ordered;
    private final boolean negative;

    RowOperators(String spelling, boolean needsValue, boolean ordered, boolean negative) {
        this.spelling = spelling;
        this.needsValue = needsValue;
        this.ordered = ordered;
        this.negative = negative;
    }

    /**
     * The operator a row names.
     *
     * @param spelling as a row spells it
     * @return the operator
     * @throws ComposeException naming every operator that would have worked
     */
    public static RowOperators spelled(String spelling) {
        RowOperators found = BY_SPELLING.get(spelling);

        if (found == null) {
            throw new ComposeException("'%s' is not a comparison a row can express. Try one of: %s"
                    .formatted(spelling, String.join(", ", BY_SPELLING.keySet())));
        }

        return found;
    }

    /** Every operator, in the order a screen should offer them. */
    public static RowOperators[] all() {
        return values();
    }

    @Override
    public String spelling() {
        return spelling;
    }

    @Override
    public boolean needsValue() {
        return needsValue;
    }

    @Override
    public boolean ordered() {
        return ordered;
    }

    @Override
    public boolean negative() {
        return negative;
    }

    // ── Building and recognising, side by side ──────────────────────────────────

    private static Expression test(String testName, boolean negated, Expression left, Expression value) {
        TestNode written = new TestNode(testName);

        written.setLeft(left);
        written.setNegated(negated);

        if (value != null) {
            ArgumentsNode arguments = new ArgumentsNode();

            arguments.add(value);
            written.setArguments(arguments);
        }

        return written;
    }

    private static Optional<Reading> readTest(Expression node, String testName, boolean negated) {
        if (!(node instanceof TestNode test)
            || !testName.equals(test.getName())
            || test.isNegated() != negated) {
            return Optional.empty();
        }

        Expression arguments = test.getArguments();

        if (arguments == null) {
            return Optional.of(new Reading(test.getLeft(), null));
        }

        // ⚠️ Exactly one argument, or this is not a row. `is contains('a', 'b')` is a perfectly good
        // query and a builder row cannot say it — so it goes back as text rather than losing an argument.
        if (arguments.getChildren().size() != 1
            || !(arguments.getChildren().getFirst() instanceof Expression only)) {
            return Optional.empty();
        }

        return Optional.of(new Reading(test.getLeft(), only));
    }

    private static Optional<Reading> readComparison(Expression node, Operator wanted) {
        if (!(node instanceof BinaryOperation comparison) || comparison.getOperator() != wanted) {
            return Optional.empty();
        }

        return Optional.of(new Reading(comparison.getLeft(), comparison.getRight()));
    }
}
