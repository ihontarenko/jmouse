package org.jmouse.files.management.access;

import org.jmouse.access.ScopeReference;
import org.jmouse.access.spi.ScopeHierarchy;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 🌲🌳 A product with more than one tree, as the one hierarchy the engine takes.
 *
 * <h3>⚠️ Why this is not a product's problem to solve</h3>
 *
 * <p>The engine injects a single {@link ScopeHierarchy}. The moment a product has two trees — pages in
 * its own sections, files in the library's directories — it has two beans of that type, and the context
 * refuses to start. Then it starts again, refusing differently, because the composite is a third bean of
 * the same type.</p>
 *
 * <p>Both failures are in Kiwi's history and neither says anything about trees. Every product that adopts
 * directories would meet them in the same order, so the composite ships here.</p>
 *
 * <h3>⚠️ Delegation by kind, never a merge</h3>
 *
 * <p>Each delegate already refuses a place of a kind it does not own, which is what keeps two trees
 * genuinely separate: a grant over a section cannot widen into a directory, because the section hierarchy
 * has nothing to say about directory identifiers and the directory hierarchy has nothing to say about
 * section ones. That separation is the reason for having two trees rather than one.</p>
 *
 * <p>⚠️ It is <strong>not</strong> the only thing keeping them apart — see {@code JMF-40}: the scope model
 * has a single linear width order, so two sibling place scopes cannot be declared as siblings, and an
 * ambient place of the wider kind would still be stamped onto the narrower one's target.</p>
 */
public class SeveralTreesScopeHierarchy implements ScopeHierarchy {

    private final List<ScopeHierarchy> trees;

    /**
     * 🏗️ Ask every tree, and let each answer about its own kind of place.
     *
     * @param trees the hierarchies this product runs
     */
    public SeveralTreesScopeHierarchy(List<ScopeHierarchy> trees) {
        this.trees = List.copyOf(trees);
    }

    /**
     * 🌿 Everything one place sits inside.
     *
     * <p>At most one tree answers — the one that owns that kind of place.</p>
     *
     * @param place the place
     * @return its ancestors
     */
    @Override
    public List<ScopeReference> containing(ScopeReference place) {
        return trees.stream()
                .map(tree -> tree.containing(place))
                .filter(found -> !found.isEmpty())
                .findFirst()
                .orElseGet(List::of);
    }

    /**
     * 🌿 Everything inside these places.
     *
     * <p>Unioned, because a person may hold grants in both trees at once and the engine asks about all of
     * them together.</p>
     *
     * @param places the places granted
     * @return the union of their subtrees
     */
    @Override
    public Set<ScopeReference> within(Collection<ScopeReference> places) {
        Set<ScopeReference> found = new LinkedHashSet<>();

        for (ScopeHierarchy tree : trees) {
            found.addAll(tree.within(places));
        }

        return found;
    }
}
