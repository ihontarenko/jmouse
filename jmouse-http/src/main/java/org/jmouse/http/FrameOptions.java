package org.jmouse.http;

/**
 * 🖼️ X-Frame-Options header builder.
 *
 * <p>Controls whether the page can be embedded in an
 * {@code <iframe>} / {@code <frame>} / {@code <object>}.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * FrameOptions deny = new FrameOptions().deny();
 * System.out.println(deny.toHttpHeader());   // X_FRAME_OPTIONS
 * System.out.println(deny.toHeaderValue()); // DENY
 *
 * FrameOptions same = new FrameOptions().sameOrigin();
 * System.out.println(same.toHeaderValue()); // SAMEORIGIN
 * }</pre>
 */
public class FrameOptions {

    private Mode mode;

    /**
     * 🏷️ The HTTP header name ({@code X-Frame-Options}).
     */
    public HttpHeader toHttpHeader() {
        return HttpHeader.X_FRAME_OPTIONS;
    }

    /**
     * 🔗 Render the header value.
     *
     * @return string like {@code DENY} or {@code SAMEORIGIN}
     */
    public String toHeaderValue() {
        return mode.name();
    }

    /**
     * ⚙️ Set explicit mode.
     */
    public FrameOptions mode(Mode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * ⛔ Shortcut for {@code DENY}.
     */
    public FrameOptions deny() {
        return mode(Mode.DENY);
    }

    /**
     * 🏠 Shortcut for {@code SAMEORIGIN}.
     */
    public FrameOptions sameOrigin() {
        return mode(Mode.SAMEORIGIN);
    }

    /**
     * 🚦 Supported modes.
     */
    public enum Mode {DENY, SAMEORIGIN}
}
