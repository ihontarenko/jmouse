package org.jmouse.cad.plugin.nexar;

import org.jmouse.http.client.JdkHttpTransport;
import org.jmouse.cad.nexar.NexarParts;
import org.jmouse.cad.nexar.NexarSettings;
import org.jmouse.settings.ParameterValues;
import org.jmouse.settings.Settings;
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

    /** What Nexar is called through: it never follows a redirect, so its token stays with Nexar; closed with the server. */
    @Bean(destroyMethod = "close")
    public JdkHttpTransport nexarHttpTransport(NexarProperties properties) {
        return new JdkHttpTransport(properties.settings().timeout(), JdkHttpTransport.Redirects.NEVER);
    }

    /** The plugin over its client; how many parts a search asks for is its setting, read at each search. */
    @Bean
    public NexarPlugin cadNexarPlugin(NexarProperties properties, Settings settings, JdkHttpTransport nexarHttpTransport) {
        NexarSettings nexar = properties.settings();
        ParameterValues values = settings.parameters(NexarPlugin.ID, NexarPlugin.CONNECTION);
        ParameterValues searches = settings.parameters(NexarPlugin.ID, NexarPlugin.SEARCHES);
        NexarParts parts = new NexarParts(() -> new NexarSettings(values.text(NexarPlugin.CLIENT), values.secret(NexarPlugin.SECRET),
                nexar.tokenUrl(), nexar.apiUrl(), nexar.limit(), nexar.timeout()),
                () -> Math.toIntExact(searches.wholeNumber(NexarPlugin.LIMIT)), Clock.systemUTC(), nexarHttpTransport);
        return new NexarPlugin(parts, nexar.limit(), nexar.clientId(), nexar.clientSecret());
    }
}
