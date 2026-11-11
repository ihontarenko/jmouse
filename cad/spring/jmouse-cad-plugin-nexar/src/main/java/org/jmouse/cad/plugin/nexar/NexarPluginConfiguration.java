package org.jmouse.cad.plugin.nexar;

import org.jmouse.cad.nexar.NexarParts;
import org.jmouse.cad.nexar.NexarSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.Duration;

/** 🧩 The Nexar plugin, on the classpath: its plugin bean over the client the server is given. */
@AutoConfiguration
@EnableConfigurationProperties(NexarPluginConfiguration.NexarProperties.class)
public class NexarPluginConfiguration {

    /**
     * {@code jmouse.cad.parts.nexar.*}: Nexar's client; without one the place is listed as not configured.
     *
     * @see NexarSettings for what each means
     */
    @ConfigurationProperties("jmouse.cad.parts.nexar")
    public record NexarProperties(String clientId, String clientSecret, String tokenUrl, String apiUrl, int limit,
                                  Duration timeout) {

        NexarSettings settings() {
            return new NexarSettings(clientId, clientSecret, tokenUrl, apiUrl, limit, timeout);
        }
    }

    @Bean
    public NexarPlugin cadNexarPlugin(NexarProperties properties) {
        return new NexarPlugin(new NexarParts(properties.settings(), Clock.systemUTC()));
    }
}
