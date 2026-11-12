package org.jmouse.cad.spring.autoconfigure;

import org.jmouse.cad.exchange.AcceptedFiles;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.spring.PluginAutoConfiguration;
import org.jmouse.storage.configuration.StorageSettings;
import org.jmouse.storage.policy.UploadPolicy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 🛃 What the installation accepts as a file: its own settings, and every format the running plugins read or keep —
 * every import's file names ({@link DocumentImport#fileNames}) and every {@link AcceptedFiles}. A format whose plugin is
 * off is not accepted, because nothing here knows it; the settings name only what is the installation's own (archives,
 * pictures, the bytes a browser cannot name).
 *
 * <p>Registered before the storage's own, whose policy is made only where none is: so this one is the installation's,
 * and the folders' rules and the managed files are judged by it.</p>
 */
@AutoConfiguration(after = PluginAutoConfiguration.class,
                   beforeName = "org.jmouse.storage.spring.autoconfigure.StorageAutoConfiguration")
@ConditionalOnClass(name = "org.jmouse.storage.policy.UploadPolicy")
public class CadUploadAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UploadPolicy cadUploadPolicy(StorageSettings settings, Extensions extensions) {
        AcceptedFiles.Accepted accepted = AcceptedFiles.of(extensions.contributions(DocumentImport.POINT),
                extensions.contributions(AcceptedFiles.POINT));
        return UploadPolicy.of(settings).accepting(accepted.mediaTypes(), accepted.extensions());
    }
}
