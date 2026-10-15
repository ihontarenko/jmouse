package org.jmouse.telegram.spring.autoconfigure;

import jakarta.persistence.EntityManager;
import org.jmouse.telegram.jpa.JpaTelegramBindings;
import org.jmouse.telegram.jpa.TelegramBindings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * ⚙️ Which chat reaches which of the product's subjects — whenever {@code jmouse-telegram-jpa} and JPA are
 * present.
 *
 * <p>⚠️ <strong>Not gated on the credential key.</strong> A binding holds no secret. It used to sit behind
 * the old credential key with the accounts, which forced a product keeping its tokens in
 * properties to set a key — and setting one swapped its identities for the (empty) accounts table.
 *
 * <p>⚠️ The {@code /start} handler is deliberately <strong>not</strong> registered for you; one line where
 * the product can see it:
 *
 * <pre>{@code
 * dispatcher.onCommand("start", new BindingCommand(bindings, gateway));
 * }</pre>
 */
@AutoConfiguration(before = TelegramAutoConfiguration.class)
@ConditionalOnClass({EntityManager.class, JpaTelegramBindings.class})
public class TelegramBindingsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TelegramBindings.class)
    public JpaTelegramBindings telegramBindings(EntityManager entityManager) {
        return new JpaTelegramBindings(entityManager);
    }
}
