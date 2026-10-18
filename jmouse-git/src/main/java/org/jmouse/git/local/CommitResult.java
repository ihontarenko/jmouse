package org.jmouse.git.local;

/** The commit created by an explicit selection. */
public record CommitResult(String id, boolean indexUpdated) {}
