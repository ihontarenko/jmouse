package org.jmouse.svit.example;

import svit.beans.annotation.Provide;

@Provide(value = "util", proxied = true)
public class DefaultUtils implements Utils {

    @Override
    public String getOsName() {
        return System.getProperty("os.name");
    }

}
