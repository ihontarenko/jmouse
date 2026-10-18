package org.jmouse.script.el;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.EvaluationException;
import org.jmouse.el.evaluation.ScopeValues;
import org.jmouse.el.evaluation.ScopedChain;
import org.jmouse.el.type.ValueType;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * What each scope of one evaluation declared with {@code let} and {@code const}: a name's type and whether it may be
 * written again. An assignment asks here before it writes, so {@code count = 'x'} after {@code let count : int} and
 * any write to a {@code const} are refused where they happen, not discovered later.
 *
 * <p>Kept per context, beside the scopes it describes, and keyed by the scope itself: a body's scope is popped when
 * it ends, and its declarations go with it.</p>
 */
public final class Declarations {

    /**
     * One declared name.
     *
     * @param name     the name
     * @param type     its type; {@code null} for an untyped declaration, which takes anything
     * @param constant whether it may not be written again — a {@code const}, a {@code parameter}
     * @param refusal  what a write to it is refused with, when it is constant
     */
    public record Declaration(String name, ValueType type, boolean constant, String refusal) {

        /** A name that may be written again. */
        public Declaration(String name, ValueType type) {
            this(name, type, false, null);
        }
    }

    /** A declared name and the scope it was declared in. */
    public record Found(Declaration declaration, ScopeValues scope) {
    }

    private final Map<ScopeValues, Map<String, Declaration>> declared = new IdentityHashMap<>();

    /** The declarations of this context, made on first use. */
    public static Declarations of(EvaluationContext context) {
        Object kept = context.getObject(Declarations.class);
        if (kept instanceof Declarations declarations) {
            return declarations;
        }
        Declarations declarations = new Declarations();
        context.setObject(Declarations.class, declarations);
        return declarations;
    }

    /** Records {@code declaration} in {@code scope}, shadowing whatever an outer scope declared under its name. */
    public void declare(ScopeValues scope, Declaration declaration) {
        declared.computeIfAbsent(scope, ignored -> new HashMap<>()).put(declaration.name(), declaration);
    }

    /** Whether {@code scope} itself declared {@code name}. */
    public boolean declaredIn(ScopeValues scope, String name) {
        return inScope(scope, name) != null;
    }

    /** The nearest declaration of {@code name} along the chain, innermost first; null when none declared it. */
    public Found find(ScopedChain chain, String name) {
        Found found = inScope(chain.peek(), name);
        if (found != null) {
            return found;
        }
        for (ScopeValues scope : chain.chain()) {
            found = inScope(scope, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * {@code value} as the declaration found for {@code name} admits it, written into the scope that declared it.
     *
     * @return the value written, or {@code null} with nothing written when no declaration names it
     * @throws EvaluationException when the name is a constant, or the value is not of its type
     */
    public static Object assign(EvaluationContext context, String name, Object value) {
        Found found = of(context).find(context.getScopedChain(), name);
        if (found == null) {
            return null;
        }
        Declaration declaration = found.declaration();
        if (declaration.constant()) {
            throw new EvaluationException("'" + name + "' " + declaration.refusal());
        }
        Object admitted = admit(context, declaration, value);
        found.scope().set(name, admitted);
        return admitted;
    }

    /** {@code value} as a declaration's type admits it; untouched for an untyped one. */
    public static Object admit(EvaluationContext context, Declaration declaration, Object value) {
        if (declaration.type() == null) {
            return value;
        }
        try {
            return declaration.type().admit(value, context.getConversion());
        } catch (EvaluationException refused) {
            throw new EvaluationException("'" + declaration.name() + " : " + declaration.type().name() + "' — "
                    + refused.getMessage(), refused);
        }
    }

    private Found inScope(ScopeValues scope, String name) {
        Map<String, Declaration> names = declared.get(scope);
        Declaration declaration = names == null ? null : names.get(name);
        return declaration == null ? null : new Found(declaration, scope);
    }
}
