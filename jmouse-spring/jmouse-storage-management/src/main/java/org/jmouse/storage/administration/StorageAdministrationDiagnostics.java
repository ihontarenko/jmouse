package org.jmouse.storage.administration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * 🔎 Says out loud where the administration surface is mounted.
 *
 * <h3>⚠️ Not decoration — this is the countermeasure to a failure that has already happened</h3>
 *
 * <p>The AI management screen's address lived in several files that nothing checked against each other,
 * and when they drifted the product did not fail. It rendered as an <strong>empty installation</strong>:
 * no error, no refusal, a screen that looked like a correctly-configured product with nothing in it. The
 * only cheap defence is that the server states the address it is actually serving, once, where somebody
 * comparing it against an interface's router can see it.</p>
 */
public class StorageAdministrationDiagnostics implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageAdministrationDiagnostics.class);

    @Override
    public void afterPropertiesSet() {
        LOGGER.info("🗄️ Storage administration mounted at {} — overview, registry, references, "
                    + "sweep preview and sweep", AdministrationRoutes.BASE);
    }
}
