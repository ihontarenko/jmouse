package org.jmouse.java.index.jpa;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.flywaydb.core.Flyway;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.Statistics;
import org.jmouse.java.index.FileIndex;
import org.jmouse.java.index.MemberResolution;
import org.jmouse.java.index.MemberResolver;
import org.jmouse.java.index.Resolution;
import org.jmouse.java.index.TypeResolver;
import org.jmouse.java.index.jpa.migration.JavaIndexDialect;
import org.jmouse.java.index.jpa.migration.JavaIndexMigrations;
import org.jmouse.java.parser.JavaParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Opt-in real-engine contract. URLs/users/passwords come from test-only environment variables. */
@EnabledIfSystemProperty(named = "java.index.database.tests", matches = "true")
class JpaIndexIntegrationTest {
    private final JavaParser parser = new JavaParser();
    private final IndexConfiguration configuration =
            IndexConfiguration.current(JavaParser.Level.EXPRESSIONS);

    @Test
    void postgresql() throws Exception {
        verify(JavaIndexDialect.POSTGRESQL, "POSTGRESQL");
    }

    @Test
    void mysql() throws Exception {
        verify(JavaIndexDialect.MYSQL, "MYSQL");
    }

    private void verify(JavaIndexDialect dialect, String engine) throws Exception {
        String url = required(engine, "URL"),
                user = required(engine, "USER"),
                password = required(engine, "PASSWORD");
        Flyway flyway =
                Flyway.configure()
                        .dataSource(url, user, password)
                        .table(JavaIndexMigrations.HISTORY_TABLE)
                        .locations(JavaIndexMigrations.locationFor(dialect))
                        .load();
        flyway.migrate();
        assertEquals(0, flyway.migrate().migrationsExecuted);
        flyway.validate();
        Configuration hibernate = new Configuration();
        for (Class<?> entity :
                List.of(
                        IndexScopeRow.class,
                        ContributionRow.class,
                        ScopeEntryRow.class,
                        DeclarationRow.class,
                        ReferenceRow.class)) {
            hibernate.addAnnotatedClass(entity);
        }
        hibernate.setProperty("hibernate.connection.url", url);
        hibernate.setProperty("hibernate.connection.username", user);
        hibernate.setProperty("hibernate.connection.password", password);
        hibernate.setProperty("hibernate.hbm2ddl.auto", "validate");
        hibernate.setProperty("hibernate.generate_statistics", "true");
        hibernate.setProperty("hibernate.connection.pool_size", "5");
        hibernate.setProperty("hibernate.show_sql", "false");
        hibernate.setProperty("hibernate.jdbc.batch_size", "50");
        try (var factory = hibernate.buildSessionFactory()) {
            FileIndex target =
                    file(
                            "module-a/Target.java",
                            "package shared; public class Target { public void run(int value) {}"
                                    + " public void run(String value) {} }");
            FileIndex alias = target.withSource("module-b/Target.java");
            FileIndex consumer =
                    file(
                            "Consumer.java",
                            "package client; import shared.*; class Consumer { Target target; }");
            String scope;
            long started = System.nanoTime();
            try (EntityManager manager = factory.createEntityManager()) {
                manager.getTransaction().begin();
                scope = new JpaIndexScopes(manager).create(configuration);
                JpaIndexStore store = new JpaIndexStore(manager, scope, configuration);
                store.write(target, hash(target));
                store.write(alias, hash(target));
                store.write(consumer, hash(consumer));
                assertEquals(
                        store.contributionId(target.source()),
                        store.contributionId(alias.source()));
                long revision = store.revision();
                store.write(target, hash(target));
                assertEquals(
                        revision,
                        store.revision(),
                        "An identical replay does not change membership.");
                assertEquals(3, revision);
                manager.getTransaction().commit();
            }
            try (EntityManager manager = factory.createEntityManager()) {
                JpaIndexStore store = new JpaIndexStore(manager, scope, configuration);
                assertEquals(target, store.read(target.source()).orElseThrow());
                assertEquals(alias, store.read(alias.source()).orElseThrow());
                assertEquals(3, store.catalog().size());
                assertEquals(List.of("Consumer.java"), store.referencingSources("Target", 10));
                var result =
                        new TypeResolver(store).resolve(consumer, consumer.references().getFirst());
                assertEquals(2, ((Resolution.Ambiguous) result).candidates().size());
                manager.getTransaction().begin();
                store.remove(alias.source());
                assertInstanceOf(
                        Resolution.Resolved.class,
                        new TypeResolver(store)
                                .resolve(consumer, consumer.references().getFirst()));
                manager.getTransaction().rollback();
            }
            try (EntityManager manager = factory.createEntityManager()) {
                JpaIndexStore store = new JpaIndexStore(manager, scope, configuration);
                assertTrue(
                        store.read(alias.source()).isPresent(),
                        "Rollback restores scope membership.");
                assertEquals(3, store.revision());
                manager.getTransaction().begin();
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                store.write(
                                        file(target.source(), "package shared; class Different {}"),
                                        hash(target)));
                manager.getTransaction().rollback();
                manager.clear();
                manager.getTransaction().begin();
                String contribution = store.contributionId(target.source()).orElseThrow();
                manager.createNativeQuery(
                                "UPDATE java_index_contributions SET payload = :payload WHERE id ="
                                        + " :id")
                        .setParameter("payload", new byte[] {1, 2, 3})
                        .setParameter("id", contribution)
                        .executeUpdate();
                manager.clear();
                assertThrows(
                        java.io.UncheckedIOException.class,
                        () ->
                                new JpaIndexStore(manager, scope, configuration)
                                        .read(target.source()));
                manager.getTransaction().rollback();
                manager.clear();
                manager.getTransaction().begin();
                store.remove(alias.source());
                FileIndex caller =
                        file(
                                "Caller.java",
                                "package client; import shared.Target; class Caller { void"
                                    + " use(Target target) { target.run(1); target.run(\"text\"); }"
                                    + " }");
                store.write(caller, hash(caller));
                var calls =
                        caller.bodies().stream()
                                .flatMap(body -> body.references().stream())
                                .filter(reference -> reference.name().equals("run"))
                                .toList();
                var targets =
                        calls.stream()
                                .map(
                                        reference ->
                                                ((MemberResolution.Resolved)
                                                                new MemberResolver(store)
                                                                        .resolve(caller, reference))
                                                        .target()
                                                        .symbolId()
                                                        .value())
                                .toList();
                assertEquals(
                        List.of("shared.Target#run(int)", "shared.Target#run(String)"), targets);
                assertEquals(5, store.revision());
                new JpaIndexScopes(manager).seal(scope);
                manager.getTransaction().commit();
            }
            try (EntityManager manager = factory.createEntityManager()) {
                JpaIndexStore store = new JpaIndexStore(manager, scope, configuration);
                manager.getTransaction().begin();
                assertThrows(IllegalStateException.class, () -> store.remove(target.source()));
                manager.getTransaction().rollback();
                store.prefetch(List.of(target.source(), "Caller.java"));
                store.findTypes("shared.Target");
                Statistics statistics = factory.getStatistics();
                long statements = statistics.getPrepareStatementCount();
                for (int number = 0; number < 10; number++) {
                    assertTrue(store.read(target.source()).isPresent());
                    assertEquals(1, store.findTypes("shared.Target").size());
                }
                assertEquals(
                        statements,
                        statistics.getPrepareStatementCount(),
                        "Prefetched sealed payloads do not cause N+1 reads.");
                manager.getTransaction().begin();
                String copy = new JpaIndexScopes(manager).copy(scope, configuration);
                JpaIndexStore copied = new JpaIndexStore(manager, copy, configuration);
                assertEquals(
                        store.contributionId(target.source()),
                        copied.contributionId(target.source()));
                copied.remove(target.source());
                assertTrue(store.read(target.source()).isPresent());
                assertTrue(copied.read(target.source()).isEmpty());
                manager.getTransaction().commit();
            }
            concurrency(factory);
            inheritance(factory);
            if (System.getProperty("java.index.corpus") != null) {
                corpusBenchmark(factory, engine);
            }
            System.out.printf(
                    "%s JPA contract passed:"
                        + " write/read/restart/rollback/dedup/overloads/ambiguity/snapshot/isolation/concurrency;"
                        + " elapsedMs=%d%n",
                    engine, (System.nanoTime() - started) / 1_000_000);
        }
    }

    private void inheritance(EntityManagerFactory factory) {
        try (EntityManager manager = factory.createEntityManager()) {
            manager.getTransaction().begin();
            String scope = new JpaIndexScopes(manager).create(configuration);
            JpaIndexStore store = new JpaIndexStore(manager, scope, configuration);
            FileIndex base =
                    file(
                            "Base.java",
                            "package shared; public class Base { public void inherited(int value)"
                                    + " {} }");
            FileIndex child =
                    file(
                            "Child.java",
                            "package client; public class Child extends shared.Base {} ");
            FileIndex caller =
                    file(
                            "Caller.java",
                            "package client; class Caller { void use(Child child) {"
                                    + " child.inherited(1); } }");
            for (FileIndex file : List.of(base, child, caller)) {
                store.write(file, hash(file));
            }
            var reference =
                    caller.bodies().getFirst().references().stream()
                            .filter(item -> item.name().equals("inherited"))
                            .findFirst()
                            .orElseThrow();
            var resolution =
                    (MemberResolution.Resolved)
                            new MemberResolver(store).resolve(caller, reference);
            assertEquals("shared.Base#inherited(int)", resolution.target().symbolId().value());
            manager.getTransaction().rollback();
            manager.clear();
            assertTrue(
                    manager.createQuery(
                                    "SELECT scope FROM IndexScopeRow scope WHERE scope.id = :id",
                                    IndexScopeRow.class)
                            .setParameter("id", scope)
                            .getResultList()
                            .isEmpty());
        }
    }

    private void corpusBenchmark(org.hibernate.SessionFactory factory, String engine)
            throws Exception {
        java.nio.file.Path root = java.nio.file.Path.of(System.getProperty("java.index.corpus"));
        record Input(FileIndex file, String contentHash) {}
        java.util.ArrayList<Input> inputs = new java.util.ArrayList<>();
        try (var paths = java.nio.file.Files.walk(root)) {
            for (var path :
                    paths.filter(value -> value.toString().endsWith(".java")).sorted().toList()) {
                String text = java.nio.file.Files.readString(path);
                inputs.add(
                        new Input(
                                file(root.relativize(path).toString().replace('\\', '/'), text),
                                IndexIdentities.hash(text)));
            }
        }
        Statistics statistics = factory.getStatistics();
        statistics.clear();
        long started = System.nanoTime();
        String scope;
        try (EntityManager manager = factory.createEntityManager()) {
            manager.getTransaction().begin();
            scope = new JpaIndexScopes(manager).create(configuration);
            JpaIndexStore store = new JpaIndexStore(manager, scope, configuration);
            for (Input input : inputs) {
                store.write(input.file(), input.contentHash());
            }
            new JpaIndexScopes(manager).seal(scope);
            manager.getTransaction().commit();
        }
        long writeMilliseconds = (System.nanoTime() - started) / 1_000_000;
        long writeStatements = statistics.getPrepareStatementCount();
        statistics.clear();
        started = System.nanoTime();
        try (EntityManager manager = factory.createEntityManager()) {
            JpaIndexStore store = new JpaIndexStore(manager, scope, configuration);
            assertEquals(inputs.size(), store.catalog().size());
            long restoreMilliseconds = (System.nanoTime() - started) / 1_000_000;
            long catalogStatements = statistics.getPrepareStatementCount();
            started = System.nanoTime();
            List<String> candidates = store.referencingSources("String", 10000);
            long candidateMicroseconds = (System.nanoTime() - started) / 1000;
            store.prefetch(inputs.stream().map(input -> input.file().source()).toList());
            var chosen =
                    inputs.stream()
                            .flatMap(
                                    input ->
                                            input.file().bodies().stream()
                                                    .filter(
                                                            body ->
                                                                    input.file()
                                                                            .isBodyComplete(
                                                                                    body.owner()))
                                                    .flatMap(body -> body.references().stream())
                                                    .filter(
                                                            reference ->
                                                                    reference.role()
                                                                            == org.jmouse.java
                                                                                    .parser.model
                                                                                    .BodyReference
                                                                                    .Role.CALLS)
                                                    .map(
                                                            reference ->
                                                                    new java.util.AbstractMap
                                                                            .SimpleImmutableEntry<>(
                                                                            input.file(),
                                                                            reference)))
                            .limit(100)
                            .toList();
            statistics.clear();
            started = System.nanoTime();
            MemberResolver resolver = new MemberResolver(store);
            int resolved = 0;
            for (var call : chosen) {
                if (resolver.resolve(call.getKey(), call.getValue())
                        instanceof MemberResolution.Resolved) {
                    resolved++;
                }
            }
            long resolutionMicroseconds = (System.nanoTime() - started) / 1000;
            long resolutionStatements = statistics.getPrepareStatementCount();
            Number bytes =
                    (Number)
                            manager.createNativeQuery(
                                            "SELECT sum(octet_length(payload)) FROM"
                                                + " java_index_contributions WHERE id IN (SELECT"
                                                + " contribution_id FROM java_index_scope_entries"
                                                + " WHERE scope_id = :scope)")
                                    .setParameter("scope", scope)
                                    .getSingleResult();
            System.out.printf(
                    "BENCHMARK %s files=%d writeMs=%d writeStatements=%d catalogMs=%d"
                            + " catalogStatements=%d candidates=%d candidateMicros=%d calls=%d"
                            + " resolved=%d resolutionMicros=%d resolutionStatements=%d"
                            + " payloadBytes=%d%n",
                    engine,
                    inputs.size(),
                    writeMilliseconds,
                    writeStatements,
                    restoreMilliseconds,
                    catalogStatements,
                    candidates.size(),
                    candidateMicroseconds,
                    chosen.size(),
                    resolved,
                    resolutionMicroseconds,
                    resolutionStatements,
                    bytes.longValue());
        }
    }

    private void concurrency(EntityManagerFactory factory) throws Exception {
        String scope;
        try (EntityManager manager = factory.createEntityManager()) {
            manager.getTransaction().begin();
            scope = new JpaIndexScopes(manager).create(configuration);
            manager.getTransaction().commit();
        }
        CountDownLatch lockHeld = new CountDownLatch(1), releaseWriter = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var writer =
                    workers.submit(
                            () -> {
                                try (EntityManager manager = factory.createEntityManager()) {
                                    manager.getTransaction().begin();
                                    new JpaIndexStore(manager, scope, configuration)
                                            .write(
                                                    file("Race.java", "class Race {}"),
                                                    IndexIdentities.hash("class Race {}"));
                                    lockHeld.countDown();
                                    if (!releaseWriter.await(10, TimeUnit.SECONDS)) {
                                        throw new AssertionError("Writer release timed out.");
                                    }
                                    manager.getTransaction().commit();
                                }
                                return null;
                            });
            assertTrue(lockHeld.await(10, TimeUnit.SECONDS));
            var sealer =
                    workers.submit(
                            () -> {
                                try (EntityManager manager = factory.createEntityManager()) {
                                    manager.getTransaction().begin();
                                    new JpaIndexScopes(manager).seal(scope);
                                    manager.getTransaction().commit();
                                }
                            });
            releaseWriter.countDown();
            writer.get(15, TimeUnit.SECONDS);
            sealer.get(15, TimeUnit.SECONDS);
        }
        try (EntityManager manager = factory.createEntityManager()) {
            JpaIndexStore store = new JpaIndexStore(manager, scope, configuration);
            assertTrue(store.read("Race.java").isPresent());
            assertEquals(2, store.revision());
            manager.getTransaction().begin();
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            store.write(
                                    file("Late.java", "class Late {}"),
                                    IndexIdentities.hash("class Late {}")));
            manager.getTransaction().rollback();
        }
    }

    private FileIndex file(String source, String text) {
        return FileIndex.of(parser.parse(source, text, JavaParser.Level.EXPRESSIONS));
    }

    private String hash(FileIndex file) {
        return IndexIdentities.hash(file.withSource("").toString());
    }

    private String required(String engine, String name) {
        String value = System.getenv("JAVA_INDEX_TEST_" + engine + "_" + name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing test connection setting for " + engine + ": " + name);
        }
        return value;
    }
}
