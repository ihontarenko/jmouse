package org.jmouse.telegram.spring.autoconfigure;

import jakarta.persistence.EntityManagerFactory;
import org.jmouse.telegram.journal.TelegramJournal;
import org.jmouse.telegram.jpa.JpaTelegramJournal;
import org.jmouse.telegram.spring.TelegramJournalRetention;
import org.jmouse.telegram.spring.TelegramSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * ⚙️ The journal in {@code telegram_journal}, when {@code jmouse.telegram.journal.store=database}.
 *
 * <p>Independent of the credential key {@link TelegramJpaAutoConfiguration} needs: an application whose
 * identities live in properties may still want its journal in the database.
 */
@AutoConfiguration(before = TelegramAutoConfiguration.class)
@ConditionalOnClass({EntityManagerFactory.class, JpaTelegramJournal.class})
@ConditionalOnProperty(name = "jmouse.telegram.journal.store", havingValue = "database")
public class TelegramJournalJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TelegramJournal.class)
    public JpaTelegramJournal telegramDatabaseJournal(EntityManagerFactory entityManagerFactory) {
        return new JpaTelegramJournal(entityManagerFactory);
    }

    /** ⚙️ Prunes the database journal hourly to {@code jmouse.telegram.journal.retention}. */
    @Bean(initMethod = "start")
    @ConditionalOnMissingBean
    public TelegramJournalRetention telegramJournalRetention(TelegramJournal journal, TelegramSettings settings) {
        return new TelegramJournalRetention(journal, settings.journal().retention());
    }
}
