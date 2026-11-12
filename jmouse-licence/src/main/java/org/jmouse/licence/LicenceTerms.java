package org.jmouse.licence;

/**
 * What one licence lets a work be done with.
 *
 * @param spdx  its SPDX identifier: {@code CC-BY-SA-4.0}
 * @param title its full name
 */
public record LicenceTerms(String spdx, String title, Redistribution redistribution, Attribution attribution, ShareAlike shareAlike) {
}
