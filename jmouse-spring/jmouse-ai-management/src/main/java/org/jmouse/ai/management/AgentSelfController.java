package org.jmouse.ai.management;

import org.jmouse.ai.agent.Agent;
import org.jmouse.ai.agent.AgentConnection;
import org.jmouse.ai.agent.AgentConnections;
import org.jmouse.ai.agent.AgentDirectory;
import org.jmouse.ai.agent.AgentGrants;
import org.jmouse.ai.agent.AgentOwners;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The same agents screen, for somebody looking at their own.
 *
 * <h2>Why this is a second controller and not a query parameter</h2>
 *
 * <p>{@link AgentAdministrationController} already answers <em>the agents one person owns</em>. It cannot
 * be the personal screen, because the owner arrives in the path: a route that takes an owner is a route
 * whoever can reach it may point at anybody, which is exactly right behind {@code ai:administer} and
 * exactly wrong in front of an ordinary person.
 *
 * <p>So the difference between the two is one sentence — <strong>here the owner comes from the security
 * context and every route re-checks it</strong> — and a product mounts them behind different
 * permissions. Everything else is the same ports, the same records, the same screen.
 *
 * <p>⚠️ <strong>The ownership check is on every route, including the reads.</strong> Fetching somebody
 * else's grants discloses their owner's whole effective permission set, so a read here is not the cheap
 * case it looks like.
 *
 * <p>⚠️ <strong>There is no create route here either, for the reason there is none next door.</strong>
 * Where an agent comes into existence by a client connecting, an agent created in advance is a row with
 * nothing behind it; where it does not, creating one needs choices this module cannot carry. A product
 * with something to offer keeps its own create route and its own screen for it.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX + "/my-agents")
public class AgentSelfController {

    private final AgentDirectory   agents;
    private final AgentConnections connections;
    private final AgentGrants      grants;
    private final AgentOwners      owners;

    public AgentSelfController(
            AgentDirectory agents,
            AgentConnections connections,
            AgentGrants grants,
            AgentOwners owners) {

        this.agents      = agents;
        this.connections = connections;
        this.grants      = grants;
        this.owners      = owners;
    }

    /** Everything the caller owns. The administration screen's view, so one component renders both. */
    @GetMapping
    public List<AgentAdministrationController.AgentView> mine() {
        return agents.ownedBy(me()).stream()
                .map(this::describe)
                .toList();
    }

    @PatchMapping("/{agentId}/name")
    public AgentAdministrationController.AgentView rename(
            @PathVariable String agentId,
            @RequestBody AgentAdministrationController.RenameRequest request) {

        return describe(agents.rename(mine(agentId).id(), request.name()));
    }

    @PatchMapping("/{agentId}/enabled")
    public AgentAdministrationController.AgentView enabled(
            @PathVariable String agentId, @RequestParam boolean enabled) {

        Agent agent = mine(agentId);

        return describe(enabled
                ? agents.putInService(agent.id())
                : agents.takeOutOfService(agent.id()));
    }

    /**
     * ⚠️ <strong>Widening one's own agent back to inherited authority is allowed, and is not an
     * escalation.</strong> An inherited agent acts with its owner's permissions and never more, so this
     * hands it exactly what the person calling already holds — which they could equally do by using the
     * product themselves.
     */
    @PatchMapping("/{agentId}/authority")
    public AgentAdministrationController.AgentView authority(
            @PathVariable String agentId,
            @RequestBody AgentAdministrationController.AuthorityRequest request) {

        return describe(agents.actWith(mine(agentId).id(), request.authority()));
    }

    @GetMapping("/{agentId}/grants")
    public AgentAdministrationController.GrantsView grants(@PathVariable String agentId) {
        String id = mine(agentId).id();

        return new AgentAdministrationController.GrantsView(grants.heldBy(id), grants.offerFor(id));
    }

    /**
     * ⚠️ Named for the owner, and the owner is the only name it can carry. The administration route takes
     * the principal because an administrator is somebody else; here the two are the same person by
     * construction, which is the property this whole controller exists to have.
     */
    @PutMapping("/{agentId}/grants")
    public AgentAdministrationController.GrantsView replaceGrants(
            @PathVariable String agentId,
            @RequestBody AgentAdministrationController.GrantsRequest request) {

        String owner = me();
        String id    = mine(agentId, owner).id();

        AgentGrants.Held held = grants.replace(
                id,
                request.permissions() == null ? List.of() : request.permissions(),
                request.placements()  == null ? List.of() : request.placements(),
                owner);

        return new AgentAdministrationController.GrantsView(held, grants.offerFor(id));
    }

    /** Ends one of the caller's own clients. The agent and its other clients carry on. */
    @DeleteMapping("/{agentId}/connections/{connectionId}")
    public AgentAdministrationController.AgentView revoke(
            @PathVariable String agentId, @PathVariable String connectionId) {

        Agent agent = mine(agentId);

        // ⚠️ Checked against THIS agent rather than only against the owner. Two of somebody's own agents
        // are still two agents, and disconnecting a client from the wrong one is a bug they would report
        // as the client having stopped working.
        connections.of(agent.id()).stream()
                .filter(connection -> connection.id().equals(connectionId))
                .findFirst()
                .orElseThrow(() -> new AgentDirectory.RefusedException(
                        "That client is not connected to this agent."));

        connections.revoke(connectionId);

        return describe(agent);
    }

    @DeleteMapping("/{agentId}/connections")
    public AgentAdministrationController.AgentView revokeAll(@PathVariable String agentId) {
        Agent agent = mine(agentId);

        connections.revokeAllOf(agent.id());

        return describe(agent);
    }

    /**
     * Discards one of the caller's own. ⚠️ Refused, with a sentence, where a product cannot do it.
     *
     * <p>⚠️ <strong>Grants first, and the order is the whole point.</strong> They live in the product's
     * authorization store with no foreign key into this library's tables, so nothing cascades and nothing
     * warns — an agent discarded without {@link AgentGrants#revokeAllOf} leaves its grants behind,
     * quietly resolving for whatever identifier is issued next. Doing it first also means a failure here
     * leaves the agent in place, which is the recoverable half of the two.
     */
    @DeleteMapping("/{agentId}")
    public void discard(@PathVariable String agentId) {
        String id = mine(agentId).id();

        grants.revokeAllOf(id);
        agents.discard(id);
    }

    private String me() {
        return owners.current().orElseThrow(() -> new AgentDirectory.RefusedException(
                "Nobody is signed in, so there are no agents to show. Sign in and try again."));
    }

    private Agent mine(String agentId) {
        return mine(agentId, me());
    }

    /**
     * ⚠️ <strong>Refused with the same sentence whether the agent belongs to somebody else or does not
     * exist.</strong> Telling the two apart turns this route into a way of asking whether a given
     * identifier is an agent, which is a disclosure the administration screen has a permission for.
     */
    private Agent mine(String agentId, String owner) {
        return agents.find(agentId)
                .filter(agent -> owner.equals(agent.ownerReference()))
                .orElseThrow(() -> new AgentDirectory.RefusedException(
                        "You have no agent with id '" + agentId + "'."));
    }

    private AgentAdministrationController.AgentView describe(Agent agent) {
        List<AgentConnection> held = connections.of(agent.id());

        return new AgentAdministrationController.AgentView(
                agent.id(),
                agent.ownerReference(),
                agent.name(),
                agent.authority(),
                agent.enabled(),
                agent.createdAt(),
                agent.lastActiveAt(),
                (int) held.stream().filter(connection -> !connection.isRevoked()).count(),
                held);
    }
}
