package org.jmouse.plugin;

/** 🚫 A change to a plugin's settings that was not kept, and why, in words a reader is shown. Nothing of it was kept. */
public class PluginSettingRefused extends RuntimeException {

    public PluginSettingRefused(String reason) {
        super(reason);
    }
}
