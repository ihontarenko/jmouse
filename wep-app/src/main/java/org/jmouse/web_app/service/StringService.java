package org.jmouse.web_app.service;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.core.AbstractStringIdGenerator;
import org.jmouse.core.SecureRandomStringGenerator;
import org.jmouse.security.core.access.Phase;
import org.jmouse.security.core.access.annotation.Authorize;

@Bean(proxied = true)
public class StringService {

    private final AbstractStringIdGenerator idGenerator = new SecureRandomStringGenerator(32);

    @Authorize(value = "returnValue | length == 32", phase = Phase.AFTER)
    public String getRandom() {
        return idGenerator.generate();
    }

}
