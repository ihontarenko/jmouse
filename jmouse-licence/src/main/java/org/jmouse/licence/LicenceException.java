package org.jmouse.licence;

import java.util.Set;

/**
 * What an exception to a licence lifts: the conditions it waives, and the uses it waives them for.
 *
 * @param spdx   its SPDX identifier: {@code KiCad-libraries-exception}
 * @param waives the conditions it lifts
 * @param within the uses it lifts them for; any other keeps the licence's every condition
 */
public record LicenceException(String spdx, String title, Set<LicenceCondition> waives, Set<LicenceUse> within) {

    public LicenceException {
        waives = Set.copyOf(waives);
        within = Set.copyOf(within);
    }

    /** The conditions it lifts for a use: none for a use it does not name. */
    public Set<LicenceCondition> waivedFor(LicenceUse use) {
        return within.contains(use) ? waives : Set.of();
    }
}
