package org.jmouse.cad.spice;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What an element card of SPICE becomes in jMX ({@link SpiceDevices}): {@code R} a resistor, {@code V … SIN(…)} a sine
 * generator.
 *
 * @param card      the card's letter: {@code R}, {@code V}, {@code E}
 * @param signal    the signal a source card writes — {@code SIN}, {@code PULSE} —, or empty for a constant one and for
 *                  any other card
 * @param component the component it becomes, by slug
 * @param pins      the component's pins, in the order SPICE writes the card's nodes
 * @param value     what the card's one value sets, or empty for a card whose signal sets several
 * @param settings  what each argument of its signal sets, by the argument's name
 */
public record ElementCard(String id, String card, Optional<String> signal, String component, List<String> pins,
                           Optional<String> value, Map<String, String> settings) {

    public ElementCard {
        pins = List.copyOf(pins);
        settings = Map.copyOf(settings);
    }

    /**
     * What an argument of its signal sets.
     *
     * @throws IllegalStateException when the table gives that argument no setting
     */
    public String setting(String argument) {
        String setting = settings.get(argument);
        if (setting == null) {
            throw new IllegalStateException("The SPICE element '" + id + "' says nothing its " + argument + " sets");
        }
        return setting;
    }

    /**
     * What its value sets.
     *
     * @throws IllegalStateException for an element whose signal sets several
     */
    public String valueSetting() {
        return value.orElseThrow(() -> new IllegalStateException("The SPICE element '" + id + "' has no one value"));
    }
}
