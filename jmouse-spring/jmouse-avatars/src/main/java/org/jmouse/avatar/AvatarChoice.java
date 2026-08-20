package org.jmouse.avatar;

/**
 * 🙂 Which of the three kinds of face somebody wears.
 *
 * <p>⚠️ A tri-state rather than "a picture or nothing", because the third case is a real choice and not
 * an absence: somebody who picked drawn initials has decided something, and a product that models it as
 * a null picture cannot tell them apart from somebody who never opened the screen.</p>
 */
public enum AvatarChoice {

    /** Drawn from the name — the state everybody starts in. */
    INITIALS,

    /** A generated face, drawn from a seed the person picked. */
    PRESET,

    /** A picture they uploaded. */
    UPLOAD
}
