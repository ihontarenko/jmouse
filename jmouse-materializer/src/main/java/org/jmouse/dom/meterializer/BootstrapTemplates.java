package org.jmouse.dom.meterializer;

import org.jmouse.meterializer.NodeTemplate;
import org.jmouse.util.Strings;

import static org.jmouse.meterializer.NodeTemplate.*;
import static org.jmouse.meterializer.NodeTemplate.when;
import static org.jmouse.meterializer.TemplatePredicate.*;
import static org.jmouse.meterializer.ValueExpression.*;

/**
 * Bootstrap-styled template presets built on top of {@link NodeTemplate}. 🎨
 *
 * <p>
 * This class provides small, composable building blocks for creating form controls and buttons
 * with Bootstrap-compatible class names and common markup structure.
 * </p>
 *
 * <p>
 * The templates produced here are still <b>declarative</b> (intermediate representation).
 * Actual DOM creation happens later during materialization.
 * </p>
 *
 * <h3>Typical usage</h3>
 *
 * <pre>{@code
 * NodeTemplate form = NodeTemplate.element("form", f -> f
 *     .child(BootstrapTemplates.inputEmail("name", "label", "value"))
 *     .child(BootstrapTemplates.submitButton("Send"))
 * );
 * }</pre>
 *
 * <h3>Integration note</h3>
 * <p>
 * For submission re-population and error decoration you typically combine these presets with
 * hooks like {@code SubmissionDecorationHook} and option-selection helpers. 🧩
 * </p>
 */
public final class BootstrapTemplates {

    private BootstrapTemplates() {}

    /**
     * Creates a Bootstrap-styled {@code <button>} wrapped with a margin container.
     *
     * <p>
     * Caption resolution priority:
     * </p>
     * <ol>
     *     <li>{@code request("submitCaption")} if present</li>
     *     <li>{@code path("description")} if present → "Submit: {description}"</li>
     *     <li>{@code defaultCaption}</li>
     * </ol>
     *
     * @param type button type attribute (e.g. {@code "submit"} or {@code "button"})
     * @param defaultCaption fallback caption text
     * @return button template
     */
    public static NodeTemplate button(String type, String defaultCaption) {
        return element("div", block -> block
                .attribute("class", constant("mt-3"))
                .child(element("button", button -> button
                        .attribute("type", constant(type))
                        .attribute("class", constant("btn btn-primary"))
                        .child(when(
                                present(request("submitCaption")),
                                t -> t.add(text(request("submitCaption"))),
                                f -> f.add(when(
                                        present(path("description")),
                                        t -> t.add(text(constant("Submit: "))).add(text(path("description"))),
                                        k -> k.add(text(constant(defaultCaption))),
                                        "div"
                                ))
                        ))
                ))
        );
    }

    /**
     * Shortcut for {@code type="submit"} button.
     *
     * @param caption default caption
     * @return submit button template
     */
    public static NodeTemplate submitButton(String caption) {
        return button("submit", caption);
    }

    /**
     * Shortcut for {@code type="button"} button.
     *
     * @param caption default caption
     * @return input/button template
     */
    public static NodeTemplate inputButton(String caption) {
        return button("button", caption);
    }

    /**
     * Creates a Bootstrap input group ({@code label + input}) inside {@code mb-3} wrapper.
     *
     * <p>
     * The {@code namePath} is used both for {@code name}, {@code id}, and {@code for}.
     * If {@code valuePath} is non-empty, the input's {@code value} is taken from request attributes
     * via {@code request(valuePath)}.
     * </p>
     *
     * @param type input type
     * @param namePath path to control name/id
     * @param labelPath path to label text
     * @param valuePath request-attribute key for value (optional; empty disables)
     * @return input template
     */
    public static NodeTemplate input(InputType type, String namePath, String labelPath, String valuePath) {
        return element("div", group -> group
                .attribute("class", constant("mb-3"))
                .child(element("label", label -> label
                        .attribute("for", path(namePath))
                        .attribute("class", constant("form-label"))
                        .child(text(path(labelPath)))
                ))
                .child(element("input", input -> {
                    input.attribute("type", constant(type.htmlValue()));
                    input.attribute("name", path(namePath));
                    input.attribute("id", path(namePath));
                    input.attribute("class", constant("form-control"));

                    if (Strings.isNotEmpty(valuePath)) {
                        input.attribute("value", request(valuePath));
                    }
                }))
        );
    }

    /**
     * Convenience for {@link InputType#TEXT}.
     */
    public static NodeTemplate inputText(String namePath, String labelPath, String valuePath) {
        return input(InputType.TEXT, namePath, labelPath, valuePath);
    }

    /**
     * Convenience for {@link InputType#NUMBER}.
     */
    public static NodeTemplate inputNumber(String namePath, String labelPath, String valuePath) {
        return input(InputType.NUMBER, namePath, labelPath, valuePath);
    }

    /**
     * Convenience for {@link InputType#EMAIL}.
     */
    public static NodeTemplate inputEmail(String namePath, String labelPath, String valuePath) {
        return input(InputType.EMAIL, namePath, labelPath, valuePath);
    }

    /**
     * Convenience for {@link InputType#PASSWORD}.
     */
    public static NodeTemplate inputPassword(String namePath, String labelPath, String valuePath) {
        return input(InputType.PASSWORD, namePath, labelPath, valuePath);
    }

    /**
     * Creates a Bootstrap {@code <select>} control with repeated {@code <option>} children.
     *
     * <p>
     * The options are expected at {@code optionsPath} and iterated via {@link NodeTemplate#repeat}.
     * Within the repeat body, the current item is bound to variable {@code "option"}.
     * </p>
     *
     * @param namePath path to control name/id
     * @param labelPath path to label text
     * @param optionsPath path to options collection
     * @param keyPath path to option key (used for {@code value} attribute)
     * @param valuePath path to option label/value text
     * @return select template
     */
    public static NodeTemplate select(
            String namePath,
            String labelPath,
            String optionsPath,
            String keyPath,
            String valuePath
    ) {
        return element("div", group -> group
                .attribute("class", constant("mb-3"))
                .child(element("label", label -> label
                        .attribute("for", path(namePath))
                        .attribute("class", constant("form-label"))
                        .child(text(path(labelPath)))
                ))
                .child(element("select", select -> select
                        .attribute("name", path(namePath))
                        .attribute("id", path(namePath))
                        .attribute("class", constant("form-select"))
                        .child(repeat(
                                path(optionsPath),
                                "option",
                                inner -> inner.add(
                                        element("option", option -> option
                                                .attribute("value", path(keyPath))
                                                .child(text(path(valuePath)))
                                        )
                                ),
                                "FOOTER"
                        ))
                ))
        );
    }

    /**
     * Creates a single Bootstrap checkbox ({@code input + label}) inside {@code form-check}.
     *
     * @param namePath path to control name/id
     * @param labelPath path to label text
     * @param valuePath path to checkbox value (optional; empty disables)
     * @return checkbox template
     */
    public static NodeTemplate checkboxSingle(String namePath, String labelPath, String valuePath) {
        return element("div", group -> group
                .attribute("class", constant("mb-3"))
                .child(element("div", row -> row
                        .attribute("class", constant("form-check"))
                        .child(element("input", input -> {
                            input.attribute("type", constant("checkbox"));
                            input.attribute("class", constant("form-check-input"));
                            input.attribute("name", path(namePath));
                            input.attribute("id", path(namePath));
                            if (Strings.isNotEmpty(valuePath)) {
                                input.attribute("value", path(valuePath));
                            }
                        }))
                        .child(element("label", label -> label
                                .attribute("class", constant("form-check-label"))
                                .attribute("for", path(namePath))
                                .child(text(path(labelPath)))
                        ))
                ))
        );
    }

    /**
     * Creates a radio group as repeated {@code form-check} rows.
     *
     * @param namePath path to control name
     * @param labelPath path to group label text
     * @param optionsPath path to options collection
     * @param keyPath path to option key (used as input value)
     * @param valuePath path to option label text
     * @return radio group template
     */
    public static NodeTemplate radioGroup(
            String namePath,
            String labelPath,
            String optionsPath,
            String keyPath,
            String valuePath
    ) {
        return element("div", group -> group
                .attribute("class", constant("mb-3"))
                .child(element("div", title -> title
                        .attribute("class", constant("form-label"))
                        .child(text(path(labelPath)))
                ))
                .child(repeat(
                        path(optionsPath),
                        "option",
                        inner -> inner.add(element("div", row -> row
                                .attribute("class", constant("form-check"))
                                .child(element("input", input -> input
                                        .attribute("type", constant("radio"))
                                        .attribute("class", constant("form-check-input"))
                                        .attribute("name", path(namePath))
                                        .attribute("value", path(keyPath))
                                ))
                                .child(element("label", label -> label
                                        .attribute("class", constant("form-check-label"))
                                        .child(text(path(valuePath)))
                                ))
                        ))
                ))
        );
    }

    /**
     * Creates a checkbox group as repeated {@code form-check} rows.
     *
     * <p>
     * The current implementation chooses between {@code checked=true/false} inputs
     * using a predicate (see {@link #checkboxInput(String, String, boolean)}).
     * </p>
     *
     * @param namePath path to control name
     * @param labelPath path to group label text
     * @param optionsPath path to options collection
     * @param keyPath path to option key (used as input value)
     * @param valuePath path to option label text
     * @return checkbox group template
     */
    public static NodeTemplate checkbox(
            String namePath,
            String labelPath,
            String optionsPath,
            String keyPath,
            String valuePath
    ) {
        return element("div", group -> group
                .attribute("class", constant("mb-3"))
                .child(element("div", title -> title
                        .attribute("class", constant("form-label"))
                        .child(text(path(labelPath)))
                ))
                .child(repeat(
                        path(optionsPath),
                        "option",
                        inner -> inner.add(element("div", row -> row
                                .attribute("class", constant("form-check"))
                                .child(when(
                                        contains(path(optionsPath), path("option.key")),
                                        whenTrue -> whenTrue.add(checkboxInput(namePath, keyPath, true)),
                                        whenFalse -> whenFalse.add(checkboxInput(namePath, keyPath, false))
                                ))
                                .child(element("label", label -> label
                                        .attribute("class", constant("form-check-label"))
                                        .child(text(path(valuePath)))
                                ))
                        ))
                ))
        );
    }

    /**
     * Builds a checkbox input template and optionally marks it as checked.
     *
     * <p>
     * This is an internal helper used by checkbox group templates.
     * </p>
     *
     * @param namePath path to input name
     * @param keyPath path to input value
     * @param checked whether to add {@code checked="checked"}
     * @return checkbox input template
     */
    private static NodeTemplate checkboxInput(String namePath, String keyPath, boolean checked) {
        return element("input", input -> {
            input.attribute("type", constant("checkbox"));
            input.attribute("class", constant("form-check-input"));
            input.attribute("name", path(namePath));
            input.attributeIf(same(constant("a"), constant("a")), "type", constant("checkbox"));
            input.attribute("value", path(keyPath));
            if (checked) {
                input.attribute("checked", constant("checked"));
            }
        });
    }

}