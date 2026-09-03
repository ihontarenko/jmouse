package org.jmouse.el;

import org.jmouse.core.context.beans.BeanLookup;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.node.expression.BeanAccessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <strong>Does {@code @bean.method(…)} reach the method somebody meant?</strong>
 *
 * <p>Two defects lived here, and both were invisible for as long as the only things calling a bean were
 * expressions written against a method the author was looking at. They surfaced the moment a
 * <em>facade</em> appeared — a whole object published for somebody else to call.</p>
 *
 * <ul>
 *   <li><strong>Overloads.</strong> The object descriptor keys methods by bare name, so a class with two
 *       {@code spawn}s kept whichever was introspected last. Which one that was, nobody could see.</li>
 *   <li><strong>Varargs.</strong> An {@code Object...} parameter is one parameter; called with three
 *       arguments it received argument zero and threw {@code argument type mismatch} — a message about
 *       types, from a call whose types were never wrong.</li>
 * </ul>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
class BeanAccessInvocationTest {

    private final ExpressionLanguage el = new ExpressionLanguage();

    private Object evaluate(String expression) {
        DefaultEvaluationContext context = new DefaultEvaluationContext();

        context.setExtensions(el.getExtensions());
        context.setBeanLookup(new Facades(Map.of("api", new Api(), "muddle", new Muddle())));

        return el.compile(expression).evaluate(context);
    }

    @Nested
    @DisplayName("overloads")
    class Overloads {

        @Test
        @DisplayName("⚠️ a call reaches the method with its own arity, not whichever was introspected last")
        void byArity() {
            assertEquals("two:a,b", evaluate("@api.at('a', 'b')"));
            assertEquals("three:a,b,c", evaluate("@api.at('a', 'b', 'c')"));
            assertEquals("none", evaluate("@api.at()"));
        }

        @Test
        @DisplayName("an arity nothing takes is refused, and the message says what is on offer")
        void noSuchArity() {
            BeanAccessException refused = assertThrows(
                    BeanAccessException.class, () -> evaluate("@api.at('a', 'b', 'c', 'd', 'e')"));

            assertTrue(refused.getMessage().contains("taking 5"), refused.getMessage());
            assertTrue(refused.getMessage().contains("methods taking"), refused.getMessage());
        }

        @Test
        @DisplayName("⚠️ two methods of one name and one arity are refused rather than guessed")
        void ambiguity() {
            // An untyped call cannot say which of these it means, and picking one is how a script calls
            // something nobody meant.
            BeanAccessException refused = assertThrows(
                    BeanAccessException.class, () -> evaluate("@muddle.same('x')"));

            assertTrue(refused.getMessage().contains("ambiguous"), refused.getMessage());
            assertTrue(refused.getMessage().contains("different names"), refused.getMessage());
        }
    }

    @Nested
    @DisplayName("⚠️ what a facade does NOT expose")
    class NotOnOffer {

        @Test
        @DisplayName("Object's own methods are not reachable through a facade")
        void objectMethods() {
            // A host publishes ITS methods by writing the class. `wait`, `notify` and `hashCode` are
            // on every object ever made and are nobody's choice — and `wait` in particular is a
            // script reaching for the host's monitor. It used to resolve, and to run.
            assertThrows(BeanAccessException.class, () -> evaluate("@api.wait()"));
            assertThrows(BeanAccessException.class, () -> evaluate("@api.notify()"));
            assertThrows(BeanAccessException.class, () -> evaluate("@api.hashCode()"));
        }
    }


    @Nested
    @DisplayName("varargs")
    class Spread {

        @Test
        @DisplayName("the trailing arguments arrive packed, however many there are")
        void packed() {
            assertEquals("all:0", evaluate("@api.all()"));
            assertEquals("all:1", evaluate("@api.all('a')"));
            assertEquals("all:3", evaluate("@api.all('a', 'b', 'c')"));
        }

        @Test
        @DisplayName("⚠️ a fixed-arity method of the same name wins over the varargs one")
        void fixedWins() {
            // `log(String)` and `log(String, Object...)` are an ordinary pair, and somebody writing one
            // argument meant the first.
            assertEquals("one", evaluate("@api.log('a')"));
            assertEquals("spread:2", evaluate("@api.log('a', 'b')"));
        }
    }

    @Nested
    @DisplayName("⚠️ a property that is not there is null, not an exception")
    class MissingProperty {

        @Test
        @DisplayName("reading a property of nothing answers nothing")
        void ofNull() {
            DefaultEvaluationContext context = new DefaultEvaluationContext();

            context.setExtensions(el.getExtensions());

            // `absent` was never put in the context, so `absent.name` is a property of nothing. It used
            // to throw an NPE naming a local variable called 'getter'.
            assertNull(el.compile("absent.name").evaluate(context));
        }

        @Test
        @DisplayName("and a map still answers, because the loop now reaches the map resolver")
        void fallsThrough() {
            DefaultEvaluationContext context = new DefaultEvaluationContext();

            context.setExtensions(el.getExtensions());
            context.setValue("row", Map.of("name", "Ada"));

            // ⚠️ The bean resolver is asked first and cannot help. It used to throw rather than answer
            // null, so the map resolver registered after it never got its turn.
            assertEquals("Ada", el.compile("row.name").evaluate(context));
        }

        @Test
        @DisplayName("a real property still resolves")
        void stillWorks() {
            DefaultEvaluationContext context = new DefaultEvaluationContext();

            context.setExtensions(el.getExtensions());
            context.setValue("thing", new Api());

            assertEquals("Ada", el.compile("thing.name").evaluate(context));
            assertNull(el.compile("thing.nothing").evaluate(context));
        }
    }

    /** A facade, with the shapes a host actually writes. */
    public static final class Api {

        public String getName() {
            return "Ada";
        }

        public String at() {
            return "none";
        }

        public String at(String first, String second) {
            return "two:" + first + "," + second;
        }

        public String at(String first, String second, String third) {
            return "three:" + first + "," + second + "," + third;
        }

        public String all(Object... everything) {
            return "all:" + everything.length;
        }

        public String log(String one) {
            return "one";
        }

        public String log(String one, Object... rest) {
            return "spread:" + (1 + rest.length);
        }
    }

    /** A facade somebody wrote carelessly. */
    public static final class Muddle {

        public String same(String value) {
            return "string";
        }

        public String same(Integer value) {
            return "integer";
        }
    }

    /** The closed lookup a facade call resolves through. */
    private record Facades(Map<String, Object> beans) implements BeanLookup {

        @Override
        public <T> T getBean(Class<T> beanClass) {
            throw new UnsupportedOperationException("by name only");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getBean(String beanName, Class<T> beanClass) {
            return (T) beans.get(beanName);
        }
    }
}
