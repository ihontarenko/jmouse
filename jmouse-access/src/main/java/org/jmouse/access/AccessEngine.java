package org.jmouse.access;

import org.jmouse.access.axis.AccessAxisEvaluator;
import org.jmouse.access.spi.AccessTargetRegistry;
import org.jmouse.access.spi.PermissionRelations;
import org.jmouse.access.spi.ResourceRelation;
import org.jmouse.access.spi.ResourceRelationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The one call every authorization question goes through.
 *
 * <pre>AccessDecision decide(Subject subject, String permission, AccessTarget target)</pre>
 *
 * <p>Six independent mechanisms used to decide whether a request was allowed, none of them knew the
 * others existed, and nothing in the installation could answer "why can this person do this". The
 * value of routing them through here is not that the answers changed — at the point this class was
 * introduced not one of them did — but that from here on there is <strong>one place to read, one
 * place to instrument, and one place to ask a hypothetical question</strong>. The third is what
 * {@code /admin/access}'s <em>Simulate</em> view is, and it works only because it is the same code
 * path the real request takes.
 *
 * <p><strong>This class is a dispatcher and nothing else</strong> (spec risk 2). It holds the ordering
 * — and the ordering lives in {@link AxisCatalog}, not here — and it stops at the first refusal. A
 * sixth responsibility belongs in a sixth bean or nowhere; if this file ever grows a rule of its own,
 * that rule is an axis somebody did not want to name.
 *
 * <p>Every decision is observable. The debug line names subject, permission, target, axis and verdict,
 * and it is the same line the control room renders — a control room that computes its own answer is a
 * control room that lies on the day it matters.
 */
public class AccessEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessEngine.class);

    private final List<AccessAxisEvaluator> axes;
    private final AccessTargetRegistry      targets;
    private final VisibilityScopeResolver   visibilityScopes;
    private final EngineRefusals            refusals;

    /** Where a row with no place of its own borrows one. Empty in a build that declares no relation. */
    private final ResourceRelationRegistry  relations;

    /** Which permissions aim elsewhere. {@link PermissionRelations#none()} where the policy says none. */
    private final PermissionRelations       permissionRelations;

    public AccessEngine(
            List<AccessAxisEvaluator> axes,
            AxisCatalog               declaredAxes,
            AccessTargetRegistry      targets,
            VisibilityScopeResolver   visibilityScopes,
            EngineRefusals            refusals,
            ResourceRelationRegistry  relations,
            PermissionRelations       permissionRelations) {

        this.axes                = orderedByAxis(axes, declaredAxes);
        this.targets             = targets;
        this.visibilityScopes    = visibilityScopes;
        this.refusals            = refusals;
        this.relations           = relations;
        this.permissionRelations = permissionRelations;
    }

    /**
     * The engine as it was before {@code through} existed.
     *
     * <p>⚠️ Kept so that adding a widening feature does not force every product to wire two beans it has
     * no use for. A build with no relations behaves identically to one compiled before this — the
     * redirect lookup answers empty and {@code decideAbout} takes the path it always took.
     */
    public AccessEngine(
            List<AccessAxisEvaluator> axes,
            AxisCatalog               declaredAxes,
            AccessTargetRegistry      targets,
            VisibilityScopeResolver   visibilityScopes,
            EngineRefusals            refusals) {

        this(axes, declaredAxes, targets, visibilityScopes, refusals,
             new ResourceRelationRegistry(List.of()), PermissionRelations.none());
    }

    /**
     * May this subject do this, here?
     *
     * <p>The axes run in {@link AxisCatalog} order and the first refusal wins, so the verdict always
     * names the <em>outermost</em> reason. That matters: a person without the permission, in a
     * workspace whose plan does not include the module, should be told about the plan — telling them
     * to ask for a permission would send them to somebody who cannot help.
     */
    public AccessDecision decide(Subject subject, String permission, AccessTarget target) {
        AccessTarget aimedAt = target == null ? AccessTarget.installation() : target;

        for (AccessAxisEvaluator axis : axes) {
            AccessDecision decision = axis.evaluate(subject, permission, aimedAt);

            if (decision.refused()) {
                log(subject, permission, aimedAt, axis.axis(), decision);
                return decision;
            }
        }

        log(subject, permission, aimedAt, null, AccessDecision.allowed());
        return AccessDecision.allowed();
    }

    /** Whether this subject may do this, here — for call sites with nothing to say about why not. */
    public boolean permits(Subject subject, String permission, AccessTarget target) {
        return decide(subject, permission, target).granted();
    }

    /**
     * Whether this subject holds the permission over rows it does <em>not</em> own.
     *
     * <p>The question {@code form:write:any}, {@code form:listAll} and {@code entry:manage} used to be
     * separate permissions for. It is asked by dropping the owner from the target, which takes
     * {@code SELF} out of the covering chain and leaves only grants at a workspace, an organisation
     * or the installation — so "somebody else's" is one expression rather than a second catalogue
     * entry with a paragraph explaining how it differs from the first.
     */
    public boolean permitsBeyondOwnRows(Subject subject, String permission, AccessTarget target) {
        return permits(subject, permission, target.withOwner(null));
    }

    /**
     * May this subject do this to <em>that row</em>?
     *
     * <p>The question the six ownership guards each answered their own way. The feature says where
     * the row lives through its {@code AccessTargetResolver}; the engine reads that as a target and
     * asks the ordinary five axes, so "mine" is the {@code SELF} link of the covering chain rather
     * than an identifier comparison somebody remembered to write — and the agent rule applies without
     * any of them knowing about it.
     *
     * <p>An unknown identifier refuses with {@code NOT_FOUND_OR_HIDDEN} rather than passing as an
     * unscoped target. That distinction is the whole safety of this method: an unscoped target passes
     * every axis that is about a place, so resolving "no such row" to "no place" would turn a typo
     * into an open door.
     */
    public AccessDecision decideAbout(
            Subject subject, String permission, Class<?> resourceType, String resourceId) {

        return decideAbout(subject, permission, resourceType, resourceId, null);
    }

    /**
     * The same, for a caller that also knows which module the route belongs to.
     *
     * <p>⚠️ <strong>This overload is what {@code @RequiresAccess(resource = …)} goes through, and adding
     * it is what made {@code through} reach an endpoint at all.</strong> The enforcement guard used to
     * resolve the row itself and call {@link #decide} with the single target that came back — so a
     * permission's {@code through} clause was honoured by anything calling this method directly and
     * silently ignored by every annotated route, which is every route. The module is threaded rather than
     * applied by the caller because the redirect produces <em>several</em> targets and each of them needs
     * it; decorating one target before the redirect ran would have decorated the wrong one.
     *
     * @param module the module the route belongs to, or null where it declares none
     */
    public AccessDecision decideAbout(
            Subject subject, String permission, Class<?> resourceType, String resourceId, String module) {

        Optional<PermissionRelations.Redirect> redirect = permissionRelations.redirectFor(permission);

        if (redirect.isPresent()) {
            return decideThrough(subject, permission, resourceType, resourceId, redirect.get(), module);
        }

        return targets.resolve(resourceType, resourceId)
                .map(target -> decide(subject, permission, within(target, module)))
                .orElseGet(() -> AccessDecision.refused(refusals.noSuchRow(), noSuchRow(resourceType)));
    }

    private AccessTarget within(AccessTarget target, String module) {
        return module == null ? target : target.withModule(module);
    }

    /**
     * The same question, asked where the permission's {@code through} clause points.
     *
     * <p>⚠️ <strong>The permission does not change and the subject does not change — only the target
     * does.</strong> {@code field:write through form} still asks about {@code field:write}; it asks it
     * about the forms the field stands on, because a field has no place of its own to ask about. See
     * {@link ResourceRelation} for why this is the one thing in the model that can widen, and for the
     * two rules that keep it safe.
     *
     * <p>⚠️ <strong>Both empties refuse, and they refuse differently.</strong> No relation declared for
     * the pair is a <em>wiring</em> fault — the policy names a destination this build cannot reach — and
     * it says so, because a rule that silently does nothing is worse than one that fails loudly. A
     * relation answering with no rows is a row that hangs off nothing, and falls back to the installation
     * target — the narrowest place still reachable, which only a GLOBAL holding covers.
     */
    private AccessDecision decideThrough(
            Subject subject,
            String permission,
            Class<?> resourceType,
            String resourceId,
            PermissionRelations.Redirect redirect,
            String module) {

        Optional<List<AccessTarget>> borrowed =
                relations.targetsOf(resourceType, redirect.destination(), resourceId);

        if (borrowed.isEmpty()) {
            throw new IllegalStateException(
                    "'" + permission + "' is declared 'through " + redirect.destination().getSimpleName()
                    + "' but nothing relates " + resourceType.getSimpleName() + " to it. Register a "
                    + "ResourceRelation for that pair, or drop the clause — a permission whose target "
                    + "cannot be reached refuses every request and explains none of them.");
        }

        List<AccessTarget> places = borrowed.get();

        if (places.isEmpty()) {
            // A row related to nothing falls back to the INSTALLATION, not to a refusal. Refusing was the
            // first design and it was wrong: fifteen of Innoventa's 283 fields stand on no form, and under
            // that rule nobody — an administrator included — could ever have edited or deleted one, with
            // no way out of the state from inside the product.
            //
            // It grants nobody anything. Only a GLOBAL holding covers the installation target, and anybody
            // with installation-wide `field:write` could already edit every field there is; a holding at an
            // organisation, a workspace or SELF does not cover it. So the floor is the narrowest place
            // still reachable, which is what a row belonging nowhere deserves.
            return decide(subject, permission, within(AccessTarget.installation(), module));
        }

        // `through each X` needs every place to agree; `through any X` needs one. The quantifier is written
        // in the policy precisely so this line is not a default somebody has to go and look up.
        AccessDecision refusal = null;

        for (AccessTarget place : places) {
            AccessDecision decision = decide(subject, permission, within(place, module));

            if (decision.granted() && !redirect.requiresEach()) {
                return decision;
            }

            if (!decision.granted()) {
                if (redirect.requiresEach()) {
                    return decision;
                }

                refusal = refusal == null ? decision : refusal;
            }
        }

        // Every place allowed it (`all`), or none did (`any`) — in which case the first refusal is the
        // one to report, so the caller is told why rather than merely that.
        return refusal == null ? AccessDecision.allowed() : refusal;
    }

    private String noSuchRow(Class<?> resourceType) {
        return "No such " + targets.nameOf(resourceType).orElseGet(resourceType::getSimpleName) + ".";
    }

    /** {@link #decideAbout}, for call sites with nothing to say about why not. */
    public boolean permitsAbout(
            Subject subject, String permission, Class<?> resourceType, String resourceId) {

        return decideAbout(subject, permission, resourceType, resourceId).granted();
    }

    /**
     * Which rows this subject may see — the same answer as {@link #decide}, shaped as a filter.
     *
     * <p>{@code decide} settles whether one row may be touched; a listing needs the identical
     * question answered over rows that have not been read yet, and answering it row by row would be
     * both the N+1 and the wrong shape. So the engine hands out a {@link VisibilityScope} and the
     * repository composes it into its own query.
     *
     * <p><strong>Gate a listing on the narrow permission and <em>filter</em> by this.</strong> That
     * is ADR-0016's lesson kept: gating a listing on the wide answer is the bug that once refused a
     * reader the list of their own forms, and it is no longer expressible because there is no second
     * permission left to gate on.
     */
    public VisibilityScope visibilityFor(Subject subject, String permission) {
        return visibilityScopes.of(subject, permission);
    }

    /**
     * The axes that are present, in the order the {@link AxisCatalog} declares them.
     *
     * <p><strong>Registered rather than listed.</strong> An axis is a bean; the engine collects
     * whatever answers the interface and never names one. What it does not leave to discovery is the
     * <em>order</em> — that comes from the axis itself, stated once where it is declared, because the
     * value of a verdict naming an axis is that it names the outermost reason and an emergent
     * ordering would make that a coincidence.
     *
     * <p>Startup fails on two beans answering one axis, and on a <em>required</em> axis with none.
     * The optional ones are how a product's own questions about a place come and go: an installation
     * with no modules starts with two axes rather than refusing to, which is what makes the set a
     * registration and not a fixed five.
     *
     * <p>The catalog is read rather than the registrations, because an axis nothing answers is
     * exactly the case worth failing on and a set inferred from the beans present could never notice
     * it.
     */
    private static List<AccessAxisEvaluator> orderedByAxis(
            List<AccessAxisEvaluator> candidates, AxisCatalog declaredAxes) {

        Map<AxisKind, AccessAxisEvaluator> byAxis = new HashMap<>();

        for (AccessAxisEvaluator candidate : candidates) {
            AccessAxisEvaluator existing = byAxis.put(candidate.axis(), candidate);

            if (existing != null) {
                throw new IllegalStateException(
                        "Two beans answer the " + candidate.axis().name() + " axis: "
                        + existing.getClass().getName() + " and " + candidate.getClass().getName()
                        + ". An axis is one question with one answer.");
            }
        }

        for (AxisKind axis : declaredAxes.declared()) {
            if (axis.required() && !byAxis.containsKey(axis)) {
                throw new IllegalStateException(
                        "No bean answers the " + axis.name() + " axis, and it is not optional. A "
                        + "missing required axis is a question silently answered yes.");
            }
        }

        for (AccessAxisEvaluator candidate : candidates) {
            if (!declaredAxes.declared().contains(candidate.axis())) {
                throw new IllegalStateException(
                        candidate.getClass().getName() + " answers the " + candidate.axis().name()
                        + " axis, which this installation does not declare. An axis that runs "
                        + "without being declared has no place in the order, and the order is what "
                        + "makes a refusal name the outermost reason.");
            }
        }

        return candidates.stream()
                .sorted(Comparator.comparing(AccessAxisEvaluator::axis, declaredAxes.outermostFirst()))
                .toList();
    }

    private void log(
            Subject        subject,
            String         permission,
            AccessTarget   target,
            AxisKind       axis,
            AccessDecision decision) {

        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        LOGGER.debug("access: subject={} permission={} target=[{}] verdict={} axis={} reason={}",
                     subject.describe(),
                     permission,
                     target.describe(),
                     decision.granted() ? "granted" : "refused",
                     axis == null ? "-" : axis.name(),
                     decision.reason() == null ? "-" : decision.reason());
    }
}
