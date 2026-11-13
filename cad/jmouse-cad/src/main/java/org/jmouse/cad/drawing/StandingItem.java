package org.jmouse.cad.drawing;

/**
 * One item of a drawing as a lock looks at it: what it is called in a refusal, what it is and where it stands apart from how it
 * stands among the others, and how it stands.
 *
 * @param name        how a refusal names it: {@code R1}, {@code the track on top from 0, 0}
 * @param standing    the item with its arrangement left out — what must stay the same while it is held
 * @param arrangement its group and its own state
 */
public record StandingItem(String name, Object standing, Arrangement arrangement) {
}
