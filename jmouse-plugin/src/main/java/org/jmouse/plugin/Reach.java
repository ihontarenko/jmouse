package org.jmouse.plugin;

/**
 * How a plugin stands on a point another plugin declares, by what it brings there.
 *
 * <p>{@link #REQUIRED}: the plugin is made for the point — without its declarer it is refused, and it starts after it.
 * {@link #OPTIONAL}: the plugin adds to the point when its declarer runs — KiCad's 3D model libraries to the 3D models
 * plugin's sources — and does without it otherwise: what it brought is left out, and nothing else of it.</p>
 */
public enum Reach {
    REQUIRED,
    OPTIONAL
}
