package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ScopeValues;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.type.ValueType;
import org.jmouse.script.el.Declarations;

/**
 * {@code let count : int = 1} or {@code const limit = 10} — a name bound for the rest of a body, with an optional type
 * every value written to it must be, and, for a constant, no second write.
 *
 * <p>⚠️ <strong>Binds in the current scope, not through {@code context.setValue}</strong>: {@code setValue} walks the
 * chain looking for a scope that already holds the name and writes there, so a declaration would quietly overwrite an
 * outer variable of the same name rather than shadowing it. {@link ScriptBodyNode} makes sure there is a scope of this
 * body's own to shadow in.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class DeclarationNode extends AbstractExpression {

    /** How long a declared name lives, and whether it may be written again. */
    public enum Kind {
        /** This body's; may be assigned again — with a value of its type. */
        LET("let", false, false),
        /** This body's; may not be assigned again. */
        CONST("const", true, false),
        /**
         * The document's: bound once per context, in its outermost scope, and kept across events — what a handler
         * remembers between one dispatch and the next. Written again freely.
         */
        STATE("state", false, true),
        /**
         * The host's to give: bound once per context, in its outermost scope, to the value the host put in the context
         * under that name, else to the written default; the document may not write it.
         */
        PARAMETER("parameter", true, true);

        private final String  keyword;
        private final boolean constant;
        private final boolean ofTheDocument;

        Kind(String keyword, boolean constant, boolean ofTheDocument) {
            this.keyword = keyword;
            this.constant = constant;
            this.ofTheDocument = ofTheDocument;
        }

        public String keyword() {
            return keyword;
        }

        public boolean isConstant() {
            return constant;
        }

        /** Whether it is bound once per context in the outermost scope rather than in the body it is written in. */
        public boolean isOfTheDocument() {
            return ofTheDocument;
        }
    }

    private Kind       kind = Kind.LET;
    private String     name;
    private ValueType  type;
    private Expression value;

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** Its type; {@code null} when it was written without one, and takes anything. */
    public ValueType getType() {
        return type;
    }

    public void setType(ValueType type) {
        this.type = type;
    }

    /** What it is bound to; {@code null} for {@code let c : current}, declared without a value. */
    public Expression getValue() {
        return value;
    }

    public void setValue(Expression value) {
        this.value = value;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        Declarations.Declaration declaration = new Declarations.Declaration(name, type, kind.isConstant(),
                kind == Kind.PARAMETER ? "is a parameter: the host gives it, and the document cannot write it."
                        : "is a constant; it cannot be assigned again.");
        Declarations declarations = Declarations.of(context);
        ScopeValues scope = kind.isOfTheDocument() ? outermost(context) : context.getScopedChain().peek();

        // The document's names are bound once per context: a second pass — the next event — finds them as they were.
        if (kind.isOfTheDocument() && declarations.declaredIn(scope, name)) {
            return scope.get(name);
        }

        Object given = kind == Kind.PARAMETER ? context.getValue(name) : null;
        Object written = given != null ? given : value == null ? null : value.evaluate(context);
        Object admitted = Declarations.admit(context, declaration, written);

        scope.set(name, admitted);
        declarations.declare(scope, declaration);

        return admitted;
    }

    /** The scope every other one in the context is nested in: where the document's own names live. */
    private static ScopeValues outermost(EvaluationContext context) {
        ScopeValues outermost = context.getScopedChain().peek();
        for (ScopeValues scope : context.getScopedChain().chain()) {
            outermost = scope;
        }
        return outermost;
    }

    @Override
    public String toSource() {
        return kind.keyword() + " " + name + (type == null ? "" : " : " + type.name())
                + (value == null ? "" : " = " + value.toSource());
    }

    @Override
    public String toString() {
        return "%s['%s'%s]".formatted(kind, name, type == null ? "" : " : " + type.name());
    }

}
