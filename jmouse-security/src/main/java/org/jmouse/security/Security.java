package org.jmouse.security;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.pipeline.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 🧱 Security built from four staged pipelines: Ingress → Identity → Policy → Egress.
 */
public class Security {

    private final Ingress  ingress;
    private final Identity identity;
    private final Policy   policy;
    private final Egress   egress;

    private Security(Ingress ingress, Identity identity, Policy policy, Egress egress) {
        this.ingress = ingress;
        this.identity = identity;
        this.policy = policy;
        this.egress = egress;
    }

    /**
     * ▶️ Evaluate envelope across full flow.
     */
    public Decision evaluate(Envelope envelope) {
        List<Pipeline<?, ? extends Link<?, ?, ?>>> pipelines = List.of(ingress, identity, policy, egress);

        for (Pipeline<?, ?> pipeline : pipelines) {
            if (pipeline.proceed(envelope) instanceof Outcome.Done<Decision>(Decision decision)) {
                if (Decision.isTerminal(decision)) {
                    return decision;
                }
            }
        }

        return Decision.permit(null);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final IngressBuilder  ingress  = new IngressBuilder();
        private final IdentityBuilder identity = new IdentityBuilder();
        private final PolicyBuilder   policy   = new PolicyBuilder();
        private final EgressBuilder   egress   = new EgressBuilder();

        public Builder ingress(Consumer<IngressBuilder> consumer) {
            consumer.accept(ingress);
            return this;
        }

        public Builder identity(Consumer<IdentityBuilder> consumer) {
            consumer.accept(identity);
            return this;
        }

        public Builder policy(Consumer<PolicyBuilder> consumer) {
            consumer.accept(policy);
            return this;
        }

        public Builder egress(Consumer<EgressBuilder> consumer) {
            consumer.accept(egress);
            return this;
        }

        public Security build() {
            return new Security(
                    ingress.build(),
                    identity.build(),
                    policy.build(),
                    egress.build()
            );
        }
    }

    public static final class IngressBuilder {

        private final List<IngressLink> links = new ArrayList<>();

        public IngressBuilder use(IngressLink link) {
            links.add(link);
            return this;
        }

        Ingress build() {
            Chain<Ingress, Envelope, Decision> chain = Chain.of(links, false)
                    .withFallback((ingress, envelope) -> Decision.defer(DecisionMessages.MESSAGE_DEFER));
            return new Ingress(chain);
        }

    }

    public static final class IdentityBuilder {

        private final List<Authenticator> authenticators = new ArrayList<>();

        public IdentityBuilder use(Authenticator authenticator) {
            authenticators.add(authenticator);
            return this;
        }

        public Identity build() {
            IdentityLink                        link  = new AuthenticationLink(List.copyOf(authenticators));
            Chain<Identity, Envelope, Decision> chain = Chain.of(List.of(link), false)
                    .withFallback((identity, envelope) -> Decision.defer(DecisionMessages.MESSAGE_DEFER));
            return new Identity(chain);
        }

    }

    public static final class PolicyBuilder {

        private final List<Authorizer> authorizers = new ArrayList<>();

        public PolicyBuilder use(Authorizer authorizer) {
            authorizers.add(authorizer);
            return this;
        }

        public Policy build() {
            PolicyLink                        link     = new AuthorizationLink(List.copyOf(authorizers));
            Decision                          decision = Decision.deny(DecisionCodes.NO_POLICY,
                                                                       DecisionMessages.MESSAGE_NO_POLICY);
            Chain<Policy, Envelope, Decision> chain    = Chain.of(List.of(link), false)
                    .withFallback((policy, envelope) -> decision);
            return new Policy(chain);
        }

    }

    public static final class EgressBuilder {

        private final List<EgressLink> links = new ArrayList<>();

        public EgressBuilder use(EgressLink link) {
            links.add(link);
            return this;
        }

        public Egress build() {
            Chain<Egress, Envelope, Decision> chain = Chain.of(links, false)
                    .withFallback((egress, envelope) -> Decision.defer(DecisionMessages.MESSAGE_DEFER));
            return new Egress(chain);
        }

    }

}
