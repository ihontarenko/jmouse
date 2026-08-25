package org.jmouse.query.translate;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.query.el.QueryLanguage;
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
 * declaration with a default has its default supplied instead.</p>
 *
 * <h2>⚠️ A default is evaluated against nothing</h2>
 *
 * <p>{@code since as temporal : now() - 30d} may call the language's own functions and may not read an
 * attribute — there is no row to read one from when a default is being decided. Evaluating it in a bare
 * context is what enforces that: an attribute reference simply has nowhere to resolve.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class DeclaredValues {

    /**
     * ⚠️ The LANGUAGE'S own vocabulary, built once — the fallback for a caller that has no language to
     * offer. A default may only use what the language provides, so nothing about this context varies per
     * call, and building a parser per translation to evaluate {@code now()} would be a cost paid on every
     * query.
     */
    private static final ExpressionLanguage BUILT_IN = new QueryLanguage().expressionLanguage();

    private DeclaredValues() {
    }

    /**
     * The bindings a block should be translated with, using the language's own vocabulary for defaults.
     *
     * <p>⚠️ Only for a caller with no language of its own. A product contributing functions —
     * {@code currentUser()}, a domain helper — must pass its own, or a default written with one fails on
     * one path and works on another.</p>
     */
    public static Bindings resolve(QueryBlockNode block, Bindings bindings) {
        return resolve(block, bindings, BUILT_IN);
    }

    /**
     * The bindings a block should actually be translated with.
     *
     * @param block    a view, or any other block — one that declares nothing is returned untouched
     * @param bindings what the caller supplied
     * @return the same bindings, with defaults filled in
     * @throws UnsupportedQueryException when something declared has neither a value nor a default
     */
    public static Bindings resolve(QueryBlockNode block, Bindings bindings, ExpressionLanguage language) {
        if (!(block instanceof ViewNode view)) {
            return bindings;
        }

        List<ParameterDeclarationNode> declared = new ArrayList<>(view.getParameters());

        declared.addAll(view.getAmbient());

        if (declared.isEmpty()) {
            return bindings;
        }

        Map<String, Object> resolved = new LinkedHashMap<>(bindings.asMap());
        List<String>        missing   = new ArrayList<>();

        for (ParameterDeclarationNode declaration : declared) {
            if (bindings.has(declaration.getName())) {
                continue;
            }

            if (declaration.getDefaultValue() == null) {
                missing.add(describe(view, declaration));
                continue;
            }

            resolved.put(declaration.getName(), evaluate(view, declaration, language));
        }

        if (!missing.isEmpty()) {
            throw new UnsupportedQueryException(
                    ("view %s needs %s, and nothing was supplied for %s; "
                     + "supply them by name or give them a default").formatted(
                            name(view), String.join(", ", missing),
                            missing.size() == 1 ? "it" : "them"));
        }

        return Bindings.of(resolved);
    }

    /**
     * A default, worked out without a row and without a database.
     *
     * <h2>⚠️ Not everything the language can COMPILE can be EVALUATED here</h2>
     *
     * <p>{@code now()} is not a function of the expression language — the compiler binds the clock once
     * per statement, so a moment means the same thing in every clause of one query. Evaluating it here
     * would need a second clock, and two clocks in one query is the bug that mechanism exists to
     * prevent.</p>
     *
     * <p>So a default that cannot be worked out standing alone is <strong>refused, naming what it
     * tried</strong>, rather than reaching a caller as an expression-language error about a missing
     * function. A literal default works; one that needs the clock does not yet.</p>
     */
    private static Object evaluate(
            ViewNode view, ParameterDeclarationNode declaration, ExpressionLanguage language) {

        try {
            return declaration.getDefaultValue().evaluate(language.newContext());
        } catch (RuntimeException undecidable) {
            throw new UnsupportedQueryException(
                    ("view %s gives '%s' the default %s, and that cannot be worked out on its own: %s. "
                     + "A default is decided with no row and no database, so it has to stand alone — "
                     + "supply the value by name instead")
                            .formatted(name(view), declaration.getName(),
                                    declaration.getDefaultValue().toSource(), undecidable.getMessage()));
        }
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
