package org.jmouse.access.spring.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.jpa.AccessAdministration;
import org.jmouse.access.jpa.AccessDisclosure;
import org.jmouse.access.jpa.DeclaredRoleBundles;
import org.jmouse.access.jpa.EntitlementAdministration;
import org.jmouse.access.jpa.JpaAccessAdministration;
import org.jmouse.access.jpa.JpaAccessDisclosure;
import org.jmouse.access.jpa.JpaEntitlementAdministration;
import org.jmouse.access.jpa.JpaEntitlementStore;
import org.jmouse.access.jpa.JpaGrantStore;
import org.jmouse.access.jpa.JpaPolicyRevisions;
import org.jmouse.access.jpa.PolicyRevisions;
import org.jmouse.access.spi.EntitlementStore;
import org.jmouse.access.spi.GrantStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.annotation.Transactional;

/**
 * The engine's storage, wired.
 *
 * <p>Two stores, two administration ports and one disclosure, and a product that keeps its grants in
 * these tables now writes none of them. Before this existed, adopting the engine's schema still meant
 * writing the stores — which is how both of them came to live in a product, reading tables the library
 * owned.
 *
 * <h2>⚠️ Everything is {@code @ConditionalOnMissingBean}</h2>
 *
 * <p>A product that keeps grants somewhere else entirely — an LDAP directory, another service — replaces
 * a bean and loses nothing. That is the same promise the policy half already makes, and it is what keeps
 * "the library owns the tables" from becoming "the library owns where your grants live".
 */
/*
 * ⚠️ Ordered after whichever autoconfiguration registers the EntityManager, and it will not start
 * without that line. Every bean below takes one, and an autoconfiguration that ran first would fail
 * with "no qualifying bean of type EntityManager" — during Tomcat startup, several frames deep,
 * naming the product's own filter rather than this class.
 *
 * Named rather than referenced, because the class that provides it is a different one in Boot 3 and
 * in Boot 4 and this module compiles against neither: a product on either gets its ordering, and one
 * on a third gets no ordering constraint rather than a NoClassDefFoundError.
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
@ConditionalOnClass({EntityManager.class, JpaGrantStore.class})
public class AccessJpaAutoConfiguration {

    /**
     * The rows, on their own — <strong>not</strong> the composite the engine resolves from.
     *
     * <p>⚠️ Named because a screen that explains authorization has to be able to ask each source
     * separately: <em>what do the tables say</em> and <em>what does the answer come to</em> are the two
     * halves of the only question such a screen exists to answer, and injecting {@link GrantStore}
     * plainly gets the {@code @Primary} composite — which is the second one.
     */
    public static final String STORED_GRANTS = "jpaGrantStore";

    /**
     * @param declared ⚠️ Defaults to none rather than being required. A product with no policy
     *                 documents has every bundle in the table, and asking it to register an empty seam
     *                 to say so is the sort of tax that makes a library feel heavy.
     */
    @Bean(STORED_GRANTS)
    @ConditionalOnMissingBean
    public GrantStore jpaGrantStore(
            EntityManagerFactory entityManagers,
            ScopeCatalog scopes,
            ObjectProvider<DeclaredRoleBundles> declared) {

        return new JpaGrantStore(sharedEntityManager(entityManagers), scopes, declared.getIfAvailable(DeclaredRoleBundles::none));
    }

    @Bean
    @ConditionalOnMissingBean
    public EntitlementStore jpaEntitlementStore(EntityManagerFactory entityManagers, ScopeCatalog scopes) {
        return new JpaEntitlementStore(sharedEntityManager(entityManagers), scopes);
    }

    /**
     * Changing authorization.
     *
     * <p>⚠️ {@code @Transactional} here rather than inside the implementation: whether a change is one
     * transaction or part of a larger one is the <em>application's</em> decision, and an implementation
     * that opened its own would make "assign three roles and record one audit event" impossible to write
     * atomically.
     */
    @Bean
    @ConditionalOnMissingBean
    @Transactional
    public AccessAdministration accessAdministration(
            EntityManagerFactory entityManagers, ObjectProvider<DeclaredRoleBundles> declared) {

        return new JpaAccessAdministration(
                sharedEntityManager(entityManagers), declared.getIfAvailable(DeclaredRoleBundles::none));
    }

    /**
     * Changing what a place is entitled to, and which of its capabilities are switched on.
     *
     * <p>The entitlement axis's counterpart to {@link #accessAdministration}, and it is here for the
     * same reason: {@code EntitlementStore} is read-only because <em>issuing</em> is a governed act,
     * which never justified the product owning {@code @Entity} classes on these tables.
     */
    @Bean
    @ConditionalOnMissingBean
    @Transactional
    public EntitlementAdministration entitlementAdministration(
            EntityManagerFactory entityManagers, ScopeCatalog scopes) {

        return new JpaEntitlementAdministration(sharedEntityManager(entityManagers), scopes);
    }

    /**
     * Who holds what, installation-wide.
     *
     * <p>⚠️ <strong>Registered as {@link AccessDisclosure} and never as anything the engine takes.</strong>
     * Nothing on the decision path injects this type, and that is the only thing keeping the promise
     * that the engine cannot walk a store — a product exposing it gates it and records the reading.
     */
    @Bean
    @ConditionalOnMissingBean
    @Transactional(readOnly = true)
    public AccessDisclosure accessDisclosure(
            EntityManagerFactory entityManagers,
            ScopeCatalog scopes,
            ObjectProvider<DeclaredRoleBundles> declared) {

        return new JpaAccessDisclosure(
                sharedEntityManager(entityManagers), scopes, declared.getIfAvailable(DeclaredRoleBundles::none));
    }

    /**
     * The editable policy's history.
     *
     * <p>⚠️ Registered only where the policy half is on the classpath is <em>not</em> a condition this
     * carries, deliberately: the table exists in this module's migration whether or not anybody edits
     * a document, and a product that adopts the editor later should not have to discover a bean it was
     * never told about.
     */
    @Bean
    @ConditionalOnMissingBean
    @Transactional
    public PolicyRevisions policyRevisions(EntityManagerFactory entityManagers) {
        return new JpaPolicyRevisions(sharedEntityManager(entityManagers));
    }

    /**
     * ⚠️ <strong>A factory is taken and a shared manager made from it, because Spring Boot never
     * registers a plain {@link EntityManager} bean.</strong>
     *
     * <p>Asking for one directly compiles, and fails at startup with <em>"no qualifying bean of type
     * EntityManager"</em> — several frames into Tomcat's boot, naming the adopting product's own
     * security filter rather than this class. Ordering the autoconfiguration later does not help:
     * there is no bean to wait for.
     *
     * <p>The shared manager is the right thing to hand a store in any case. It is a proxy resolving to
     * whatever manager the <em>current transaction</em> is using, so a store reading inside one sees
     * that transaction's own writes — which is what makes "assign a role, then read it back in the
     * same method" behave the way anybody would expect.
     */
    private static EntityManager sharedEntityManager(EntityManagerFactory entityManagers) {
        return SharedEntityManagerCreator.createSharedEntityManager(entityManagers);
    }
}
