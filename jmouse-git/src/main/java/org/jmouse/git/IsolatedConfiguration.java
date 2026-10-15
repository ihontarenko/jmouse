package org.jmouse.git;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Keeps the machine's git configuration — {@code ~/.gitconfig}, the XDG file, the system file and JGit's own — out
 * of every repository this library opens, so {@code url.insteadOf}, {@code http.extraHeader},
 * {@code http.sslVerify} and the rest cannot redirect or weaken a transfer.
 *
 * <p>JGit reads that configuration through one JVM-wide {@link SystemReader}, and a repository on disk takes it as
 * its base when it is opened; there is no per-repository switch. So a delegating reader is installed once, wrapping
 * whatever reader is in place. It changes nothing for any thread outside {@link #run(Supplier)} and answers empty
 * configuration for a thread inside it. If somebody installs another reader later, the next call wraps that one in
 * turn.
 */
final class IsolatedConfiguration {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private IsolatedConfiguration() {
    }

    /** Runs an operation with the machine's git configuration hidden from this thread. */
    static <T> T run(Supplier<T> operation) {
        install();
        boolean outermost = !ACTIVE.get();
        ACTIVE.set(Boolean.TRUE);
        try {
            return operation.get();
        } finally {
            if (outermost) {
                ACTIVE.remove();
            }
        }
    }

    private static synchronized void install() {
        SystemReader current = SystemReader.getInstance();
        if (!(current instanceof IsolatingSystemReader)) {
            SystemReader.setInstance(new IsolatingSystemReader(current));
        }
    }

    /** Delegates everything, except that configuration is empty for a thread inside {@link #run(Supplier)}. */
    private static final class IsolatingSystemReader extends SystemReader.Delegate {

        private final SystemReader delegate;

        private IsolatingSystemReader(SystemReader delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        @Override
        public FileBasedConfig openUserConfig(Config parent, FS fileSystem) {
            return ACTIVE.get() ? new EmptyConfiguration(parent, fileSystem) : delegate.openUserConfig(parent, fileSystem);
        }

        @Override
        public FileBasedConfig openSystemConfig(Config parent, FS fileSystem) {
            return ACTIVE.get() ? new EmptyConfiguration(parent, fileSystem) : delegate.openSystemConfig(parent, fileSystem);
        }

        @Override
        public FileBasedConfig openJGitConfig(Config parent, FS fileSystem) {
            return ACTIVE.get() ? new EmptyConfiguration(parent, fileSystem) : delegate.openJGitConfig(parent, fileSystem);
        }

        @Override
        public StoredConfig getUserConfig() throws ConfigInvalidException, IOException {
            return ACTIVE.get() ? new EmptyConfiguration(null, FS.DETECTED) : delegate.getUserConfig();
        }

        @Override
        public StoredConfig getSystemConfig() throws ConfigInvalidException, IOException {
            return ACTIVE.get() ? new EmptyConfiguration(null, FS.DETECTED) : delegate.getSystemConfig();
        }

        @Override
        public StoredConfig getJGitConfig() throws ConfigInvalidException, IOException {
            return ACTIVE.get() ? new EmptyConfiguration(null, FS.DETECTED) : delegate.getJGitConfig();
        }
    }

    /** A configuration with no file behind it: nothing to load, nothing to save, never outdated. */
    private static final class EmptyConfiguration extends FileBasedConfig {

        private EmptyConfiguration(Config parent, FS fileSystem) {
            super(parent, null, fileSystem);
        }

        @Override
        public void load() {
        }

        @Override
        public void save() {
        }

        @Override
        public boolean isOutdated() {
            return false;
        }

        @Override
        public String toString() {
            return "EmptyConfiguration";
        }
    }
}
