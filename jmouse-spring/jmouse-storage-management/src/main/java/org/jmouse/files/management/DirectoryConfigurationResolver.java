package org.jmouse.files.management;

import org.jmouse.files.jpa.directory.StorageDirectory;

import java.util.Map;

/**
 * 🔧 Whoever turns a folder's stored rows into the rule that actually applies to it.
 *
 * <p>One implementation per <strong>kind</strong> of directory configuration, and the tree's service
 * holds however many exist — which is what lets a second kind arrive without this interface, that
 * service or any view changing shape. {@code DirectoryUploadPolicyResolver} is the first one.</p>
 *
 * <h3>Two halves, and they always travel together</h3>
 *
 * <p>Resolving walks a folder's ancestors, so anything doing it caches, and anything caching is caching
 * a fact about a whole branch. Three things falsify it:</p>
 *
 * <ul>
 *   <li>a configuration <strong>written or cleared</strong> — every descendant without one of its own
 *       resolves differently now;</li>
 *   <li>a folder <strong>moved</strong> — the moved subtree inherits from somewhere else entirely, and
 *       ⚠️ this is the one that gets missed;</li>
 *   <li>a folder <strong>deleted</strong>.</li>
 * </ul>
 */
public interface DirectoryConfigurationResolver {

    /**
     * 🔎 What applies to this folder, of this resolver's kind, and where it came from.
     *
     * @param directory the folder
     * @return the kinds this resolver speaks for, mapped to what applies; empty where it has nothing
     *         to say about this folder
     */
    Map<String, DirectoryConfigurationView> describe(StorageDirectory directory);

    /**
     * 🧹 Forget what this folder and everything under it resolved to.
     *
     * @param directory the folder whose subtree changed
     */
    void evictSubtreeOf(StorageDirectory directory);

    /**
     * 🧹 Forget everything.
     *
     * <p>For when the subtree is no longer walkable — a folder already removed, a move whose numbering
     * has been rebuilt. Cheap: each folder pays one chain read on its next upload.</p>
     */
    void evictEverything();
}
