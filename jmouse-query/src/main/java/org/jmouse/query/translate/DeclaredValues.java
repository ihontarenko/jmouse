package org.jmouse.query.translate;

import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.TranslationRefusedException;

import org.jmouse.el.node.Expression;
import org.jmouse.query.el.node.ParameterDeclarationNode;
import org.jmouse.query.el.node.QueryBlockNode;
import org.jmouse.query.el.node.ViewNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a view declared, against what the caller actually supplied.
 *
 * <h2>⚠️ Never null when missing</h2>
 *
 * <p>A view declaring {@code hot(since as temporal)} and run without {@code since} used to compile: the
 * checker allowed the name because the view declared it, and the compiler then bound whatever
 * {@code null} is on that backend. The query ran. It returned rows. Nobody had a reason to look.</p>
 *
 * <p>That is the same failure a source declaration is forbidden from — a query silently reading the wrong
 * thing with no message anywhere — and it is worse here, because the name in question is frequently a
 * tenant, a workspace or the current member.</p>
 *
 * <p>So: a declaration without a default that nobody supplied is <strong>refused, by name</strong>, and a
 * declaration with a default has its default put where the name is used instead.</p>
 *
 * <h2>⚠️ A default is not worked out — it is COMPILED where the name stands</h2>
 *
 * <p>{@code since as temporal : now() - days(30)} is an expression the backend already knows how to
 * compile, and it is handed over as one. Evaluating it here instead would need an evaluation context of
 * its own, and {@code now()} is precisely the thing that must not have one: the compiler binds the clock
 * <strong>once per statement</strong> so that a moment means the same thing in every clause of one
 * query. A default evaluated separately would carry a second clock, differing from the statement's by
 * however long compilation took — two moments in one query, which is the bug binding it once exists to
 * prevent.</p>
 *
 * <p>Handing over the tree costs nothing and gains the rest: a default may use anything the query itself
 * may use, including whatever vocabulary the product contributed, because the same compiler reads
 * both.</p>
 *
 * <p>⚠️ And the tree is handed over, never installed. Nothing here touches the parsed view — a view
 * compiled once without the value and again with it must mean two different things, and a default baked
 * into the view by the first compilation would make the second read correctly and answer wrongly.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class DeclaredValues {

    private DeclaredValues() {
    }

    /**
     * What a block should actually be translated with: the caller's values, and an expression for every
     * declared name the caller left out.
     *
     * @param bindings what the caller supplied, unchanged
     * @param defaults the expression standing in for each name nobody supplied, by name
     */
    public record Declared(Bindings bindings, Map<String, Expression> defaults) {

        public Declared {
            defaults = Map.copyOf(defaults);
        }

        /** Nothing was declared, so there is nothing to stand in for. */
        public static Declared of(Bindings bindings) {
            return new Declared(bindings, Map.of());
        }

        public Map<String, Object> asMap() {
            return bindings.asMap();
        }
    }

    /**
     * Reads what a block declared against what the caller supplied.
     *
     * @param block    a view, or any other block — one that declares nothing is returned untouched
     * @param bindings what the caller supplied
     * @return the caller's values, plus an expression for each name left out that has a default
     * @throws TranslationRefusedException when something declared has neither a value nor a default
     */
    public static Declared resolve(QueryBlockNode block, Bindings bindings) {
        if (!(block instanceof ViewNode view)) {
            return Declared.of(bindings);
        }

        List<ParameterDeclarationNode> declared = new ArrayList<>(view.getParameters());

        declared.addAll(view.getAmbient());

        if (declared.isEmpty()) {
            return Declared.of(bindings);
        }

        Map<String, Expression> defaults = new LinkedHashMap<>();
        List<String>            missing  = new ArrayList<>();

        for (ParameterDeclarationNode declaration : declared) {
            if (bindings.has(declaration.getName())) {
                continue;
            }

            if (declaration.getDefaultValue() == null) {
                missing.add(describe(view, declaration));
                continue;
            }

            defaults.put(declaration.getName(), declaration.getDefaultValue());
        }

        if (!missing.isEmpty()) {
            throw new TranslationRefusedException(
                    ("view %s needs %s, and nothing was supplied for %s; "
                     + "supply them by name or give them a default").formatted(
                            name(view), String.join(", ", missing),
                            missing.size() == 1 ? "it" : "them"));
        }

        return new Declared(bindings, defaults);
    }

    /**
     * ⚠️ The refusal says which KIND was declared, because a parameter and an ambient value are supplied
     * from different places — one at the call, one on the context above — and somebody who declared the
     * one and meant the other would otherwise read a message that looks correct.
     */
    private static String describe(ViewNode view, ParameterDeclarationNode declaration) {
        boolean isParameter = view.getParameters().contains(declaration);

        return "%s '%s'".formatted(isParameter ? "parameter" : "the value", declaration.getName());
    }

    private static String name(ViewNode view) {
        return view.getIdentifier().map("'%s'"::formatted).orElseGet(() -> "'%s'".formatted(view.getTitle()));
    }
}
