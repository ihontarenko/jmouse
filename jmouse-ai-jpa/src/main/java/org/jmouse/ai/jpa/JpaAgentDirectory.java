package org.jmouse.ai.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.ai.agent.Agent;
import org.jmouse.ai.agent.AgentAuthority;
import org.jmouse.ai.agent.AgentDirectory;
import org.jmouse.ai.jpa.entity.AiAgent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Agents, in {@code ai_agents}.
 *
 * <h2>⚠️ Its own transaction, and what that costs you</h2>
 *
 * <p>Like everything else in this module, each call commits on its own rather than joining whoever
 * called — the module holds Jakarta Persistence and no transaction manager, on purpose. For counters
 * that is the point; here it has a consequence worth stating out loud, because it will otherwise be
 * discovered:
 *
 * <p><strong>Creating an agent and granting it privileges cannot be one transaction.</strong> The
 * privileges live in the product's access store and the agent lives here, so the two were never going to
 * be atomic whatever this class did. What follows is a small ordering rule with a real failure behind it:
 * register with {@link Draft#enabled()} {@code false}, grant, then {@link #putInService(String)}. An
 * agent that exists and is enabled before its ceiling has been written is, for that window, an agent
 * whose effective permissions are its owner's entire set.
 *
 * <h2>What a duplicate name means</h2>
 *
 * <p>Checked here rather than left to the unique constraint, because the constraint's own message names a
 * column and an index and the person reading it named an agent. The check and the constraint are both
 * present deliberately: the first is the sentence, the second is the guarantee.
 */
public final class JpaAgentDirectory implements AgentDirectory {

    private final EntityManagerFactory entityManagerFactory;

    public JpaAgentDirectory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public Agent register(Draft draft) {
        String name = requireName(draft.name());

        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            refuseDuplicateName(entityManager, draft.ownerReference(), name, null);

            AiAgent created = new AiAgent(
                    UUID.randomUUID().toString(),
                    draft.ownerReference(),
                    name,
                    draft.authority(),
                    draft.enabled(),
                    Instant.now());

            entityManager.persist(created);

            return describe(created);
        });
    }

    @Override
    public Optional<Agent> find(String agentId) {
        return OwnTransaction.call(entityManagerFactory,
                entityManager -> row(entityManager, agentId).map(JpaAgentDirectory::describe));
    }

    @Override
    public List<Agent> ownedBy(String ownerReference) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> entityManager.createQuery("""
                                select agent
                                  from AiAgent agent
                                 where agent.ownerReference = :ownerReference
                                 order by agent.createdAt desc
                                """, AiAgent.class)
                        .setParameter("ownerReference", ownerReference)
                        .getResultList())
                .stream()
                .map(JpaAgentDirectory::describe)
                .toList();
    }

    @Override
    public List<Agent> all(int limit) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> entityManager.createQuery("""
                                select agent
                                  from AiAgent agent
                                 order by agent.createdAt desc
                                """, AiAgent.class)
                        .setMaxResults(limit)
                        .getResultList())
                .stream()
                .map(JpaAgentDirectory::describe)
                .toList();
    }

    @Override
    public Agent rename(String agentId, String name) {
        String wanted = requireName(name);

        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiAgent agent = require(entityManager, agentId);

            refuseDuplicateName(entityManager, agent.getOwnerReference(), wanted, agentId);
            agent.setName(wanted);

            return describe(agent);
        });
    }

    @Override
    public Agent actWith(String agentId, AgentAuthority authority) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiAgent agent = require(entityManager, agentId);
            agent.setAuthority(authority);

            return describe(agent);
        });
    }

    @Override
    public Agent putInService(String agentId) {
        return inService(agentId, true);
    }

    @Override
    public Agent takeOutOfService(String agentId) {
        return inService(agentId, false);
    }

    /**
     * ⚠️ Deletes the connections explicitly, even though the foreign key already cascades.
     *
     * <p>Not belt and braces: the persistence context does not learn about rows the database removed
     * underneath it, so a connection loaded earlier in the same unit of work would survive as a managed
     * entity and could be flushed back. Doing it here also means the two stores agree whatever the
     * product's schema validator was told.
     */
    @Override
    public void discard(String agentId) {
        OwnTransaction.run(entityManagerFactory, entityManager -> {
            AiAgent agent = require(entityManager, agentId);

            deleteConnectionsOf(entityManager, agentId);
            entityManager.remove(agent);
        });
    }

    @Override
    public void discardAllOwnedBy(String ownerReference) {
        OwnTransaction.run(entityManagerFactory, entityManager -> entityManager.createQuery("""
                        select agent.id
                          from AiAgent agent
                         where agent.ownerReference = :ownerReference
                        """, String.class)
                .setParameter("ownerReference", ownerReference)
                .getResultList()
                .forEach(agentId -> {
                    deleteConnectionsOf(entityManager, agentId);
                    entityManager.remove(entityManager.getReference(AiAgent.class, agentId));
                }));
    }

    /**
     * ⚠️ Silent about an agent that no longer exists.
     *
     * <p>Every other method here refuses one, and this one must not: it runs after work that already
     * succeeded, so turning a completed call into a failure because the bookkeeping missed is the wrong
     * trade in the only direction that matters.
     */
    @Override
    public void stampActive(String agentId, Instant when) {
        OwnTransaction.run(entityManagerFactory, entityManager -> entityManager.createQuery("""
                        update AiAgent agent
                           set agent.lastActiveAt = :when
                         where agent.id = :agentId
                        """)
                .setParameter("when", when)
                .setParameter("agentId", agentId)
                .executeUpdate());
    }

    private Agent inService(String agentId, boolean enabled) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiAgent agent = require(entityManager, agentId);
            agent.setEnabled(enabled);

            return describe(agent);
        });
    }

    private void deleteConnectionsOf(EntityManager entityManager, String agentId) {
        entityManager.createQuery("""
                        delete from AiAgentConnection connection
                         where connection.agentId = :agentId
                        """)
                .setParameter("agentId", agentId)
                .executeUpdate();
    }

    private void refuseDuplicateName(
            EntityManager entityManager, String ownerReference, String name, String exceptAgentId) {

        boolean taken = !entityManager.createQuery("""
                        select agent.id
                          from AiAgent agent
                         where agent.ownerReference = :ownerReference
                           and agent.name = :name
                           and (:exceptAgentId is null or agent.id <> :exceptAgentId)
                        """, String.class)
                .setParameter("ownerReference", ownerReference)
                .setParameter("name", name)
                .setParameter("exceptAgentId", exceptAgentId)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();

        if (taken) {
            throw new RefusedException(
                    "An agent called '" + name + "' already exists under this owner. A name is how the "
                    + "two would be told apart afterwards — on a screen, in a log line, and on every "
                    + "record either of them creates — so pick a different one.");
        }
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new RefusedException(
                    "An agent needs a name. It is what a person picks it out by later, and what every "
                    + "record it creates will be labelled with.");
        }

        return name.trim();
    }

    private AiAgent require(EntityManager entityManager, String agentId) {
        return row(entityManager, agentId).orElseThrow(() -> new RefusedException(
                "No agent with id '" + agentId + "' exists. It may have been discarded, in which case "
                + "every connection under it went with it."));
    }

    private Optional<AiAgent> row(EntityManager entityManager, String agentId) {
        return Optional.ofNullable(entityManager.find(AiAgent.class, agentId));
    }

    private static Agent describe(AiAgent agent) {
        return new Agent(
                agent.getId(),
                agent.getOwnerReference(),
                agent.getName(),
                agent.getAuthority(),
                agent.isEnabled(),
                agent.getCreatedAt(),
                agent.getLastActiveAt());
    }
}
