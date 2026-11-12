package org.jmouse.licence;

/**
 * Where a licence's or an exception's own text is read.
 *
 * @param spdx its SPDX identifier
 * @param home the address of its text
 */
public record LicenceHome(String spdx, String home) {
}
