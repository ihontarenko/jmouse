package org.jmouse.el.node.expression;

import org.jmouse.core.access.descriptor.Describer;
import org.jmouse.core.access.descriptor.FieldDescriptor;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.context.beans.BeanLookupContext;
import org.jmouse.core.invoke.*;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.literal.NullLiteralNode;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BeanAccessNode extends AbstractExpression {

    private static final InvocationMethodContext EMPTY_CONTEXT = new InvocationMethodContext();

    /**
     * Methods already resolved, by class, name and arity.
     *
     * <p>⚠️ {@code Class.getMethods()} allocates an array of every public method on every call, and this
     * sits on the path a script takes for each facade call — so the answer is remembered. Keyed by
     * arity as well as name, because that is the whole point of the lookup.</p>
     */
    private static final Map<MethodKey, Method> METHODS = new ConcurrentHashMap<>();

    private AccessType type;
    private String     bean;
    private BeanAction action;

    public String getBean() {
        return bean;
    }

    public void setBean(String bean) {
        this.bean = bean;
    }

    public AccessType getType() {
        return type;
    }

    public void setType(AccessType type) {
        this.type = type;
    }

    public BeanAction getAction() {
        return action;
    }

    public void setAction(BeanAction action) {
        this.action = action;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        Object bean      = null;
        Object evaluated = null;

        if (context instanceof BeanLookupContext lookupContext) {
            bean = lookupContext.getBean(getBean(), Object.class);
        }

        if (bean != null) {
            ObjectDescriptor<?> descriptor = Describer.forObjectDescriptor(bean.getClass());

            evaluated = switch (getAction()) {
                case ConstantAccess(String constant) -> {
                    FieldDescriptor fieldDescriptor = descriptor.getType().getField(constant);

                    if (fieldDescriptor == null) {
                        throw new BeanAccessException("No constant '"+ constant +"' exist in bean '"+ descriptor.getType() +"'");
                    }

                    yield Reflections.getFieldValue(null, fieldDescriptor.unwrap());
                }
                case FieldAccess(String fieldName) -> {
                    FieldDescriptor fieldDescriptor = descriptor.getType().getField(fieldName);

                    if (fieldDescriptor == null) {
                        throw new BeanAccessException("No field '$"+ fieldName +"' exist in bean '"+ descriptor.getType() +"'");
                    }

                    yield Reflections.getFieldValue(bean, fieldDescriptor.unwrap());
                }
                case MethodCall(String method, Expression arguments) -> {
                    Object   evaluatedArguments = arguments.evaluate(context);
                    Object[] arrayArguments     = new Object[0];

                    if (evaluatedArguments instanceof Object[] array) {
                        arrayArguments = array;
                    }

                    Method   target = resolveMethod(bean.getClass(), method, arrayArguments.length);
                    Object[] passed = pack(target, arrayArguments);

                    MethodInvoker   invoker         = new MethodInvoker.Default(
                            new ArrayArgumentsMethodArgumentResolver(passed));
                    InvocableMethod invocableMethod = new InvocableMethod(bean, target);

                    yield invoker.invoke(new InvocationRequest.Default(
                            invocableMethod, EMPTY_CONTEXT
                    ));
                }
                default -> null;
            };
        }

        return evaluated;
    }

    /**
     * Finds the method a call means — by name <strong>and by how many arguments it was given</strong>.
     *
     * <h2>⚠️ Why this does not go through the object descriptor</h2>
     *
     * <p>{@code ClassTypeData} keys its methods by bare name — {@code methods.put(method.getName(), …)}
     * — so a class with two methods of one name keeps whichever was introspected last, and which one
     * that is nobody can see from the call site. Asking the descriptor therefore cannot answer this
     * question: the alternatives are gone before it is asked.</p>
     *
     * <p>Fixing that in the descriptor would change a published shape in {@code jmouse-core} that
     * property access, mapping and the tool layer all read. So bean access does its own lookup, and the
     * descriptor keeps answering what it is good at — fields and constants.</p>
     *
     * <h2>⚠️ Ambiguity is refused, never guessed</h2>
     *
     * <p>Two methods of one name and one arity cannot be told apart from an untyped call. Picking one is
     * how a script calls something nobody meant, so this says so instead — and names both.</p>
     *
     * @param type      the facade's class
     * @param name      the method name written in the expression
     * @param arity     how many arguments the call supplied
     * @return the method to invoke
     * @throws BeanAccessException when nothing matches, or when more than one does
     */
    private static Method resolveMethod(Class<?> type, String name, int arity) {
        return METHODS.computeIfAbsent(new MethodKey(type, name, arity), key -> {
            List<Method> named   = new ArrayList<>();
            List<Method> matched = new ArrayList<>();
            List<Method> spread  = new ArrayList<>();

            for (Method candidate : key.type().getMethods()) {
                if (!candidate.getName().equals(key.name())) {
                    continue;
                }

                // ⚠️ A bean exposes ITS methods — the ones somebody chose to write. `wait`, `notify`,
                // `hashCode` and `getClass` are on every object ever made and are nobody's choice, so a
                // caller reaching one has reached past what the bean's author published.
                //
                // ⚠️ `@bean.wait()` is the case that matters: it resolved, it invoked, and it is an
                // expression reaching for the host's monitor. Confirmed before this line existed — it
                // came back with IllegalMonitorStateException, which is to say it ran.
                if (candidate.getDeclaringClass() == Object.class) {
                    continue;
                }

                named.add(candidate);

                if (candidate.isVarArgs()) {
                    if (key.arity() >= candidate.getParameterCount() - 1) {
                        spread.add(candidate);
                    }
                } else if (candidate.getParameterCount() == key.arity()) {
                    matched.add(candidate);
                }
            }

            // ⚠️ A fixed-arity method beats a varargs one of the same name. `log(String)` and
            // `log(String, Object...)` are an ordinary pair, and the caller who wrote one argument meant
            // the first.
            List<Method> chosen = matched.isEmpty() ? spread : matched;

            if (chosen.size() == 1) {
                return chosen.getFirst();
            }

            if (chosen.isEmpty()) {
                throw new BeanAccessException(
                        "No method '%s' taking %d argument(s) exists on '%s'%s".formatted(
                                key.name(), key.arity(), key.type().getName(),
                                named.isEmpty() ? "" : "; it has " + arities(named)));
            }

            // ⚠️ The parentheses are load-bearing. Without them `.formatted` binds to the SECOND
            // literal only — which carries no placeholders — so every argument was silently dropped
            // and the reader got a message still containing '%s' and '%d'. A diagnostic that cannot
            // name the method or the type is worse than none, because it looks like one that did.
            throw new BeanAccessException(
                    ("'%s' on '%s' is ambiguous: %d methods take %d argument(s), and a call cannot say "
                     + "which. Give them different names.").formatted(
                            key.name(), key.type().getName(), chosen.size(), key.arity()));
        });
    }

    /**
     * Packs the trailing arguments of a varargs call into the array its last parameter expects.
     *
     * <p>⚠️ Without this a varargs method receives argument zero where it expects an array, and throws
     * {@code argument type mismatch} — a message about types, from a call whose types were never
     * wrong.</p>
     *
     * @param target    the method about to be invoked
     * @param arguments what the expression evaluated to
     * @return the arguments as that method's parameters expect them
     */
    private static Object[] pack(Method target, Object[] arguments) {
        if (!target.isVarArgs()) {
            return arguments;
        }

        int      fixed  = target.getParameterCount() - 1;
        Object[] packed = new Object[target.getParameterCount()];
        Object[] rest   = (Object[]) Array.newInstance(
                target.getParameterTypes()[fixed].getComponentType(), arguments.length - fixed);

        System.arraycopy(arguments, 0, packed, 0, fixed);
        System.arraycopy(arguments, fixed, rest, 0, rest.length);

        packed[fixed] = rest;

        return packed;
    }

    private static String arities(List<Method> named) {
        return named.stream()
                .map(method -> method.getParameterCount() + (method.isVarArgs() ? "+" : ""))
                .distinct()
                .sorted()
                .collect(Collectors.joining(", ", "methods taking ", " argument(s)"));
    }

    /** What a resolved method is remembered under. */
    private record MethodKey(Class<?> type, String name, int arity) {
    }

    /**
     * Writes this access back out in the form it was read in.
     *
     * <p>⚠️ <strong>Without this, rendering fails from the outside in.</strong> The default
     * {@link Expression#toSource()} throws, and the node that throws is buried — what a caller asked to
     * render was an ordinary {@link BinaryOperation}, so the failure surfaces at a whole condition and
     * says nothing about the {@code @} inside it. Anything that turns a tree back into text meets this:
     * a document writer, a normalising rewrite, a diff of two revisions, an editor that reformats.</p>
     *
     * <p>⚠️ <strong>An empty argument list is a {@code null} literal, not the word {@code null}.</strong>
     * The parser records "no arguments" by storing a {@link NullLiteralNode} where the arguments would
     * be, and that node renders itself as {@code null} — so a call written {@code @bean.method()} would
     * come back as {@code @bean.method(null)}: a document that still parses and no longer says the same
     * thing. One genuine null argument is an {@link ArgumentsNode} holding a null literal, which is a
     * different tree and renders correctly as {@code (null)}.</p>
     *
     * <p>Arguments render through their own {@code toSource()} rather than by re-reading the file: this
     * is a rendering of the tree, and a caller that wanted the original characters has
     * {@code SourceReading.text}.</p>
     *
     * @return the access as source text
     */
    @Override
    public String toSource() {
        return "@" + getBean() + switch (getAction()) {
            case MethodCall(String method, Expression arguments) ->
                    ".%s(%s)".formatted(method, arguments instanceof NullLiteralNode ? "" : arguments.toSource());
            case FieldAccess(String field) -> ":$" + field;
            case ConstantAccess(String constant) -> "#" + constant;
            case null, default -> "";
        };
    }

    @Override
    public String toString() {
        return toSource();
    }

    public enum AccessType {
        METHOD_CALL, FIELD_ACCESS, CONSTANT_ACCESS
    }

    public interface BeanAction {
    }

    public record MethodCall(String name, Expression arguments) implements BeanAction {
    }

    public record ConstantAccess(String name) implements BeanAction {
    }

    public record FieldAccess(String name) implements BeanAction {
    }

}
