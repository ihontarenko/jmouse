/**
 * Spring Boot autoconfiguration for the AI modules.
 *
 * <p>Wiring, and nothing but wiring: collect the tool definitions a product's features contribute,
 * build the catalogue so its startup refusals run, bind properties, order the guard chain, run the
 * library's migrations, register the Model Context Protocol transport and — only when that module is
 * present and switched on — the management controllers.
 *
 * <p>Every satellite is optional, so a product pays for what it takes: tools without a model provider,
 * a provider without tools, either without an access engine.
 *
 * <p>No controllers of its own. Neither of the other jMouse starters ships one, for the same reason: a
 * library controller cannot know a product's error model, its authorization annotation, or its admin
 * route conventions.
 */
package org.jmouse.ai.spring;
