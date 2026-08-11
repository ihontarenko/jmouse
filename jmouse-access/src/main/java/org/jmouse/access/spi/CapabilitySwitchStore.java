package org.jmouse.access.spi;

import org.jmouse.access.ScopeReference;

import java.util.Set;

/**
 * What a place has switched <strong>off</strong> — the seam for the axis that asks whether somewhere
 * wants a capability, as opposed to whether it may have one.
 *
 * <h2>⚠️ A switch is not a grant, and must not be one</h2>
 *
 * <p>The two look alike and answer opposite questions. A grant says what a place <em>may</em> have and
 * is issued by whoever decides that; a switch says what it <em>wants</em> and is flipped by whoever
 * works there. Forcing a switch into {@link CapabilityGrant} would hand it an allowance, a validity
 * window and a provenance it has no use for — and would put an ordinary settings-screen write behind
 * an interface that is read-only precisely because issuing a grant is a governed act.
 *
 * <p>Keeping them apart is also what makes the refusals different, which is the whole value of the
 * axis order: <em>"your plan does not include this"</em> is something to buy, and <em>"somebody here
 * turned this off"</em> is the one refusal on the list a reader can undo themselves.
 *
 * <h2>⚠️ It names what is off, never what is on</h2>
 *
 * <p>Default-on is the engine's assumption and this store lists the exceptions. A store answering
 * <em>"what is enabled here"</em> would have to know the catalogue to be complete, and would answer
 * wrongly for every capability introduced after its rows were written — a feature shipped next year
 * would arrive switched off everywhere, silently, with nobody having decided that.
 *
 * <h2>One question, because that is all the axis asks</h2>
 *
 * <p>A switch is a property of the <em>place</em>, never of the person: a module cannot differ between
 * two people in one workspace. So there is no subject here, and no covering chain — a place answers
 * for itself, and the engine does not walk upwards looking for somewhere that turned something off on
 * its behalf.
 */
@FunctionalInterface
public interface CapabilitySwitchStore {

    /**
     * The capabilities this place has switched off.
     *
     * @param place where the question is being asked
     * @return their keys, or an empty set — never null
     */
    Set<String> switchedOffAt(ScopeReference place);

    /** A store where nothing is ever switched off — what a product registering none gets. */
    static CapabilitySwitchStore allOn() {
        return place -> Set.of();
    }
}
