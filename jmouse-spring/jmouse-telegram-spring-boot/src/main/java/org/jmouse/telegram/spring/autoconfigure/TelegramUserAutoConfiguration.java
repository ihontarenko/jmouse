package org.jmouse.telegram.spring.autoconfigure;

import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.user.ChannelReader;
import org.jmouse.telegram.user.UserAccounts;
import org.jmouse.telegram.user.UserAdministration;
import org.jmouse.telegram.user.UserClientFactory;
import org.jmouse.telegram.user.UserSessions;
import org.jmouse.telegram.user.UserTransport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

/**
 * ⚙️ Acting as a person — when {@code jmouse-telegram-user} is on the classpath <strong>and</strong> the
 * product declared a {@link UserClientFactory} over its MTProto library.
 *
 * <pre>{@code
 * jmouse:
 *   telegram:
 *     user:
 *       sessions: /var/lib/product/telegram-sessions   # where each person's session directory goes
 *       session-key: ${TELEGRAM_SESSION_KEY}           # ⚠️ base64, 32 bytes — from the environment ONLY
 *     identities:
 *       ivan-reader:
 *         kind: user
 *         token: ivan                                  # a USER identity's credential is a SESSION NAME
 * }</pre>
 *
 * <p>The {@link UserTransport} joins the gateway's transports, so {@code gateway.as("ivan-reader")} sends as
 * the person; {@link UserAdministration} lets {@code ChatAdministration.createGroup} work for that purpose;
 * {@link ChannelReader} reads history and joins channels.
 */
@AutoConfiguration(before = TelegramAutoConfiguration.class)
@ConditionalOnClass(UserTransport.class)
@ConditionalOnBean(UserClientFactory.class)
public class TelegramUserAutoConfiguration {

    /** {@code jmouse.telegram.user.*}. */
    public record UserSettings(Path sessions, String sessionKey) {
    }

    @Bean
    @ConditionalOnMissingBean
    public UserSessions telegramUserSessions(Environment environment) {
        UserSettings settings = Binder.get(environment)
                                      .bind("jmouse.telegram.user", UserSettings.class)
                                      .orElse(new UserSettings(null, null));

        if (settings.sessions() == null) {
            throw new IllegalStateException("A UserClientFactory is declared but jmouse.telegram.user.sessions is not: "
                                            + "name the directory each person's session is kept in");
        }

        return UserSessions.fromBase64Key(settings.sessions(), settings.sessionKey());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public UserAccounts telegramUserAccounts(UserSessions sessions, UserClientFactory factory) {
        return new UserAccounts(sessions, factory);
    }

    @Bean
    @ConditionalOnMissingBean
    public UserTransport telegramUserTransport(UserAccounts accounts) {
        return new UserTransport(accounts);
    }

    @Bean
    @ConditionalOnMissingBean
    public UserAdministration telegramUserAdministration(UserAccounts accounts) {
        return new UserAdministration(accounts);
    }

    @Bean
    @ConditionalOnMissingBean
    public ChannelReader telegramChannelReader(IdentitySource identities, UserAccounts accounts) {
        return new ChannelReader(identities, accounts);
    }
}
