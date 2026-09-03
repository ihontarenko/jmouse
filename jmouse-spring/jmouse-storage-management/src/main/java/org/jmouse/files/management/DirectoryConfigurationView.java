package org.jmouse.files.management;

/**
 * 🔧 What applies to a folder, of one kind, and where it came from.
 *
 * <h3>⚠️ Shaped per KIND rather than for {@code upload} alone</h3>
 *
 * <p>A view with fields called {@code uploadMode}, {@code uploadExtensions}, … has to be redesigned the
 * day a second kind of directory configuration arrives — and the table behind it was built to take one.
 * Kind to {@code { effective, origin }} costs nothing now and everything later.</p>
 *
 * <h3>⚠️ Why the origin cannot be left to the client</h3>
 *
 * <p>From below, an inherited configuration and a folder's own look identical: both are simply "the rule
 * here". Nothing a screen can see distinguishes a folder somebody configured from one that merely sits
 * under one, and "inherited from {@code innoventa/files}" against "set here" is exactly the sentence a
 * person needs before changing anything.</p>
 *
 * @param effective   the rule that applies here, after inheritance, in its kind's own shape
 * @param origin      where it came from — {@link #SELF}, {@link #INHERITED} or {@link #INSTALLATION}
 * @param originPath  the ancestor's whole address when inherited, otherwise {@code null}
 * @param own         whether this folder carries a row of its own, which is what "clear" is drawn from
 */
public record DirectoryConfigurationView(Object effective, String origin, String originPath,
                                         boolean own) {

    /** This folder carries the rule itself. */
    public static final String SELF = "SELF";

    /** An ancestor carries it, and {@link #originPath} says which. */
    public static final String INHERITED = "INHERITED";

    /** Nobody in the chain has an opinion, so the installation's own rule applies. */
    public static final String INSTALLATION = "INSTALLATION";

    /**
     * 🏗️ A rule this folder states itself.
     *
     * @param effective the rule
     * @return the view
     */
    public static DirectoryConfigurationView self(Object effective) {
        return new DirectoryConfigurationView(effective, SELF, null, true);
    }

    /**
     * 🏗️ A rule an ancestor states.
     *
     * @param effective  the rule
     * @param originPath the ancestor's address
     * @return the view
     */
    public static DirectoryConfigurationView inherited(Object effective, String originPath) {
        return new DirectoryConfigurationView(effective, INHERITED, originPath, false);
    }

    /**
     * 🏗️ The installation's own rule, nobody in the chain having one.
     *
     * @param effective the rule
     * @return the view
     */
    public static DirectoryConfigurationView installation(Object effective) {
        return new DirectoryConfigurationView(effective, INSTALLATION, null, false);
    }
}
