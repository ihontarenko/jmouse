package org.jmouse.parallax.jpa.computation;

import org.jmouse.parallax.owner.ParallaxOwner;

/**
 * 📍 Where an owned job belongs: what it was computed for (a project when one was named, else the workspace) and the
 * realm it counts against and is listed under (the workspace).
 *
 * @param owner what the job was computed for
 * @param realm where it counts and is listed
 */
public record JobPlacement(ParallaxOwner owner, ParallaxOwner realm) {
}
