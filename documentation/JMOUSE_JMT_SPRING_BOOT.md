# jmouse-jmt-spring-boot

> Spring integration module · Maven artifact `org.jmouse:jmouse-jmt-spring-boot` · source: [module POM](../jmouse-spring/jmouse-jmt-spring-boot/pom.xml).

## Purpose and boundaries

Makes the jMouse Template engine a Spring MVC view engine. The adapter resolves controller view names, exposes the MVC model to templates, and renders servlet responses; template parsing and execution remain in jmouse-el.

## How a product adopts it

Add this artifact to a servlet MVC product with jmouse-el. Place templates under the configured resource prefix and return logical view names from MVC controllers. Supply a TemplateEngine bean or JMouseTemplateViewResolver bean to replace the corresponding default.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-jmt-spring-boot/pom.xml). This integration module uses these library artifacts:

- `jmouse-el`.

## Runtime path

JMouseTemplateAutoConfiguration creates the engine and resolver only in a servlet application with TemplateEngine and DispatcherServlet classes available. The resolver creates JMouseTemplateView instances; each view merges the MVC model into the rendering context and writes the rendered template to the response.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [JMouseTemplateAutoConfiguration](../jmouse-spring/jmouse-jmt-spring-boot/src/main/java/org/jmouse/jmt/spring/autoconfigure/JMouseTemplateAutoConfiguration.java) | Registers the MVC engine and view resolver with missing-bean backoff. |
| [JMouseTemplateProperties](../jmouse-spring/jmouse-jmt-spring-boot/src/main/java/org/jmouse/jmt/spring/JMouseTemplateProperties.java) | Binds template resource naming, response content type and resolver order. |
| [JMouseTemplateViewResolver](../jmouse-spring/jmouse-jmt-spring-boot/src/main/java/org/jmouse/jmt/spring/JMouseTemplateViewResolver.java) | Turns logical MVC view names into template-backed views. |
| [JMouseTemplateView](../jmouse-spring/jmouse-jmt-spring-boot/src/main/java/org/jmouse/jmt/spring/JMouseTemplateView.java) | Renders an MVC model through TemplateEngine into the servlet response. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-jmt-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.jmt.spring.autoconfigure.JMouseTemplateAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [JMouseTemplateAutoConfiguration](../jmouse-spring/jmouse-jmt-spring-boot/src/main/java/org/jmouse/jmt/spring/autoconfigure/JMouseTemplateAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)`<br>`@ConditionalOnClass({TemplateEngine.class, DispatcherServlet.class})`<br>`@EnableConfigurationProperties(JMouseTemplateProperties.class)`<br>`@ConditionalOnMissingBean(TemplateEngine.class)`<br>`@ConditionalOnMissingBean(JMouseTemplateViewResolver.class)` |

## Configuration contract

jmouse.jmt.prefix = templates/; jmouse.jmt.suffix = .html; jmouse.jmt.content-type = text/html;charset=UTF-8; jmouse.jmt.order = Integer.MAX_VALUE - 10 (2147483637). These values are field initializers in JMouseTemplateProperties.

The definitions that own these settings and route placeholders are:

- [JMouseTemplateProperties](../jmouse-spring/jmouse-jmt-spring-boot/src/main/java/org/jmouse/jmt/spring/JMouseTemplateProperties.java).

## HTTP and protocol surface

This module declares no MVC controller routes. It provides services, configuration, lifecycle or view integration to the host. Any endpoints described by a dependent domain library or a separate management artifact are not implicitly owned by this adapter.

## Lifecycle and adoption gotchas

View resolver order matters when several engines coexist. Prefix and suffix are resource naming settings, not request route prefixes. Replacing the engine does not automatically replace the resolver: each bean has its own missing-bean condition.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-jmt-spring-boot/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-jmt-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-jmt-spring-boot/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-el` | compile | no |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | yes |
| `org.springframework:spring-webmvc` | compile | yes |
| `jakarta.servlet:jakarta.servlet-api` | provided | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.jmt.spring`

| Type | Responsibility / contract |
|---|---|
| [JMouseTemplateProperties](../jmouse-spring/jmouse-jmt-spring-boot/src/main/java/org/jmouse/jmt/spring/JMouseTemplateProperties.java) | Operations: `getPrefix`, `setPrefix`, `getSuffix`, `setSuffix`, `getContentType`, `setContentType`, `getOrder`, `setOrder`. |
| [JMouseTemplateView](../jmouse-spring/jmouse-jmt-spring-boot/src/main/java/org/jmouse/jmt/spring/JMouseTemplateView.java) | Implements/extends `View`. Operations: `getContentType`, `setContentType`, `render`. |
| [JMouseTemplateViewResolver](../jmouse-spring/jmouse-jmt-spring-boot/src/main/java/org/jmouse/jmt/spring/JMouseTemplateViewResolver.java) | Implements/extends `ViewResolver, Ordered`. Operations: `resolveViewName`, `setContentType`, `getOrder`, `setOrder`. |

#### `org.jmouse.jmt.spring.autoconfigure`

| Type | Responsibility / contract |
|---|---|
| [JMouseTemplateAutoConfiguration](../jmouse-spring/jmouse-jmt-spring-boot/src/main/java/org/jmouse/jmt/spring/autoconfigure/JMouseTemplateAutoConfiguration.java) | Operations: `jmouseTemplateEngine`, `jmouseTemplateViewResolver`. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-jmt-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
