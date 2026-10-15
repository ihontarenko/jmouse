package org.jmouse.parallax;

import org.jmouse.parallax.object.Interval;

import java.util.List;

/**
 * The part of an object's plane a view asks for when it is zoomed past the detail of the whole picture: a centre and
 * a half-width, so a zoom history is a list of them. The height follows the object's own aspect, so a window is the
 * object's rectangle moved and scaled, never stretched. Every computation that samples a rectangle of the plane
 * declares it — escape time, Newton basins, domain colouring, field heat maps, implicit curves, densities.
 */
public final class PlaneWindow {

    public static final String SETTING = "window";

    private static final SettingSpecification SPECIFICATION = SettingSpecification.vector(SETTING, List.of(), 3,
            -1e12, 1e12, "[centre x, centre y, half-width]; the object's region when empty");

    private PlaneWindow() {
    }

    public static SettingSpecification specification() {
        return SPECIFICATION;
    }

    /** The rectangle to sample: the window when one is asked for, else the object's own `x` × `y`. */
    public static Interval[] region(ComputationSettings settings, Interval x, Interval y) {
        double[] window = settings.vectorOr(SETTING, new double[0]);
        if (window.length == 0) {
            return new Interval[]{x, y};
        }
        if (!(window[2] > 0)) {
            throw new IllegalArgumentException("The window's half-width is positive.");
        }
        double aspect = y.length() / x.length();
        return new Interval[]{Interval.of(window[0] - window[2], window[0] + window[2]),
                Interval.of(window[1] - window[2] * aspect, window[1] + window[2] * aspect)};
    }

    /** How many times the window magnifies the object's own width; 1 without a window. */
    public static double magnification(ComputationSettings settings, Interval x) {
        double[] window = settings.vectorOr(SETTING, new double[0]);
        return window.length == 0 || !(window[2] > 0) ? 1 : Math.max(1, x.length() / (2 * window[2]));
    }
}
