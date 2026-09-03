package org.jmouse.storage.administration;

import java.util.List;

/**
 * 🗄️ What this installation is actually configured to do with files.
 *
 * <p>⚠️ <strong>Resolved, not as written.</strong> The upload policy is the single most misread piece of
 * storage configuration: a product names a profile and the effective allow/deny lists live in code, so
 * "what does this installation accept" has never been answerable without reading Java. This answers it.</p>
 *
 * @param backends         every backend this application built
 * @param defaultBackend   which one new content goes to
 * @param contentAddressed whether keys are digests, and so whether identical bytes cost one object
 * @param maximumSizeBytes the installation's ceiling
 * @param acceptanceMode   ALLOW_LIST or DENY_LIST — what the lists below mean
 * @param contentTypes     the resolved content-type list
 * @param extensions       the resolved extension list
 * @param sweeperEnabled   whether the scheduled sweep runs at all
 * @param gracePeriod      how long an object is left alone before it can be a candidate
 * @param registeredObjects how many rows the registry holds
 */
public record StorageOverview(List<String> backends, String defaultBackend, boolean contentAddressed,
                              long maximumSizeBytes, String acceptanceMode, List<String> contentTypes,
                              List<String> extensions, boolean sweeperEnabled, String gracePeriod,
                              long registeredObjects) {
}
