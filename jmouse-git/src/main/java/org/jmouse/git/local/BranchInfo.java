package org.jmouse.git.local;

/**
 * A local branch.
 *
 * @param name    its short name: {@code main}
 * @param head    the commit it stands at
 * @param current whether the working tree is on it
 */
public record BranchInfo(String name, String head, boolean current) {
}
