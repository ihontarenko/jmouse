package org.jmouse.files.management.access;

import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.spi.ScopeHierarchy;
import org.jmouse.files.jpa.directory.StorageDirectories;
import org.jmouse.files.jpa.directory.StorageDirectory;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 🌳 A grant on a directory covers everything below it.
 *
 * <h3>Why this is the library's and not a product's</h3>
 *
 * <p>It was a product's, once — Kiwi wrote it — and every product wanting per-folder grants would have
 * written the same forty lines over the same {@link StorageDirectories}. The only product-specific thing
 * in it is <em>which {@link ScopeKind} constant</em> stands for a directory, because the engine
 * cross-checks a product's registered scopes against its own policy file in both directions. So that one
 * value is a constructor argument and the rest is here.</p>
 *
 * <p>⚠️ Both methods are asked on <strong>every authorized read of a file</strong>, which is why both are
 * indexed range queries rather than walks — and why the library's tree is a real nested set rather than a
 * parent pointer.</p>
 */
public class DirectoryScopeHierarchy implements ScopeHierarchy {

    private final StorageDirectories directories;
    private final ScopeKind          directoryScope;

    /**
     * 🏗️ Answer about directories, in this product's vocabulary.
     *
     * @param directories    the tree
     * @param directoryScope what this product calls a directory scope
     */
    public DirectoryScopeHierarchy(StorageDirectories directories, ScopeKind directoryScope) {
        this.directories    = directories;
        this.directoryScope = directoryScope;
    }

    /**
     * 🌿 Every directory this one sits inside, outermost first.
     *
     * @param place the directory
     * @return its ancestors
     */
    @Override
    public List<ScopeReference> containing(ScopeReference place) {
        if (!directoryScope.equals(place.type()) || place.id() == null) {
            return List.of();
        }

        return directories.ancestorsOf(place.id()).stream()
                .sorted(Comparator.comparingInt(StorageDirectory::getTreeLeft))
                .map(ancestor -> ScopeReference.of(directoryScope, ancestor.getId()))
                .toList();
    }

    /**
     * 🌿 Everything inside these, themselves included.
     *
     * @param places the directories granted
     * @return the subtree
     */
    @Override
    public Set<ScopeReference> within(Collection<ScopeReference> places) {
        List<String> identifiers = places.stream()
                .filter(place -> directoryScope.equals(place.type()) && place.id() != null)
                .map(ScopeReference::id)
                .toList();

        if (identifiers.isEmpty()) {
            return Set.of();
        }

        return directories.subtreesOf(identifiers).stream()
                .map(descendant -> ScopeReference.of(directoryScope, descendant.getId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
