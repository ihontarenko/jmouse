package org.jmouse.git.local;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A repository's own settings are set as {@code git config} sets them, where git working in the folder reads them. */
class RepositorySettingsTest {

    @TempDir
    Path folder;

    private final LocalGit git = new JGitLocalGit();

    @Test
    void aSettingIsWrittenIntoTheRepositorysOwnSettings() throws Exception {
        git.initialize(folder);
        git.configure(folder, "core.longpaths", "true");
        try (Git opened = Git.open(folder.toFile())) {
            assertEquals("true", opened.getRepository().getConfig().getString("core", null, "longpaths"));
        }
    }

    @Test
    void aSubsectionIsTheMiddleOfItsKey() throws Exception {
        git.initialize(folder);
        git.configure(folder, "remote.origin.url", "https://example.org/library.git");
        try (Git opened = Git.open(folder.toFile())) {
            assertEquals("https://example.org/library.git", opened.getRepository().getConfig().getString("remote", "origin", "url"));
        }
    }

    @Test
    void aKeyWithoutItsSectionIsRefused() {
        git.initialize(folder);
        assertThrows(LocalGitException.class, () -> git.configure(folder, "longpaths", "true"));
    }
}
