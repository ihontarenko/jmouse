package org.jmouse.parallax.event;

/** The catalogue warm-up ended, finished or stopped, with what it did on the way. */
public record CatalogueWarmed(int computed, int queued, int cached, int failed) {
}
