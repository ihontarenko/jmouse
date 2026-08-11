package org.jmouse.access.jpa;

import org.jmouse.access.spi.BundledPermission;

import java.util.List;

/**
 * What a policy <em>document</em> says a role carries, for roles whose bundle is not in the table.
 *
 * <h2>⚠️ Why the store needs this at all</h2>
 *
 * <p>A role's bundle may live in a file and the <em>assignment</em> of that role is still a row. Without
 * the union, moving a bundle out of the migrations would leave every holder of that role conferring
 * <strong>nothing</strong> — silently, because an empty bundle is a perfectly ordinary thing for a role
 * to have.
 *
 * <p>It is a seam rather than a direct call into the policy module because a <em>dry run</em> builds one
 * of these over a <em>candidate</em> document and asks the same store what it would confer. That is only
 * honest if the store can be handed bundles other than the ones in force.
 *
 * <p>A product with no policy documents registers {@link #none()} and loses nothing.
 */
@FunctionalInterface
public interface DeclaredRoleBundles {

    /** What the document says this role carries, or empty where it says nothing about it. */
    List<BundledPermission> bundleOf(String roleName);

    /** For an installation whose roles are entirely in the table. */
    static DeclaredRoleBundles none() {
        return roleName -> List.of();
    }
}
