package org.jmouse.money.spring.autoconfigure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.jmouse.money.CurrencyCode;
import org.jmouse.money.ExchangeRates;
import org.jmouse.money.MoneyConverter;
import org.jmouse.money.jpa.ExchangeRateRegistry;
import org.jmouse.money.jpa.JpaExchangeRates;
import org.jmouse.money.spring.ExchangeRateService;
import org.jmouse.money.spring.MoneyProperties;
import org.jmouse.money.spring.provider.ExchangeRateProvider;
import org.jmouse.money.spring.provider.NbuExchangeRateProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * ⚙️ Everything a product needs to convert money, wired from four properties.
 *
 * <h3>⚠️ The product must scan this library's entities, and nothing here can do it for it</h3>
 *
 * <p>{@code @EntityScan} in the application class has to name {@code org.jmouse.money.jpa}, exactly as
 * it already names the access library's entity package. This module deliberately does <strong>not</strong>
 * register the package itself: contributing an {@code EntityScanPackages} bean would <em>replace</em> the
 * default scan of the application's own package for any product that has no explicit
 * {@code @EntityScan}, which trades one late failure for a much worse one.</p>
 *
 * <p>⚠️ Hibernate validates only what it was given, so a missing package starts cleanly and dies on the
 * first query — with a message about an unknown entity rather than about a scan.</p>
 *
 * <h3>⚠️ Nothing here calls a bank on its own</h3>
 *
 * <p>The provider is a bean; the <em>sync</em> is not scheduled unless {@code jmouse.money.sync-cron} is
 * set. A library that starts making outbound requests because it landed on somebody's classpath is a
 * library that surprises people, and a bank is the last place to be surprised by traffic.</p>
 */
@AutoConfiguration
@ConditionalOnClass({EntityManagerFactory.class, ExchangeRateRegistry.class})
@EnableConfigurationProperties(MoneyProperties.class)
public class MoneyAutoConfiguration {

    /**
     * ⚠️ {@code @PersistenceContext} rather than an {@code EntityManager} parameter, because Spring Boot
     * registers no {@code EntityManager} bean — only a factory. This is the transaction-aware proxy, and
     * it is what makes {@code ExchangeRateRegistry} join the caller's transaction instead of opening one.
     */
    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    @ConditionalOnMissingBean
    public ExchangeRateRegistry exchangeRateRegistry() {
        return new ExchangeRateRegistry(entityManager);
    }

    /**
     * 📈 The read seam, over the rows.
     *
     * <p>⚠️ The pivot comes from configuration and is read <strong>once, here</strong>. Reading it from
     * the table instead would let a stale row decide what every conversion means.</p>
     */
    @Bean
    @ConditionalOnMissingBean(ExchangeRates.class)
    public JpaExchangeRates jpaExchangeRates(ExchangeRateRegistry registry, MoneyProperties properties) {
        return new JpaExchangeRates(registry, CurrencyCode.required(properties.getPivot()));
    }

    @Bean
    @ConditionalOnMissingBean
    public MoneyConverter moneyConverter(ExchangeRates rates) {
        return new MoneyConverter(rates);
    }

    /**
     * 💱 The rules about what a rate row may become.
     *
     * <p>⚠️ Conditional on a provider being present. A product that only <em>converts</em> — reading rates
     * some other process wrote — gets the converter and no service, rather than a service that would throw
     * the moment anybody pressed Sync.</p>
     */
    @Bean
    @ConditionalOnBean(ExchangeRateProvider.class)
    @ConditionalOnMissingBean
    public ExchangeRateService exchangeRateService(ExchangeRateRegistry registry,
                                                   ExchangeRateProvider provider,
                                                   MoneyProperties properties) {
        return new ExchangeRateService(registry, provider, CurrencyCode.required(properties.getPivot()));
    }

    /**
     * 📡 The feeds this library ships.
     *
     * <p>⚠️ A nested {@code @Configuration} rather than {@code @ConditionalOnClass} on a method. A
     * condition placed beside a sibling bean does nothing — it silently KEEPS the bean — and this one has
     * to be genuinely absent when a product has no {@code RestClient} on its classpath.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestClient.class)
    public static class Providers {

        @Bean
        @ConditionalOnMissingBean(ExchangeRateProvider.class)
        public NbuExchangeRateProvider nbuExchangeRateProvider(RestClient.Builder restClientBuilder,
                                                               MoneyProperties properties) {
            return new NbuExchangeRateProvider(restClientBuilder, properties.getBaseUrl());
        }
    }
}
