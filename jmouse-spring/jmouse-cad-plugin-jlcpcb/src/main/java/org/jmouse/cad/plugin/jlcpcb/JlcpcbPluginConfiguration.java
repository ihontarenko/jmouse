package org.jmouse.cad.plugin.jlcpcb;

import org.jmouse.cad.plugin.gerber.GerberSet;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** 🧩 The JLCPCB plugin, on the classpath: its plugin bean, on the Gerber plugin's set. */
@AutoConfiguration(afterName = "org.jmouse.cad.plugin.gerber.GerberPluginConfiguration")
public class JlcpcbPluginConfiguration {

    @Bean
    public JlcpcbPlugin cadJlcpcbPlugin(GerberSet set) {
        return new JlcpcbPlugin(set);
    }
}
