package org.jmouse.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/** A local repository built with JGit in a temporary directory, standing in for a remote. */
final class SourceRepository implements AutoCloseable {

    private static final PersonIdent AUTHOR = new PersonIdent("Test Author", "author@example.test");

    private final Path directory;
    private final Git  git;

    private SourceRepository(Path directory, Git git) {
        this.directory = directory;
        this.git = git;
    }

    static SourceRepository create(Path directory) throws GitAPIException {
        return new SourceRepository(directory, Git.init().setDirectory(directory.toFile()).setInitialBranch("main").call());
    }

    /** The remote address, as a {@code file://} URI. */
    URI address() {
        return URI.create("file:///" + directory.toAbsolutePath().toString().replace('\\', '/'));
    }

    String commit(String file, String content) throws IOException, GitAPIException {
        return commit(file, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    String commit(String file, byte[] content) throws IOException, GitAPIException {
        Path target = directory.resolve(file);
        Files.createDirectories(target.getParent());
        Files.write(target, content);
        git.add().addFilepattern(file).call();
        RevCommit commit = git.commit()
                .setMessage("Change " + file)
                .setAuthor(AUTHOR)
                .setCommitter(AUTHOR)
                .setSign(false)
                .call();
        return commit.name();
    }

    /** One commit writing {@code count} files under {@code files/}, each reading "{prefix} {index}". */
    String commitMany(String prefix, int count) throws IOException, GitAPIException {
        Path folder = Files.createDirectories(directory.resolve("files"));
        for (int index = 0; index < count; index++) {
            Files.writeString(folder.resolve(index + ".txt"), prefix + " " + index);
        }
        git.add().addFilepattern("files").call();
        return git.commit()
                .setMessage("Write " + count + " files")
                .setAuthor(AUTHOR)
                .setCommitter(AUTHOR)
                .setSign(false)
                .call()
                .name();
    }

    void createBranchAndSwitch(String branch) throws GitAPIException {
        git.checkout().setCreateBranch(true).setName(branch).call();
    }

    void switchTo(String branch) throws GitAPIException {
        git.checkout().setName(branch).call();
    }

    @Override
    public void close() {
        git.close();
    }
}
