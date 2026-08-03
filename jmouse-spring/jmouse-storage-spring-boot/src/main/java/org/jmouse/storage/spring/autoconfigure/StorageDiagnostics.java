package org.jmouse.storage.spring.autoconfigure;

import org.jmouse.storage.FileStores;
import org.jmouse.storage.configuration.StorageSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 🔎 Says out loud, once, what storage actually came up as.
 *
 * <p>Not decoration. Boot 4 split autoconfiguration into per-technology modules, and when a
 * third-party integration fails to register the failure mode is <em>silence</em> — no error, no
 * warning, just a context with no storage beans in it and a {@code NoSuchBeanDefinitionException}
 * somewhere further along that names something else entirely. One line at startup turns "did the
 * autoconfiguration run?" from a debugging session into a look at the log.</p>
 *
 * <p>It also prints what would otherwise have to be inferred: which backends exist, which is the
 * default, whether callers may choose, and which key layout is in force.</p>
 */
public class StorageDiagnostics {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageDiagnostics.class);

    /**
     * 🏗️ Report, on construction, what storage came up as.
     *
     * <p>In the constructor rather than behind a lifecycle annotation so that the report needs no
     * annotation library on the classpath and cannot be skipped by a context that processes
     * lifecycle callbacks differently.</p>
     *
     * @param fileStores the backends
     * @param settings   the active settings
     */
    public StorageDiagnostics(FileStores fileStores, StorageSettings settings) {
        LOGGER.info("🗄️ jMouse storage ready — backends {}, default '{}', backend choice {}, "
                            + "keys {}, max upload {} bytes, sweeper {}",
                    fileStores.backendNames(),
                    fileStores.defaultBackendName(),
                    fileStores.isChoiceExposed() ? "exposed" : "hidden",
                    settings.contentAddressedKeys() ? "content-addressed" : "owner-namespaced",
                    settings.maxSizeBytes(),
                    settings.sweeper().enabled() ? "enabled" : "disabled");
    }
}
