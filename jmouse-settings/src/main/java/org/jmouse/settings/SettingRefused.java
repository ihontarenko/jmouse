package org.jmouse.settings;

/** 🚫 A change to settings that was not kept, and why, in words a reader is shown. Nothing of it was kept. */
public class SettingRefused extends RuntimeException {

    public SettingRefused(String reason) {
        super(reason);
    }
}
