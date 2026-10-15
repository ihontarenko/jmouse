package org.jmouse.spice.node;

/**
 * Sees each kind of {@link SpiceNode} by its type: {@code node.accept(visitor)} calls the method for the node's kind.
 * A method for every kind, none defaulted, so a visitor that compiles has decided what each kind means to it.
 *
 * @param <R> what visiting a node gives
 */
public interface SpiceVisitor<R> {

    R visitDeck(SpiceDeck deck);

    R visitPassive(SpiceElement.Passive passive);

    R visitSource(SpiceElement.Source source);

    R visitDiode(SpiceElement.Diode diode);

    R visitBipolar(SpiceElement.Bipolar bipolar);

    R visitMosfet(SpiceElement.Mosfet mosfet);

    R visitInstance(SpiceElement.Instance instance);

    R visitBehavioural(SpiceElement.Behavioural behavioural);

    R visitControlled(SpiceElement.Controlled controlled);

    R visitCoupling(SpiceElement.Coupling coupling);

    R visitGenericElement(SpiceElement.Generic element);

    R visitModel(SpiceCard.Model model);

    R visitSubcircuit(SpiceCard.Subcircuit subcircuit);

    R visitParameters(SpiceCard.Parameters parameters);

    R visitInclude(SpiceCard.Include include);

    R visitTransient(SpiceCard.Transient analysis);

    R visitAc(SpiceCard.Ac analysis);

    R visitDc(SpiceCard.Dc analysis);

    R visitOperatingPoint(SpiceCard.OperatingPoint analysis);

    R visitControl(SpiceCard.Control control);

    R visitDirective(SpiceCard.Directive directive);

    R visitEnd(SpiceCard.End end);
}
