package org.jmouse.git;

import org.eclipse.jgit.lib.Repository;

import java.util.regex.Pattern;

/** Which branch an operation reads: the one the remote names as its default, or one named by the caller. */
public sealed interface BranchSelection permits BranchSelection.DefaultBranch, BranchSelection.NamedBranch {

    /** The branch the remote's {@code HEAD} points at. */
    static DefaultBranch defaultBranch() {
        return DefaultBranch.INSTANCE;
    }

    /** One branch by its short name, such as {@code main} or {@code release/2.0}. */
    static NamedBranch named(String name) {
        return new NamedBranch(name);
    }

    /** The remote's default branch. */
    record DefaultBranch() implements BranchSelection {

        private static final DefaultBranch INSTANCE = new DefaultBranch();
    }

    /**
     * A branch by short name. The name must be one git accepts, minus anything that could read as a command-line
     * option or climb a path.
     */
    record NamedBranch(String name) implements BranchSelection {

        private static final Pattern BRANCH_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._/-]{0,199}");

        public NamedBranch {
            if (name == null || !BRANCH_NAME.matcher(name).matches() || name.contains("..") || name.contains("//")
                    || name.endsWith("/") || name.endsWith(".") || name.endsWith(".lock")
                    || !Repository.isValidRefName("refs/heads/" + name)) {
                throw new IllegalArgumentException("That is not a branch name git accepts.");
            }
        }
    }
}
