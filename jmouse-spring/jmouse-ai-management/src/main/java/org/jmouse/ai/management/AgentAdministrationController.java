package org.jmouse.ai.management;

import org.jmouse.ai.agent.Agent;
import org.jmouse.ai.agent.AgentAuthority;
import org.jmouse.ai.agent.AgentConnection;
import org.jmouse.ai.agent.AgentConnections;
import org.jmouse.ai.agent.AgentDirectory;
import org.jmouse.ai.agent.AgentGrants;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

/**
 * Every agent this installation has, what each may do, and which clients are holding a credential for
 * one.
 *
 * <p><strong>One screen for products that store an agent completely differently.</strong> One keeps a
 * row in this library's own table; another has an account with a ceiling, a storage allowance and space
 * memberships. Neither has to give in, because both answer {@link AgentDirectory} — which is why this
 * controller reads a port and never a table, and why a product migrating from one arrangement to the
 * other changes nothing here.
 *
 * <p>⚠️ <strong>Nothing in it is guarded, exactly like the five controllers beside it.</strong> A
 * library's handler cannot carry an authorization declaration, so a product states the requirement
 * <em>about</em> these types instead. This one discloses who has connected what across the whole
 * installation and can switch an agent off, so a product that mounts it behind nothing has published
 * its agents and handed out their switches. See this module's {@code package-info}.
 *
 * <p>⚠️ <strong>Not every product can answer every write.</strong> Creating an agent, discarding one,
 * or making one act with its owner's authority can each be more than a given product's arrangement
 * allows, and the port refuses those with a sentence saying why. This controller therefore has no
 * create route at all — where agents are created by connecting a client there is nothing to create,
 * and where they are not, creation needs choices this module cannot carry.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX + "/agents")
public class AgentAdministrationController {

    private final AgentDirectory   agents;
    private final AgentConnections connections;
    private final AgentGrants      grants;

    public AgentAdministrationController(
            AgentDirectory agents, AgentConnections connections, AgentGrants grants) {

        this.agents      = agents;
        this.connections = connections;
        this.grants      = grants;
    }

    /**
     * One agent with the clients connected to it.
     *
     * <p>Together rather than as two calls, because the only questions anybody asks of this screen —
     * <em>is this still in use</em> and <em>which of these can I safely end</em> — need both halves at
     * once, and fetching connections per row afterwards would be a request per agent.
     *
     * @param connectionCount ⚠️ counted separately from the list because the list includes revoked ones
     *                        — a screen shows history — and "3 clients" must not count endings
     */
    public record AgentView(
            String                id,
            String                ownerReference,
            String                name,
            AgentAuthority        authority,
            boolean               enabled,
            Instant               createdAt,
            Instant               lastActiveAt,
            int                   connectionCount,
            List<AgentConnection> connections
    ) {
    }

    /** What a rename carries. */
    public record RenameRequest(String name) {
    }

    /** What a change of authority carries. */
    public record AuthorityRequest(AgentAuthority authority) {
    }

    /**
     * Every agent in the installation, newest first.
     *
     * <p>Bounded here rather than by the caller: a listing with no ceiling is a table scan anybody
     * reaching the route can ask for.
     */
    @GetMapping
    public List<AgentView> agents(@RequestParam(defaultValue = "0") int limit) {
        return agents.all(ManagementRoutes.boundedLimit(limit)).stream()
                .map(this::describe)
                .toList();
    }

    /** The agents one person owns — the same view, narrowed to an owner. */
    @GetMapping("/owned-by/{ownerReference}")
    public List<AgentView> ownedBy(@PathVariable String ownerReference) {
        return agents.ownedBy(ownerReference).stream()
                .map(this::describe)
                .toList();
    }

    @PatchMapping("/{agentId}/name")
    public AgentView rename(@PathVariable String agentId, @RequestBody RenameRequest request) {
        return describe(agents.rename(agentId, request.name()));
    }

    /**
     * Switches one on or off.
     *
     * <p>⚠️ It does not touch the connections, and that is the point: whoever pulls this wants the agent
     * stopped, not a decision about which clients come back afterwards. Ending the clients as well is
     * revoking them, one at a time, which is a separate act because it is a separate intention.
     */
    @PatchMapping("/{agentId}/enabled")
    public AgentView enabled(@PathVariable String agentId, @RequestParam boolean enabled) {
        return describe(enabled ? agents.putInService(agentId) : agents.takeOutOfService(agentId));
    }

    /**
     * Changes whose permissions it acts with.
     *
     * <p>⚠️ <strong>Restricting takes effect on the next call, and an agent that has been granted
     * nothing can then do nothing.</strong> That is the honest behaviour — restricting is an act, and
     * one that silently left everything permitted would be worse — which means a screen offering this
     * has to say so before the click rather than after it.
     */
    @PatchMapping("/{agentId}/authority")
    public AgentView authority(@PathVariable String agentId, @RequestBody AuthorityRequest request) {
        return describe(agents.actWith(agentId, request.authority()));
    }

    /** What an agent holds, beside what its owner could hand it. */
    public record GrantsView(AgentGrants.Held held, AgentGrants.Offer offer) {
    }

    /** What a save carries — the whole set, never a delta. */
    public record GrantsRequest(List<String> permissions, List<AgentGrants.Placement> placements) {
    }

    /**
     * ⚠️ <strong>Read together, because a screen showing one without the other teaches the wrong
     * rule.</strong> An agent's set is intersected with its owner's on every request, so a permission
     * granted here that the owner does not hold resolves to nothing — and looks, from the outside,
     * exactly like the agent being broken. The offer is what stops somebody granting into a void.
     */
    @GetMapping("/{agentId}/grants")
    public GrantsView grants(@PathVariable String agentId) {
        return new GrantsView(grants.heldBy(agentId), grants.offerFor(agentId));
    }

    /**
     * Sets what an agent holds.
     *
     * <p>⚠️ Named for the person who pressed the button rather than the agent, because a grant's
     * {@code granted_by} is the only thing that answers <em>who decided this</em> a year later.
     */
    @PutMapping("/{agentId}/grants")
    public GrantsView replaceGrants(
            @PathVariable String agentId,
            @RequestBody GrantsRequest request,
            Principal grantedBy) {

        AgentGrants.Held held = grants.replace(
                agentId,
                request.permissions() == null ? List.of() : request.permissions(),
                request.placements()  == null ? List.of() : request.placements(),
                grantedBy == null ? null : grantedBy.getName());

        return new GrantsView(held, grants.offerFor(agentId));
    }

    /** Ends one client. The agent and its other clients carry on. */
    @DeleteMapping("/{agentId}/connections/{connectionId}")
    public AgentView revoke(@PathVariable String agentId, @PathVariable String connectionId) {
        connections.revoke(connectionId);

        return describe(require(agentId));
    }

    /** Ends every client of one agent at once, without switching the agent off. */
    @DeleteMapping("/{agentId}/connections")
    public AgentView revokeAll(@PathVariable String agentId) {
        connections.revokeAllOf(agentId);

        return describe(require(agentId));
    }

    private Agent require(String agentId) {
        return agents.find(agentId).orElseThrow(() -> new AgentDirectory.RefusedException(
                "No agent with id '" + agentId + "' exists."));
    }

    private AgentView describe(Agent agent) {
        List<AgentConnection> held = connections.of(agent.id());

        return new AgentView(
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
