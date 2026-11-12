package org.jmouse.suppliers;

import org.jmouse.http.client.HttpCall;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * One way to reach a distributor: per call rather than per driver, so an installation may hold several accounts and
 * rotate a secret while it runs.
 *
 * @param label       how a reader knows this access — it opens every failure's words
 * @param address     where the distributor is called; null for the driver's own
 * @param credentials what it knows the caller by
 * @param timeout     how long one call may take as a whole; null for the HTTP client's usual time
 */
public record SupplierAccess(String label, URI address, SupplierCredentials credentials, Duration timeout) {

    public SupplierAccess {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(credentials, "credentials");
        timeout = timeout == null ? HttpCall.USUAL_TIMEOUT : timeout;
    }
}
