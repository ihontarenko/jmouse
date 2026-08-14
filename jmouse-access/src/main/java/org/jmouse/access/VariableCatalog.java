package org.jmouse.access;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Every value that is true of <strong>every</strong> decision, and where each one comes from.
 *
 * <h2>Why this is not {@link ActionCatalog}</h2>
 *
 * <p>Because an action produces what that call is about — {@code purpose}, for a route listing one
 * purpose — and a variable is simply <em>there</em>, on the routes that name an action and on the ones
 * that name none. Folded into the action catalogue the two are indistinguishable, and the cost is not
 * theoretical: every action's declaration has to repeat every variable, a policy file states that
 * {@code entry.listByPurpose} produces {@code deployment} — which it does not — and the file is being
 * made to say something false in order to pass a check whose entire purpose is truthfulness.
 *
 * <p>So they are two catalogues, and a condition may read a name that is in either.
 *
 * <p>⚠️ <strong>Registered, never listed.</strong> Like {@link ActionCatalog} this is computed from
 * whatever publishes the values, not hand-kept beside it. A list written next to the publishers is one
 * commit behind from the day it is written, and it fails in the quiet direction — a rule mentioning a
 * name the list has lost is refused as a typo.
 *
 * <p>⚠️ <strong>Not consulted by the engine.</strong> Nothing on the decision path reads this. It
 * exists for whoever <em>checks</em> a rule that was written and for whatever offers a name to
 * somebody writing one.
 */
public interface VariableCatalog {

    /** What an installation attaching nothing has. Checks nothing, refuses nothing. */
    static VariableCatalog empty() {
        return Map::of;
    }

    /** A fixed catalogue. */
    static VariableCatalog of(Map<String, VariableKind> variables) {
        Map<String, VariableKind> copy = new TreeMap<>(variables);

        return () -> Map.copyOf(copy);
    }

    /** Every variable name, mapped to where its value comes from. */
    Map<String, VariableKind> kindsByName();

    /** Every variable name, in a stable order — what a picker on a screen offers. */
    default Set<String> all() {
        return new TreeSet<>(kindsByName().keySet());
    }

    default boolean contains(String name) {
        return kindsByName().containsKey(name);
    }

    /** Where one variable comes from, or {@code null} where nothing attaches that name. */
    default VariableKind kindOf(String name) {
        return kindsByName().get(name);
    }
}
