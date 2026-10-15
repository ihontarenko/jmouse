package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.script.el.node.FunctionDeclarationNode;
import org.jmouse.script.el.node.IncludeNode;
import org.jmouse.script.el.node.ReturnSignal;
import org.jmouse.script.el.node.ScriptBodyNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A parsed jMS program: its top-level statements, run in order, and the functions and includes declared beside
 * them. A function declaration is not a statement — it is lifted out at parse time, so running the program never
 * runs a function by accident; it is called by name. {@code return} at the top level ends the program with a value.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ProgramNode extends ScriptBodyNode {

    private final String                               name;
    private final Map<String, FunctionDeclarationNode> functions = new LinkedHashMap<>();
    private final List<String>                         includes  = new ArrayList<>();

    public ProgramNode(String name, List<Expression> statements) {
        this.name = name;
        for (Expression statement : statements) {
            switch (statement) {
                case FunctionDeclarationNode function -> declare(function);
                case IncludeNode include -> includes.add(include.getPath());
                default -> addExpression(statement);
            }
        }
    }

    public String getName() {
        return name;
    }

    /** The functions this program declares, by name, in the order written. */
    public Map<String, FunctionDeclarationNode> getFunctions() {
        return Collections.unmodifiableMap(functions);
    }

    /** The paths it includes; each contributes its functions, never its statements. */
    public List<String> getIncludes() {
        return List.copyOf(includes);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        try {
            return evaluateBody(context);
        } catch (ReturnSignal signal) {
            return signal.value();
        }
    }

    @Override
    protected String describe() {
        return "the program '" + name + "'";
    }

    @Override
    public String toString() {
        return "PROGRAM['%s']".formatted(name);
    }

    private void declare(FunctionDeclarationNode function) {
        if (functions.putIfAbsent(function.getName(), function) != null) {
            throw new IllegalArgumentException("'%s' declares the function '%s' twice".formatted(name, function.getName()));
        }
    }
}
