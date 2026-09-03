package org.jmouse.files.management;

import org.jmouse.files.OwnerReference;
import org.jmouse.files.directory.DirectoryUploadConfiguration;
import org.jmouse.files.directory.EffectiveUploadRule;
import org.jmouse.files.jpa.directory.StorageDirectories;
import org.jmouse.files.jpa.directory.StorageDirectory;
import org.jmouse.files.jpa.directory.StorageDirectoryConfigurations;
import org.jmouse.storage.policy.UploadPolicy;
import org.jmouse.storage.policy.UploadPolicyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🛃 A folder decides what may enter it — and so does the nearest one above it that has an opinion.
 *
 * <h3>The rule, in one sentence</h3>
 *
 * <p><strong>Walk up from the destination; the first directory carrying an {@code upload} configuration
 * wins, entirely; if nobody carries one, the installation's policy applies.</strong></p>
 *
 * <p>No merging. Not "the strictest wins", not "allowlists union and denylists intersect". The nearest
 * configuration <em>replaces</em> everything, because a rule assembled out of four ancestors is one
 * nobody can read off a single screen — and a rule nobody can read is a rule nobody can be responsible
 * for.</p>
 *
 * <p>⚠️ Both directions, one mechanism: the mode is part of a folder's configuration, so a folder may
 * be <em>stricter</em> than a permissive installation exactly as easily as it may be looser.</p>
 *
 * <h3>⚠️ Anything that is not a directory keeps the installation's policy</h3>
 *
 * <p>A file bound to {@code ISSUE:…}, {@code PAGE:…} or {@code ENTRY_FIELD:…} has no place in the tree
 * to inherit from. Do not invent one — this is the file cabinet's mechanism, deliberately.</p>
 *
 * <h3>⚠️ Tightening a rule is not retroactive</h3>
 *
 * <p>A rule governs <strong>entry</strong>, never residence. Files already in a folder stay when its
 * rule narrows, so a listing may well show files the folder would now refuse. That is decided rather
 * than missed, and a screen is where it is made legible; nobody should "fix" it here.</p>
 */
public class DirectoryUploadPolicyResolver
        implements UploadPolicyResolver, DirectoryConfigurationResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryUploadPolicyResolver.class);

    private final StorageDirectories             directories;
    private final StorageDirectoryConfigurations configurations;
    private final UploadPolicy                   installationPolicy;
    private final long                           installationMaxSizeBytes;

    /**
     * Resolved policies, by directory identifier.
     *
     * <p>⚠️ <strong>A real cache, not a memo inside the method it is meant to accelerate.</strong> This
     * runs on every upload, and without it resolving a chain costs a query per level of the tree.</p>
     *
     * <p>The key is the directory identifier and nothing else — a {@code String}, whose {@code equals}
     * is as sharp as it looks. A key comparing less sharply than it appears would hand one folder's
     * policy to another, and the symptom would be a file accepted somewhere it should not have been,
     * which nobody ever reports as a bug.</p>
     */
    private final Map<String, ResolvedUploadRule> resolved = new ConcurrentHashMap<>();

    /**
     * 🛃 A rule and the folder it came from.
     *
     * <p>⚠️ The origin is not decoration. From below, an inherited configuration and a folder's own look
     * <strong>identical</strong> — a screen cannot derive "set here" from "inherited from
     * {@code innoventa/files}", and the difference is between a folder somebody configured and one that
     * merely sits under one.</p>
     *
     * @param policy              what applies
     * @param originDirectoryId   which folder supplied it, or {@code null} when nobody did and this is
     *                            the installation's own rule
     */
    public record ResolvedUploadRule(UploadPolicy policy, String originDirectoryId) {

        /**
         * 🏛️ Whether this is the installation's rule rather than any folder's.
         *
         * @return {@code true} when no folder in the chain had an opinion
         */
        public boolean fromInstallation() {
            return originDirectoryId == null;
        }
    }

    /**
     * 🏗️ Build the resolver over the tree and its configurations.
     *
     * @param directories              the tree, for the ancestor chain
     * @param configurations           what folders say about themselves
     * @param installationPolicy       what applies where no folder has an opinion
     * @param installationMaxSizeBytes the size limit a folder keeps unless it states its own
     */
    public DirectoryUploadPolicyResolver(StorageDirectories directories,
                                         StorageDirectoryConfigurations configurations,
                                         UploadPolicy installationPolicy,
                                         long installationMaxSizeBytes) {
        this.directories              = directories;
        this.configurations           = configurations;
        this.installationPolicy       = installationPolicy;
        this.installationMaxSizeBytes = installationMaxSizeBytes;
    }

    /**
     * 🛃 The rule governing content headed for this destination.
     *
     * @param ownerType what kind of thing will hold it
     * @param ownerId   which one
     * @return the policy to judge it by
     */
    @Override
    public UploadPolicy policyFor(String ownerType, String ownerId) {
        if (!OwnerReference.DIRECTORY.equals(ownerType) || ownerId == null) {
            return installationPolicy;
        }

        return resolve(ownerId).policy();
    }

    /**
     * 🛃 The rule that applies to a folder, and where it came from.
     *
     * <p>What a screen needs: the same walk, reported rather than merely applied.</p>
     *
     * @param directoryId the folder
     * @return the rule and its origin
     */
    public ResolvedUploadRule resolve(String directoryId) {
        return resolved.computeIfAbsent(directoryId, this::walkUpFrom);
    }

    /**
     * 🔎 The upload rule that applies to this folder, and where it came from.
     *
     * <p>⚠️ The origin is what a screen cannot work out for itself: from below, an inherited rule and a
     * folder's own look identical.</p>
     *
     * @param directory the folder
     * @return the {@code upload} kind, mapped to what applies
     */
    @Override
    public Map<String, DirectoryConfigurationView> describe(StorageDirectory directory) {
        ResolvedUploadRule    rule      = resolve(directory.getId());
        EffectiveUploadRule   effective = EffectiveUploadRule.of(rule.policy());
        String                kind      = DirectoryUploadConfiguration.KIND.name();

        if (rule.fromInstallation()) {
            return Map.of(kind, DirectoryConfigurationView.installation(effective));
        }

        if (directory.getId().equals(rule.originDirectoryId())) {
            return Map.of(kind, DirectoryConfigurationView.self(effective));
        }

        // The ancestor's PATH, not merely "inherited" — a person about to change a rule has to be told
        // which folder they would actually be changing.
        String originPath = directories.ancestorsOf(directory.getId()).stream()
                .filter(ancestor -> ancestor.getId().equals(rule.originDirectoryId()))
                .map(StorageDirectory::getPath)
                .findFirst()
                .orElse(null);

        return Map.of(kind, DirectoryConfigurationView.inherited(effective, originPath));
    }

    /**
     * 🧹 Forget what a folder and everything under it resolved to.
     *
     * <p>⚠️ <strong>The whole subtree, never one row.</strong> Writing or clearing a configuration
     * changes what every descendant <em>without</em> one of its own resolves to. So does a move — a
     * moved subtree inherits from somewhere else entirely — and so does a delete. Three eviction
     * points, and the move is the one that gets missed.</p>
     *
     * @param directory the folder whose subtree should be forgotten
     */
    @Override
    public void evictSubtreeOf(StorageDirectory directory) {
        if (directory == null) {
            return;
        }

        for (StorageDirectory each : directories.subtreeOf(directory)) {
            resolved.remove(each.getId());
        }
    }

    /**
     * 🧹 Forget everything.
     *
     * <p>For the cases where the subtree is no longer walkable — a folder already removed, a move whose
     * numbering has been rebuilt. Cheap: the next upload into each folder pays one chain read.</p>
     */
    @Override
    public void evictEverything() {
        resolved.clear();
    }

    /**
     * 🌿 The nearest configuration above this directory, or the installation's rule.
     *
     * <p>⚠️ <strong>One query for the chain, not one per ancestor.</strong> Walking level by level is
     * the N+1 that stays invisible until a tree is deep.</p>
     */
    private ResolvedUploadRule walkUpFrom(String directoryId) {
        List<String> chain = chainFrom(directoryId);

        Map<String, DirectoryUploadConfiguration> carried =
                configurations.findAll(chain, DirectoryUploadConfiguration.KIND);

        for (String each : chain) {
            DirectoryUploadConfiguration configuration = carried.get(each);

            if (configuration != null) {
                LOGGER.debug("🛃 Directory '{}' takes its upload rule from '{}'", directoryId, each);

                return new ResolvedUploadRule(configuration.asPolicy(installationMaxSizeBytes), each);
            }
        }

        return new ResolvedUploadRule(installationPolicy, null);
    }

    /**
     * 🧭 The directory itself, then its ancestors, NEAREST FIRST.
     *
     * <p>⚠️ <strong>{@code ancestorsOf} answers outermost first</strong> — it orders by the nested-set
     * left bound, so a root comes back before a leaf's parent. Reversing it is the whole difference
     * between "the nearest configuration wins" and "a root's rule silently overrides every folder
     * beneath it", and both orders look perfectly correct in a debugger.</p>
     */
    private List<String> chainFrom(String directoryId) {
        List<StorageDirectory> ancestors = directories.ancestorsOf(directoryId);
        List<String>           chain     = new ArrayList<>(ancestors.size() + 1);

        chain.add(directoryId);

        for (int index = ancestors.size() - 1; index >= 0; index--) {
            chain.add(ancestors.get(index).getId());
        }

        return chain;
    }
}
