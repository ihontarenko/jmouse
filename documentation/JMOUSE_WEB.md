# jmouse-web — servlet MVC, web context and embedded server

> **Agent orientation file.** Code wins over this description.
> Module: `jmouse-web` · Java 21 · Servlet-bound jMouse MVC, distinct from Spring MVC integration modules.

## What it is

The jMouse web framework over its own bean/application context: servlet request dispatch, annotated
handlers, argument and return-value adaptation, views, static resources, filters, web security and
embedded Tomcat. [jmouse-http](JMOUSE_HTTP.md) contains protocol values usable without a servlet
container; this module supplies their request/response integration.

## The map

| Type | Responsibility |
|---|---|
| [WebApplicationLauncher](../jmouse-web/src/main/java/org/jmouse/web/mvc/WebApplicationLauncher.java) / [WebLauncher](../jmouse-web/src/main/java/org/jmouse/web/WebLauncher.java) | Bootstrap a web context and create its configured server |
| [WebContextBuilder](../jmouse-web/src/main/java/org/jmouse/web/WebContextBuilder.java) / [WebApplicationContextBuilder](../jmouse-web/src/main/java/org/jmouse/web/WebApplicationContextBuilder.java) / [WebApplicationFactory](../jmouse-web/src/main/java/org/jmouse/web/WebApplicationFactory.java) | Assemble web-aware bean contexts and configuration roots |
| [WebMvcComponents](../jmouse-web/src/main/java/org/jmouse/web/mvc/WebMvcComponents.java) | Register the MVC components used by dispatch |
| [ServletDispatcher](../jmouse-web/src/main/java/org/jmouse/web/servlet/ServletDispatcher.java) | Abstract servlet initialization and request-dispatch skeleton |
| [HandlerMethod](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/HandlerMethod.java) / [HandlerMethodInvocation](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/HandlerMethodInvocation.java) / [HandlerMethodContext](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/HandlerMethodContext.java) | Controller method metadata and invocation context |
| [ReturnValueProcessor](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/ReturnValueProcessor.java) / [ReturnValueHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/ReturnValueHandler.java) | Select an adapter for a controller's result |
| [ViewResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/ViewResolver.java) / [View](../jmouse-web/src/main/java/org/jmouse/web/mvc/View.java) / [InternalViewResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/view/internal/InternalViewResolver.java) | Resolve logical views and render through the internal template engine |
| [HttpMessageConverter](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/HttpMessageConverter.java) / [HttpMessageReturnValueHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/HttpMessageReturnValueHandler.java) | Convert request/response bodies and render HTTP message results |
| [ResourceHandlerRegistry](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceHandlerRegistry.java) / [ResourceHttpHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceHttpHandler.java) / [ResourceResolverChain](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceResolverChain.java) | Register static paths, resolve resources and serve content |
| [MediaTypeManager](../jmouse-web/src/main/java/org/jmouse/web/negotiation/MediaTypeManager.java) / [MediaTypeLookup](../jmouse-web/src/main/java/org/jmouse/web/negotiation/MediaTypeLookup.java) | Ordered request content-negotiation strategies |
| [RegistrationBean](../jmouse-web/src/main/java/org/jmouse/web/servlet/registration/RegistrationBean.java) / [FilterRegistrationBean](../jmouse-web/src/main/java/org/jmouse/web/servlet/registration/FilterRegistrationBean.java) / [ServletRegistrationBean](../jmouse-web/src/main/java/org/jmouse/web/servlet/registration/ServletRegistrationBean.java) | Register container components from beans |
| [HttpSecurity](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/builder/HttpSecurity.java) / [SecurityConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/SecurityConfigurer.java) | Compose request security filters and their shared collaborators |
| [Firewall](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/Firewall.java) / [FirewallPolicy](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/FirewallPolicy.java) | Ordered request inspection decisions |
| [WebServer](../jmouse-web/src/main/java/org/jmouse/web/server/WebServer.java) / [TomcatWebServerFactory](../jmouse-web/src/main/java/org/jmouse/web/server/tomcat/TomcatWebServerFactory.java) / [WebServerConfig](../jmouse-web/src/main/java/org/jmouse/web/server/WebServerConfig.java) | Embedded-server lifecycle and Tomcat configuration |

## Boot and request flow

```java
new WebApplicationLauncher(Application.class).launch(arguments);
```

`Application` is the adopter's configuration root. The launcher creates the source context and
server using the web-context factories, feature configuration and `WebServerFactory` beans. This
is a jMouse bootstrap path, not `SpringApplication.run`. Use the `wep-app` source as an integration
example; its product classes are not library API.

A servlet request passes registered filters into the configured dispatcher, which finds a mapped
handler, chooses an adapter, resolves arguments, invokes the handler/interceptors and processes the
result. Body converters and view resolvers answer different result shapes; a view name is not
automatically a JSON response. Exception resolvers map known MVC failures to HTTP responses.

## Extension points

Contribute handler mappings/adapters, method argument resolvers, return-value handlers, interceptors,
message converters or view resolvers through the framework's configured components. Use
`ResourceHandlerRegistry` and its resolver/version strategies for static assets. Content negotiation
uses ordered `MediaTypeLookup` strategies, including Accept, Content-Type and configured path/query
lookups; preserve ordering instead of assuming every lookup is consulted equally.

Servlet filter/listener/servlet registration beans bridge the bean lifecycle to the container.
`RequestContextListener` and session filters establish request attributes for context-aware code;
thread-local request state does not automatically follow asynchronous work.

## Security and operational boundaries

`@EnableWebSecurity`, method authorization features and JSR-250 support are explicit feature choices.
Authentication filters, security-context repositories and authorization filters have separate
responsibilities. Configure the desired repository/session behavior rather than assuming every
credential creates a session. Product services invoked outside HTTP still require their own
authorization boundary; filters only cover requests traversing their chain.

Shallow ETag filters buffer responses to compute a tag, so account for body size when choosing them
for large streams. Firewall inspection is a request policy layer; it does not replace parameterized
SQL, correct output encoding or domain authorization. Server, session, dispatcher and view property
classes in the class map identify each configuration surface and its owning package.

For using jMouse templates within an existing Spring MVC app, use
[jmouse-jmt-spring-boot](JMOUSE_JMT_SPRING_BOOT.md) instead of starting a second jMouse container.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-web/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |
| `org.jmouse:jmouse-mapper` | compile | no |
| `org.jmouse:jmouse-http` | compile | no |
| `org.jmouse:jmouse-security` | compile | no |
| `org.jmouse:jmouse-jdbc` | compile | no |
| `org.jmouse:jmouse-context` | compile | no |
| `org.jmouse:jmouse-validation` | compile | no |
| `jakarta.servlet:jakarta.servlet-api` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |
| `com.fasterxml.jackson.core:jackson-databind` | compile | no |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-xml` | compile | no |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` | compile | no |
| `jakarta.xml.bind:jakarta.xml.bind-api` | compile | no |
| `org.glassfish.jaxb:jaxb-runtime` | compile | no |
| `org.apache.tomcat.embed:tomcat-embed-core` | compile | no |
| `org.apache.tomcat.embed:tomcat-embed-jasper` | compile | no |
| `org.apache.tomcat.embed:tomcat-embed-el` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.security.web`

| Type | Responsibility / contract |
|---|---|
| [AuthenticationEntryPoint](../jmouse-web/src/main/java/org/jmouse/security/web/AuthenticationEntryPoint.java) | Start an authentication challenge/redirect through initiate(request, response, exception). |
| [AuthorizationFailureHandler](../jmouse-web/src/main/java/org/jmouse/security/web/AuthorizationFailureHandler.java) | Render an authorization failure through onFailure(request, response, exception). |
| [EnableAuthorizeMethod](../jmouse-web/src/main/java/org/jmouse/security/web/EnableAuthorizeMethod.java) | 🔐 Enables native jMouse method-level authorization support. |
| [EnableJsr250MethodSecurity](../jmouse-web/src/main/java/org/jmouse/security/web/EnableJsr250MethodSecurity.java) | 🔐 Enables JSR-250 method-level security support. |
| [EnableWebSecurity](../jmouse-web/src/main/java/org/jmouse/security/web/EnableWebSecurity.java) | 🔐 Enables core web security support. |
| [MatchableSecurityFilterChain](../jmouse-web/src/main/java/org/jmouse/security/web/MatchableSecurityFilterChain.java) | Implements/extends `Matcher`. Operations: `matches`, `getFilters`. |
| [MatchResult](../jmouse-web/src/main/java/org/jmouse/security/web/MatchResult.java) | 🎯 Result of evaluating a web request matcher (e.g., path/method/headers). |
| [OrderedFilter](../jmouse-web/src/main/java/org/jmouse/security/web/OrderedFilter.java) | Value record: `(Filter filter, int order) implements Filter, Ordered`. |
| [RequestMatch](../jmouse-web/src/main/java/org/jmouse/security/web/RequestMatch.java) | 📦 Represents a successful mapping between an incoming HttpServletRequest and a resolved Match context. |
| [RequestMatcherEntry](../jmouse-web/src/main/java/org/jmouse/security/web/RequestMatcherEntry.java) | Value record: `(MappingMatcher matcher, E entry)`. |
| [SecurityFilter](../jmouse-web/src/main/java/org/jmouse/security/web/SecurityFilter.java) | Implements/extends `BeanFilter`. |
| [SecurityFilterChain](../jmouse-web/src/main/java/org/jmouse/security/web/SecurityFilterChain.java) | Implements/extends `MatchableSecurityFilterChain`. Operations: `matches`, `getFilters`. |
| [SecurityFilterChainDelegator](../jmouse-web/src/main/java/org/jmouse/security/web/SecurityFilterChainDelegator.java) | 🛡️ SecurityFilterChainDelegator |
| [SecurityFilterChainDelegatorRegistration](../jmouse-web/src/main/java/org/jmouse/security/web/SecurityFilterChainDelegatorRegistration.java) | Implements/extends `DelegatingBeanFilterRegistration`. |
| [SecurityFilterOrder](../jmouse-web/src/main/java/org/jmouse/security/web/SecurityFilterOrder.java) | 🧱 Defines recommended filter ordering in the jMouse Security web pipeline. |

#### `org.jmouse.security.web.access`

| Type | Responsibility / contract |
|---|---|
| [CompositeAuthorizationManager](../jmouse-web/src/main/java/org/jmouse/security/web/access/CompositeAuthorizationManager.java) | Implements/extends `AuthorizationManager`. Operations: `check`, `addMapping`, `defaultManager`, `build`, `isEmpty`. |
| [ExceptionTranslationFilter](../jmouse-web/src/main/java/org/jmouse/security/web/access/ExceptionTranslationFilter.java) | Implements/extends `BeanFilter`. Operations: `doFilterInternal`. |

#### `org.jmouse.security.web.access.method`

| Type | Responsibility / contract |
|---|---|
| [AuthorizeMethodSecurityConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/access/method/AuthorizeMethodSecurityConfigurer.java) | Implements/extends `BeanConfigurer`. Operations: `configure`. |
| [Jsr250MethodSecurityConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/access/method/Jsr250MethodSecurityConfigurer.java) | 🔐 BeanConfigurer that enables JSR-250 method-level security. |

#### `org.jmouse.security.web.authentication`

| Type | Responsibility / contract |
|---|---|
| [AbstractAuthenticationFilter](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/AbstractAuthenticationFilter.java) | Implements/extends `SecurityFilter`. Operations: `doFilterInternal`, `setContinueChainBeforeSuccess`, `getContextRepository`, `getAuthenticationManager`, `getMatcherCriteria`, `getSuccessHandler`, `setSuccessHandler`, `getFailureHandler`, `setFailureHandler`. |
| [AbstractAuthenticationProvider](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/AbstractAuthenticationProvider.java) | Implements/extends `AuthenticationProvider`. Operations: `provide`. |
| [AbstractRealmAuthenticationEntryPoint](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/AbstractRealmAuthenticationEntryPoint.java) | 🛡️ AbstractRealmAuthenticationEntryPoint |
| [AnonymousAuthenticationFilter](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/AnonymousAuthenticationFilter.java) | Implements/extends `SecurityFilter`. Operations: `doFilterInternal`. |
| [AuthenticationFailureHandler](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/AuthenticationFailureHandler.java) | Handle a failed authentication attempt through onFailure(request, response, exception). |
| [AuthenticationProvider](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/AuthenticationProvider.java) | Operations: `provide`. |
| [AuthenticationSuccessHandler](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/AuthenticationSuccessHandler.java) | Handle a successful authentication attempt through onSuccess(request, response). |
| [IdentityRestoreService](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/IdentityRestoreService.java) | Operations: `autoLogin`, `success`, `failure`. |
| [LogoutAuthenticationFilter](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/LogoutAuthenticationFilter.java) | Implements/extends `BeanFilter`. Operations: `doFilterInternal`. |
| [NoopHttp200SuccessHandler](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/NoopHttp200SuccessHandler.java) | Implements/extends `AuthenticationSuccessHandler`. Operations: `onSuccess`. |
| [NoopHttp401FailureHandler](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/NoopHttp401FailureHandler.java) | Implements/extends `AuthenticationFailureHandler`. Operations: `onFailure`. |

#### `org.jmouse.security.web.authentication.bearer`

| Type | Responsibility / contract |
|---|---|
| [BearerTokenAuthenticationEntryPoint](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/bearer/BearerTokenAuthenticationEntryPoint.java) | 🪙 BearerTokenAuthenticationEntryPoint |
| [BearerTokenAuthenticationProvider](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/bearer/BearerTokenAuthenticationProvider.java) | Implements/extends `AbstractAuthenticationProvider`. |

#### `org.jmouse.security.web.authentication.form`

| Type | Responsibility / contract |
|---|---|
| [SubmitFormRequestAuthenticationFilter](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/form/SubmitFormRequestAuthenticationFilter.java) | Implements/extends `AbstractAuthenticationFilter`. Operations: `getUsernameParameter`, `setUsernameParameter`, `getPasswordParameter`, `setPasswordParameter`, `getAuthenticationProvider`, `setAuthenticationProvider`. |
| [SubmitFormRequestAuthenticationProvider](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/form/SubmitFormRequestAuthenticationProvider.java) | 📝 SubmitFormRequestAuthenticationProvider |

#### `org.jmouse.security.web.authentication.jwt`

| Type | Responsibility / contract |
|---|---|
| [JwtAuthenticationFilter](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/jwt/JwtAuthenticationFilter.java) | 🚪 Extracts Bearer token and delegates to AuthenticationManager. |

#### `org.jmouse.security.web.authentication.remember`

| Type | Responsibility / contract |
|---|---|
| [AutoLoginAuthenticationFilter](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/remember/AutoLoginAuthenticationFilter.java) | Implements/extends `AbstractAuthenticationFilter`. Operations: `getAuthenticationProvider`, `setAuthenticationProvider`. |
| [AutoLoginAuthenticationProvider](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/remember/AutoLoginAuthenticationProvider.java) | Implements/extends `AuthenticationProvider`. Operations: `provide`, `getCookieName`, `setCookieName`. |

#### `org.jmouse.security.web.authentication.ui`

| Type | Responsibility / contract |
|---|---|
| [DefaultLoginPageGeneratingFilter](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/ui/DefaultLoginPageGeneratingFilter.java) | Implements/extends `BeanFilter, HeaderWriter`. Operations: `doFilterInternal`, `writeHeaders`. |
| [FailureRedirectHandler](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/ui/FailureRedirectHandler.java) | Implements/extends `AuthenticationFailureHandler`. Operations: `onFailure`. |
| [LoginUrlAuthenticationEntryPoint](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/ui/LoginUrlAuthenticationEntryPoint.java) | Implements/extends `AuthenticationEntryPoint`. Operations: `initiate`, `toString`. |
| [SavedRequestAwareAuthenticationSuccessHandler](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/ui/SavedRequestAwareAuthenticationSuccessHandler.java) | Implements/extends `AuthenticationSuccessHandler`. Operations: `defaultTargetUrl`, `onSuccess`. |
| [SuccessRedirectHandler](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/ui/SuccessRedirectHandler.java) | Implements/extends `AuthenticationSuccessHandler`. Operations: `onSuccess`. |

#### `org.jmouse.security.web.authentication.www`

| Type | Responsibility / contract |
|---|---|
| [BasicAuthenticationEntryPoint](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/www/BasicAuthenticationEntryPoint.java) | 🔑 BasicAuthenticationEntryPoint |
| [BasicAuthenticationFilter](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/www/BasicAuthenticationFilter.java) | Implements/extends `AbstractAuthenticationFilter`. Operations: `getAuthenticationProvider`, `setAuthenticationProvider`. |
| [BasicAuthenticationProvider](../jmouse-web/src/main/java/org/jmouse/security/web/authentication/www/BasicAuthenticationProvider.java) | Implements/extends `AbstractAuthenticationProvider`. |

#### `org.jmouse.security.web.authorization`

| Type | Responsibility / contract |
|---|---|
| [AuthorizationFilter](../jmouse-web/src/main/java/org/jmouse/security/web/authorization/AuthorizationFilter.java) | Implements/extends `BeanFilter, AuthenticationInspector`. Operations: `doFilterInternal`. |
| [ForbiddenAuthorizationFailureHandler](../jmouse-web/src/main/java/org/jmouse/security/web/authorization/ForbiddenAuthorizationFailureHandler.java) | Implements/extends `AuthorizationFailureHandler`. Operations: `onFailure`. |

#### `org.jmouse.security.web.configuration`

| Type | Responsibility / contract |
|---|---|
| [AbstractConfiguredSecurityBuilder](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/AbstractConfiguredSecurityBuilder.java) | 🧩 AbstractConfiguredSecurityBuilder |
| [AbstractSecurityBuilder](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/AbstractSecurityBuilder.java) | 🏗️ AbstractSecurityBuilder |
| [AuthorizationAccessCriterion](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/AuthorizationAccessCriterion.java) | Operations: `not`, `permitAll`, `denyAll`, `anonymous`, `authenticated`, `hasAuthority`, `hasAnyAuthority`, `hasRole`, `hasAnyRole`, `hasVariable`, `access`, `equalTo`. |
| [ConfigurerAdapter](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/ConfigurerAdapter.java) | Implements/extends `SecurityConfigurer`. Operations: `getBuilder`, `setBuilder`, `getBeanProvider`, `setBeanProvider`. |
| [ContextVariables](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/ContextVariables.java) | Operations: `getValue`. |
| [HttpSecurityBuilder](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/HttpSecurityBuilder.java) | Implements/extends `SecurityBuilder`. Operations: `getObject`, `addFilter`, `addFilterAfter`, `addFilterBefore`, `getFilters`, `setSharedObject`, `getSharedObject`, `removeConfigurer`, `getConfigurer`, `chainMatcher`. |
| [HttpSecurityConfiguration](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/HttpSecurityConfiguration.java) | 🧱 Core web-security configuration for jMouse Security. |
| [HttpSecurityConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/HttpSecurityConfigurer.java) | Implements/extends `HttpSecurityBuilder > extends ConfigurerAdapter`. Operations: `disable`. |
| [SecurityBuilder](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/SecurityBuilder.java) | Build a configured security object; build() may report configuration failure by exception. |
| [SecurityConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/SecurityConfigurer.java) | ⚙️ SecurityConfigurer |
| [SharedAttributes](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/SharedAttributes.java) | Typed class keys for shared authentication manager, security repository, handlers, filter order, context strategy and request cache. |

#### `org.jmouse.security.web.configuration.builder`

| Type | Responsibility / contract |
|---|---|
| [HttpSecurity](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/builder/HttpSecurity.java) | Implements/extends `AbstractConfiguredSecurityBuilder implements HttpSecurityBuilder`. Operations: `addFilter`, `addFilterAfter`, `addFilterBefore`, `getFilters`, `chainMatcher`, `principalService`, `securityContext`, `sessionManagement`, `authorization`, `authentication`, `exceptionHandling`, `attach`. |

#### `org.jmouse.security.web.configuration.configurer`

| Type | Responsibility / contract |
|---|---|
| [AbstractAuthenticationConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/configurer/AbstractAuthenticationConfigurer.java) | 🔧 AbstractAuthenticationConfigurer |
| [AbstractSecurityContextConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/configurer/AbstractSecurityContextConfigurer.java) | Implements/extends `HttpSecurityConfigurer , B>`. Operations: `contextRepository`. |
| [AnonymousConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/configurer/AnonymousConfigurer.java) | Implements/extends `HttpSecurityConfigurer , B>`. Operations: `configure`. |
| [AuthenticationConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/configurer/AuthenticationConfigurer.java) | Implements/extends `HttpSecurityConfigurer , B>`. Operations: `anonymous`, `submitForm`, `autoLogin`, `httpBasic`, `jwt`. |
| [AuthorizeRequestConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/configurer/AuthorizeRequestConfigurer.java) | Implements/extends `HttpSecurityConfigurer , B>`. Operations: `getRequestMatchCriterion`, `addMapping`, `initialize`, `configure`, `createAuthorizationManager`, `roleHierarchy`, `rolePrefix`, `getRoleHierarchy`, `getRolePrefix`. |
| [AutoLoginConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/configurer/AutoLoginConfigurer.java) | Implements/extends `AbstractAuthenticationConfigurer >`. |
| [ExceptionHandlingConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/configurer/ExceptionHandlingConfigurer.java) | Implements/extends `HttpSecurityConfigurer , B>`. Operations: `authenticationEntryPoint`, `accessDeniedHandler`, `requestCache`, `configure`. |
| [HttpBasicConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/configurer/HttpBasicConfigurer.java) | Implements/extends `AbstractAuthenticationConfigurer >`. Operations: `realmName`, `disableChallengeOnFailure`, `enableChallengeOnFailure`. |
| [JwtTokenConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/configurer/JwtTokenConfigurer.java) | Implements/extends `AbstractAuthenticationConfigurer >`. Operations: `entryPoint`, `codec`, `realm`, `adapter`, `hs256`, `rs256`, `ed25519`, `es256`, `secret`, `skew`, `clock`, `build`. |
| [SecurityContextConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/configurer/SecurityContextConfigurer.java) | Implements/extends `AbstractSecurityContextConfigurer`. Operations: `configure`. |
| [SessionManagementConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/configurer/SessionManagementConfigurer.java) | Implements/extends `AbstractSecurityContextConfigurer`. Operations: `policy`, `sessionRegistry`, `maximumSessions`, `rewrite`, `sessionAuthenticationStrategy`, `invalidSessionStrategy`, `configure`, `enable`, `disable`, `stateless`, `always`, `ifPresent`. |
| [SubmitFormConfigurer](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/configurer/SubmitFormConfigurer.java) | Implements/extends `AbstractAuthenticationConfigurer >`. Operations: `disableDefaultLoginPage`, `usernamePassword`, `usernameParameter`, `passwordParameter`, `processing`, `authenticationProvider`, `configure`, `formAction`, `httpMethod`, `get`, `post`. |

#### `org.jmouse.security.web.configuration.feature`

| Type | Responsibility / contract |
|---|---|
| [AuthorizeMethodFeatureSelector](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/feature/AuthorizeMethodFeatureSelector.java) | 🔎 FeatureSelector that enables native jMouse method authorization infrastructure. |
| [Jsr250MethodSecurityFeatureSelector](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/feature/Jsr250MethodSecurityFeatureSelector.java) | 🔐 FeatureSelector for enabling JSR-250 method-level security. |
| [WebSecurityFeatureSelector](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/feature/WebSecurityFeatureSelector.java) | 🔐 FeatureSelector that enables core web security infrastructure. |

#### `org.jmouse.security.web.configuration.matching`

| Type | Responsibility / contract |
|---|---|
| [AbstractAuthorizationRequestMatchCriterion](../jmouse-web/src/main/java/org/jmouse/security/web/configuration/matching/AbstractAuthorizationRequestMatchCriterion.java) | Operations: `requestPath`, `anyRequest`, `noneRequest`, `dispatcherType`, `matcherCriteria`, `mappingMatcher`. |

#### `org.jmouse.security.web.context`

| Type | Responsibility / contract |
|---|---|
| [HttpSessionSecurityContextRepository](../jmouse-web/src/main/java/org/jmouse/security/web/context/HttpSessionSecurityContextRepository.java) | Implements/extends `SecurityContextRepository, AuthenticationInspector`. Operations: `sessionKey`, `allowSessionCreation`, `strategy`, `load`, `save`, `clear`, `contains`. |
| [InMemorySecurityContextRepository](../jmouse-web/src/main/java/org/jmouse/security/web/context/InMemorySecurityContextRepository.java) | Implements/extends `SecurityContextRepository`. Operations: `load`, `save`, `clear`, `contains`. |
| [NullSecurityContextRepository](../jmouse-web/src/main/java/org/jmouse/security/web/context/NullSecurityContextRepository.java) | Implements/extends `SecurityContextRepository`. Operations: `load`, `save`, `clear`, `contains`. |
| [SecurityContextPersistenceFilter](../jmouse-web/src/main/java/org/jmouse/security/web/context/SecurityContextPersistenceFilter.java) | Implements/extends `BeanFilter`. Operations: `doFilterInternal`. |
| [SecurityContextRepository](../jmouse-web/src/main/java/org/jmouse/security/web/context/SecurityContextRepository.java) | 🗄️ SecurityContextRepository |

#### `org.jmouse.security.web.jwt`

| Type | Responsibility / contract |
|---|---|
| [JacksonJsonMapper](../jmouse-web/src/main/java/org/jmouse/security/web/jwt/JacksonJsonMapper.java) | 🧩 Jackson-based JsonMapper adapter. |

#### `org.jmouse.security.web.request`

| Type | Responsibility / contract |
|---|---|
| [RequestAttributeSecurityContextRepository](../jmouse-web/src/main/java/org/jmouse/security/web/request/RequestAttributeSecurityContextRepository.java) | Implements/extends `SecurityContextRepository`. Operations: `load`, `save`, `clear`, `contains`. |

#### `org.jmouse.security.web.session`

| Type | Responsibility / contract |
|---|---|
| [ChangeSessionIdAuthenticateHandler](../jmouse-web/src/main/java/org/jmouse/security/web/session/ChangeSessionIdAuthenticateHandler.java) | Implements/extends `SessionAuthenticateHandler`. Operations: `onAuthentication`. |
| [CompositeSessionAuthenticateHandler](../jmouse-web/src/main/java/org/jmouse/security/web/session/CompositeSessionAuthenticateHandler.java) | Implements/extends `SessionAuthenticateHandler`. Operations: `onAuthentication`. |
| [DisableURLEncodingFilter](../jmouse-web/src/main/java/org/jmouse/security/web/session/DisableURLEncodingFilter.java) | Implements/extends `BeanFilter`. Operations: `doFilterInternal`, `encodeURL`, `encodeRedirectURL`. |
| [ForceEagerSessionCreationFilter](../jmouse-web/src/main/java/org/jmouse/security/web/session/ForceEagerSessionCreationFilter.java) | Implements/extends `BeanFilter`. Operations: `doFilterInternal`. |
| [HttpStatusInvalidSessionHandler](../jmouse-web/src/main/java/org/jmouse/security/web/session/HttpStatusInvalidSessionHandler.java) | Implements/extends `SessionInvalidHandler`. Operations: `onInvalidSession`. |
| [NullAuthenticatedSessionStrategy](../jmouse-web/src/main/java/org/jmouse/security/web/session/NullAuthenticatedSessionStrategy.java) | Implements/extends `SessionAuthenticateHandler`. Operations: `onAuthentication`. |
| [RedirectInvalidSessionHandler](../jmouse-web/src/main/java/org/jmouse/security/web/session/RedirectInvalidSessionHandler.java) | Implements/extends `SessionInvalidHandler`. Operations: `onInvalidSession`. |
| [RegisterNewSessionAuthenticateHandler](../jmouse-web/src/main/java/org/jmouse/security/web/session/RegisterNewSessionAuthenticateHandler.java) | Implements/extends `SessionAuthenticateHandler`. Operations: `onAuthentication`. |
| [SessionAuthenticateHandler](../jmouse-web/src/main/java/org/jmouse/security/web/session/SessionAuthenticateHandler.java) | Operations: `onAuthentication`. |
| [SessionControlFilter](../jmouse-web/src/main/java/org/jmouse/security/web/session/SessionControlFilter.java) | Implements/extends `BeanFilter`. Operations: `doFilterInternal`. |
| [SessionCreationPolicy](../jmouse-web/src/main/java/org/jmouse/security/web/session/SessionCreationPolicy.java) | 🗝️ SessionCreationPolicy |
| [SessionExpirationAuthenticateHandler](../jmouse-web/src/main/java/org/jmouse/security/web/session/SessionExpirationAuthenticateHandler.java) | ⏳ SessionExpirationAuthenticateHandler |
| [SessionInvalidHandler](../jmouse-web/src/main/java/org/jmouse/security/web/session/SessionInvalidHandler.java) | React to an invalid session through onInvalidSession(RequestContextKeeper). |
| [SessionManagementFilter](../jmouse-web/src/main/java/org/jmouse/security/web/session/SessionManagementFilter.java) | Implements/extends `BeanFilter`. Operations: `doFilterInternal`. |
| [SessionPersistenceResponseWrapper](../jmouse-web/src/main/java/org/jmouse/security/web/session/SessionPersistenceResponseWrapper.java) | Implements/extends `OnCommitResponseWrapper`. Operations: `sendError`, `sendRedirect`. |
| [SessionSettings](../jmouse-web/src/main/java/org/jmouse/security/web/session/SessionSettings.java) | Operations: `of`, `policy`, `urlRewriting`, `sessionAuthStrategy`, `invalidSessionStrategy`. |

#### `org.jmouse.web`

| Type | Responsibility / contract |
|---|---|
| [Launcher](../jmouse-web/src/main/java/org/jmouse/web/Launcher.java) | 🚀 Base contract for launching an application with a configurable BeanContext. |
| [WebApplicationContextBuilder](../jmouse-web/src/main/java/org/jmouse/web/WebApplicationContextBuilder.java) | 🔧 Builder for creating and customizing WebBeanContext instances. |
| [WebApplicationFactory](../jmouse-web/src/main/java/org/jmouse/web/WebApplicationFactory.java) | 🕸️ Factory class for creating and configuring WebBeanContext instances in a web-based jMouse application. |
| [WebContextBuilder](../jmouse-web/src/main/java/org/jmouse/web/WebContextBuilder.java) | 🧰 Fluent builder for constructing WebBeanContext instances. |
| [WebLauncher](../jmouse-web/src/main/java/org/jmouse/web/WebLauncher.java) | 🚀 Entry point contract for launching a web application. |

#### `org.jmouse.web.annotation`

| Type | Responsibility / contract |
|---|---|
| [Controller](../jmouse-web/src/main/java/org/jmouse/web/annotation/Controller.java) | Annotation contract; members and defaults are defined in this source. |
| [ExceptionHandler](../jmouse-web/src/main/java/org/jmouse/web/annotation/ExceptionHandler.java) | Annotation contract; members and defaults are defined in this source. |
| [GetMapping](../jmouse-web/src/main/java/org/jmouse/web/annotation/GetMapping.java) | Annotation contract; members and defaults are defined in this source. |
| [Header](../jmouse-web/src/main/java/org/jmouse/web/annotation/Header.java) | 📬 Annotation for mapping a route based on a specific HTTP header and its value. |
| [Mapping](../jmouse-web/src/main/java/org/jmouse/web/annotation/Mapping.java) | 🗺️ Core annotation for declaring route mappings in jMouse MVC. |
| [MethodDescription](../jmouse-web/src/main/java/org/jmouse/web/annotation/MethodDescription.java) | Annotation contract; members and defaults are defined in this source. |
| [PathVariable](../jmouse-web/src/main/java/org/jmouse/web/annotation/PathVariable.java) | 🧩 Binds a URI path variable to a method parameter. |
| [PostMapping](../jmouse-web/src/main/java/org/jmouse/web/annotation/PostMapping.java) | Annotation contract; members and defaults are defined in this source. |
| [QueryParameter](../jmouse-web/src/main/java/org/jmouse/web/annotation/QueryParameter.java) | 🔎 Annotation for mapping a route based on a query parameter and its expected value. |
| [RequestHeader](../jmouse-web/src/main/java/org/jmouse/web/annotation/RequestHeader.java) | 📑 Bind an HTTP request header to a controller method parameter. |
| [RequestMethod](../jmouse-web/src/main/java/org/jmouse/web/annotation/RequestMethod.java) | 🚦 Bind the HTTP request method to a controller method parameter. |
| [RequestParameter](../jmouse-web/src/main/java/org/jmouse/web/annotation/RequestParameter.java) | Annotation contract; members and defaults are defined in this source. |
| [StatusCode](../jmouse-web/src/main/java/org/jmouse/web/annotation/StatusCode.java) | 📡 Bind the current HTTP status code to a controller method parameter. |
| [ViewMapping](../jmouse-web/src/main/java/org/jmouse/web/annotation/ViewMapping.java) | 🖼️ Maps a controller method to a named view. |

#### `org.jmouse.web.binding`

| Type | Responsibility / contract |
|---|---|
| [BindingContext](../jmouse-web/src/main/java/org/jmouse/web/binding/BindingContext.java) | Value record: `( Errors errors, String objectName, Hints hints )`. |
| [BindingError](../jmouse-web/src/main/java/org/jmouse/web/binding/BindingError.java) | Value record: `( MappingException error, PropertyPath path )`. |
| [BindingException](../jmouse-web/src/main/java/org/jmouse/web/binding/BindingException.java) | Implements/extends `RuntimeException`. Operations: `getErrors`. |
| [BindingFinish](../jmouse-web/src/main/java/org/jmouse/web/binding/BindingFinish.java) | Value record: `( Object target )`. |
| [BindingMappingPlugin](../jmouse-web/src/main/java/org/jmouse/web/binding/BindingMappingPlugin.java) | Implements/extends `MappingPlugin`. Operations: `onStart`, `onValue`, `onFinish`, `onError`. |
| [BindingProcessor](../jmouse-web/src/main/java/org/jmouse/web/binding/BindingProcessor.java) | Binding lifecycle hooks: onStart, onValue, onFinish and onError; the default value hook preserves the current value. |
| [BindingResult](../jmouse-web/src/main/java/org/jmouse/web/binding/BindingResult.java) | Operations: `target`, `errors`. |
| [BindingSession](../jmouse-web/src/main/java/org/jmouse/web/binding/BindingSession.java) | Operations: `errors`, `objectName`, `hints`. |
| [BindingStart](../jmouse-web/src/main/java/org/jmouse/web/binding/BindingStart.java) | Value record: `( Object sourceRoot, Object targetRoot )`. |
| [BindingValue](../jmouse-web/src/main/java/org/jmouse/web/binding/BindingValue.java) | Value record: `( Object current, MappingDestination destination, PropertyPath path )`. |
| [DefaultMappingFailureTranslator](../jmouse-web/src/main/java/org/jmouse/web/binding/DefaultMappingFailureTranslator.java) | Implements/extends `MappingFailureTranslator`. Operations: `translate`. |
| [ErrorsMerging](../jmouse-web/src/main/java/org/jmouse/web/binding/ErrorsMerging.java) | Operations: `merge`. |
| [FailureToErrorsProcessor](../jmouse-web/src/main/java/org/jmouse/web/binding/FailureToErrorsProcessor.java) | Implements/extends `BindingProcessor`. Operations: `onError`. |
| [MappingFailureTranslator](../jmouse-web/src/main/java/org/jmouse/web/binding/MappingFailureTranslator.java) | Operations: `translate`. |
| [ParametersDataBinder](../jmouse-web/src/main/java/org/jmouse/web/binding/ParametersDataBinder.java) | Operations: `bind`. |
| [SimpleBindingResult](../jmouse-web/src/main/java/org/jmouse/web/binding/SimpleBindingResult.java) | Value record: `(T target, Errors errors) implements BindingResult`. |
| [ValidationBindingProcessor](../jmouse-web/src/main/java/org/jmouse/web/binding/ValidationBindingProcessor.java) | Implements/extends `BindingProcessor`. Operations: `onFinish`. |
| [ValidationHintsSupplier](../jmouse-web/src/main/java/org/jmouse/web/binding/ValidationHintsSupplier.java) | Operations: `get`. |
| [WebModel](../jmouse-web/src/main/java/org/jmouse/web/binding/WebModel.java) | Annotation contract; members and defaults are defined in this source. |

#### `org.jmouse.web.binding.validation`

| Type | Responsibility / contract |
|---|---|
| [AnnotationValidationProcessor](../jmouse-web/src/main/java/org/jmouse/web/binding/validation/AnnotationValidationProcessor.java) | Implements/extends `BindingProcessor`. Operations: `onValue`, `onFinish`, `defaults`. |
| [ReflectionValidationAnnotationReader](../jmouse-web/src/main/java/org/jmouse/web/binding/validation/ReflectionValidationAnnotationReader.java) | Implements/extends `ValidationAnnotationReader`. Operations: `findOnDestination`, `findOnTargetClass`. |
| [Validate](../jmouse-web/src/main/java/org/jmouse/web/binding/validation/Validate.java) | Annotation contract; members and defaults are defined in this source. |
| [ValidationAnnotationReader](../jmouse-web/src/main/java/org/jmouse/web/binding/validation/ValidationAnnotationReader.java) | Operations: `findOnDestination`, `findOnTargetClass`. |
| [ValidationHintsResolver](../jmouse-web/src/main/java/org/jmouse/web/binding/validation/ValidationHintsResolver.java) | Operations: `resolve`. |

#### `org.jmouse.web.context`

| Type | Responsibility / contract |
|---|---|
| [AbstractWebApplicationBeanContext](../jmouse-web/src/main/java/org/jmouse/web/context/AbstractWebApplicationBeanContext.java) | 🧩 Abstract base implementation of WebBeanContext that integrates the servlet environment with the jMouse application context. |
| [RequestAttributesBeanContainer](../jmouse-web/src/main/java/org/jmouse/web/context/RequestAttributesBeanContainer.java) | BeanContainer implementation that stores beans in the current HTTP request scope. Each bean is stored as a request attribute under a unique key to avoid name collisions. |
| [WebApplicationBeanContext](../jmouse-web/src/main/java/org/jmouse/web/context/WebApplicationBeanContext.java) | 🌐 Specialized bean context for web environments. |
| [WebBeanContext](../jmouse-web/src/main/java/org/jmouse/web/context/WebBeanContext.java) | 🌐 Web-specific BeanContext. |
| [WebContextException](../jmouse-web/src/main/java/org/jmouse/web/context/WebContextException.java) | A custom exception for web-related HTTP errors. |

#### `org.jmouse.web.factories`

| Type | Responsibility / contract |
|---|---|
| [ExpressionLanguageI18nConfiguration](../jmouse-web/src/main/java/org/jmouse/web/factories/ExpressionLanguageI18nConfiguration.java) | Operations: `i18nExtension`. |
| [WebParametersDataBinderConfiguration](../jmouse-web/src/main/java/org/jmouse/web/factories/WebParametersDataBinderConfiguration.java) | Operations: `parametersDataBinder`, `bindingMappingPlugin`. |
| [WebServerFactoryConfiguration](../jmouse-web/src/main/java/org/jmouse/web/factories/WebServerFactoryConfiguration.java) | Operations: `createWebServerFactory`, `setWebServerConfigs`, `getWebServerConfig`, `getDefaultWebServer`, `setDefaultWebServer`, `getDefaultWebServerConfiguration`. |

#### `org.jmouse.web.http`

| Type | Responsibility / contract |
|---|---|
| [ExceptionHolder](../jmouse-web/src/main/java/org/jmouse/web/http/ExceptionHolder.java) | 💥 Thread-local holder for exceptions during request processing. |
| [HeaderWriter](../jmouse-web/src/main/java/org/jmouse/web/http/HeaderWriter.java) | Operations: `writeHeaders`. |
| [QueryParameters](../jmouse-web/src/main/java/org/jmouse/web/http/QueryParameters.java) | 📦 Immutable wrapper for HTTP query parameters. |
| [RequestAttributes](../jmouse-web/src/main/java/org/jmouse/web/http/RequestAttributes.java) | Operations: `getAttribute`, `setAttribute`, `removeAttribute`, `ofRequest`. |
| [RequestAttributesHolder](../jmouse-web/src/main/java/org/jmouse/web/http/RequestAttributesHolder.java) | 🔒 Thread-bound holder for RequestAttributes and request-scoped metadata. |
| [RequestContext](../jmouse-web/src/main/java/org/jmouse/web/http/RequestContext.java) | 🌐 Immutable wrapper for the current HTTP request and response. |
| [RequestContextKeeper](../jmouse-web/src/main/java/org/jmouse/web/http/RequestContextKeeper.java) | 📦 RequestContextKeeper |
| [RequestHeaders](../jmouse-web/src/main/java/org/jmouse/web/http/RequestHeaders.java) | 📦 Represents HTTP headers of a request as a structured Headers container. |
| [RequestParameters](../jmouse-web/src/main/java/org/jmouse/web/http/RequestParameters.java) | Value record: `(Map parameters)`. |
| [RequestPath](../jmouse-web/src/main/java/org/jmouse/web/http/RequestPath.java) | 🧭 Represents the decomposed path components of an HTTP servlet request. |
| [RequestRoute](../jmouse-web/src/main/java/org/jmouse/web/http/RequestRoute.java) | 🛬 Represents an incoming HTTP request for routing and matching purposes. |
| [SessionAttributesHolder](../jmouse-web/src/main/java/org/jmouse/web/http/SessionAttributesHolder.java) | A holder class that stores RequestAttributes representing session attributes in a thread-local variable. |
| [WebHttpRequest](../jmouse-web/src/main/java/org/jmouse/web/http/WebHttpRequest.java) | 🛡️ Wrapper around HttpServletRequest implementing WebRequest. |
| [WebHttpSession](../jmouse-web/src/main/java/org/jmouse/web/http/WebHttpSession.java) | Adapter that exposes HttpSession operations behind the WebSession API, backed by the current HttpServletRequest. |
| [WebRequest](../jmouse-web/src/main/java/org/jmouse/web/http/WebRequest.java) | 🌐 Abstraction over a web request. |
| [WebSession](../jmouse-web/src/main/java/org/jmouse/web/http/WebSession.java) | Implements/extends `RequestAttributes, Session`. Operations: `getSession`, `getCurrentSession`. |

#### `org.jmouse.web.http.cache`

| Type | Responsibility / contract |
|---|---|
| [HttpSessionRequestCache](../jmouse-web/src/main/java/org/jmouse/web/http/cache/HttpSessionRequestCache.java) | 💾 HttpSessionRequestCache |
| [RequestCache](../jmouse-web/src/main/java/org/jmouse/web/http/cache/RequestCache.java) | Operations: `saveRequest`, `getRequest`, `removeRequest`. |
| [SavedRequest](../jmouse-web/src/main/java/org/jmouse/web/http/cache/SavedRequest.java) | Implements/extends `Serializable`. Operations: `getMethod`, `getRequestURI`, `getQueryParameters`, `getHeaders`, `getRedirectUrl`. |

#### `org.jmouse.web.http.multipart`

| Type | Responsibility / contract |
|---|---|
| [AbstractMultipartWebHttpRequest](../jmouse-web/src/main/java/org/jmouse/web/http/multipart/AbstractMultipartWebHttpRequest.java) | Implements/extends `WebHttpRequest implements MultipartWebRequest`. Operations: `getMultipartFiles`, `setMultipartFiles`, `getFiles`, `getFileContentType`. |
| [MultipartConfigFactory](../jmouse-web/src/main/java/org/jmouse/web/http/multipart/MultipartConfigFactory.java) | Operations: `getFileSizeThreshold`, `setFileSizeThreshold`, `getMaxFileSize`, `setMaxFileSize`, `getMaxRequestSize`, `setMaxRequestSize`, `getLocation`, `setLocation`, `createMultipartConfig`. |
| [MultipartFile](../jmouse-web/src/main/java/org/jmouse/web/http/multipart/MultipartFile.java) | 📦 Represents an uploaded multipart file. |
| [MultipartProperties](../jmouse-web/src/main/java/org/jmouse/web/http/multipart/MultipartProperties.java) | Operations: `isEnabled`, `setEnabled`, `getFileSizeThreshold`, `setFileSizeThreshold`, `getMaxFileSize`, `setMaxFileSize`, `getMaxRequestSize`, `setMaxRequestSize`, `getLocation`, `setLocation`, `getMultipartConfig`. |
| [MultipartRequestException](../jmouse-web/src/main/java/org/jmouse/web/http/multipart/MultipartRequestException.java) | Implements/extends `RuntimeException`. |
| [MultipartResolver](../jmouse-web/src/main/java/org/jmouse/web/http/multipart/MultipartResolver.java) | 📂 Strategy for resolving multipart requests. |
| [MultipartWebHttpRequest](../jmouse-web/src/main/java/org/jmouse/web/http/multipart/MultipartWebHttpRequest.java) | 📦 Specialized HTTP request wrapper for handling multipart/form-data requests. |
| [MultipartWebRequest](../jmouse-web/src/main/java/org/jmouse/web/http/multipart/MultipartWebRequest.java) | 📂 Abstraction for accessing multipart request data. |
| [SimpleMultipartResolver](../jmouse-web/src/main/java/org/jmouse/web/http/multipart/SimpleMultipartResolver.java) | 📂 Simple MultipartResolver implementation. |
| [UploadedFile](../jmouse-web/src/main/java/org/jmouse/web/http/multipart/UploadedFile.java) | 📎 Simple implementation of MultipartFile backed by a servlet Part. |
| [UploadLimitExceededException](../jmouse-web/src/main/java/org/jmouse/web/http/multipart/UploadLimitExceededException.java) | Implements/extends `MultipartRequestException`. |

#### `org.jmouse.web.http.response`

| Type | Responsibility / contract |
|---|---|
| [HeadersBuffer](../jmouse-web/src/main/java/org/jmouse/web/http/response/HeadersBuffer.java) | 📑 Contract for buffering and writing HTTP headers into a HttpServletResponse. |
| [HttpServletHeadersBuffer](../jmouse-web/src/main/java/org/jmouse/web/http/response/HttpServletHeadersBuffer.java) | 📨 Buffer for HTTP headers before committing them to a HttpServletResponse. |

#### `org.jmouse.web.http.values`

| Type | Responsibility / contract |
|---|---|
| [HeaderValueProvider](../jmouse-web/src/main/java/org/jmouse/web/http/values/HeaderValueProvider.java) | Implements/extends `RequestValueProvider`. Operations: `getTargetName`, `getRequestValues`. |
| [QueryStringValueProvider](../jmouse-web/src/main/java/org/jmouse/web/http/values/QueryStringValueProvider.java) | Implements/extends `RequestValueProvider`. Operations: `getTargetName`, `getRequestValues`. |
| [RequestPathValueProvider](../jmouse-web/src/main/java/org/jmouse/web/http/values/RequestPathValueProvider.java) | Implements/extends `RequestValueProvider`. Operations: `getTargetName`, `getRequestValues`. |
| [RequestValueProvider](../jmouse-web/src/main/java/org/jmouse/web/http/values/RequestValueProvider.java) | Operations: `getTargetName`, `getRequestValues`. |
| [RequestValues](../jmouse-web/src/main/java/org/jmouse/web/http/values/RequestValues.java) | Operations: `getProviders`, `getStringMap`, `defaults`. |

#### `org.jmouse.web.http.writers`

| Type | Responsibility / contract |
|---|---|
| [AbstractHeaderWriter](../jmouse-web/src/main/java/org/jmouse/web/http/writers/AbstractHeaderWriter.java) | Implements/extends `HeaderWriter`. Operations: `writeHeaders`. |
| [WWWAuthenticateWriter](../jmouse-web/src/main/java/org/jmouse/web/http/writers/WWWAuthenticateWriter.java) | Implements/extends `AbstractHeaderWriter`. |

#### `org.jmouse.web.initializer`

| Type | Responsibility / contract |
|---|---|
| [ServletContextRegistrations](../jmouse-web/src/main/java/org/jmouse/web/initializer/ServletContextRegistrations.java) | Resolves all servlet-related WebApplicationInitializer instances from the bean context, including predefined listeners for request and web bean scopes. |
| [WebApplicationInitializer](../jmouse-web/src/main/java/org/jmouse/web/initializer/WebApplicationInitializer.java) | Interface for initializing a web application. Implementations provide custom startup logic for configuring the ServletContext. |
| [WebApplicationInitializerProvider](../jmouse-web/src/main/java/org/jmouse/web/initializer/WebApplicationInitializerProvider.java) | Provides access to registered WebApplicationInitializer beans with filtering capabilities based on type. |

#### `org.jmouse.web.initializer.context`

| Type | Responsibility / contract |
|---|---|
| [StartupApplicationContextInitializer](../jmouse-web/src/main/java/org/jmouse/web/initializer/context/StartupApplicationContextInitializer.java) | 🧩 Default initializer for bootstrapping a WebBeanContext. |

#### `org.jmouse.web.match`

| Type | Responsibility / contract |
|---|---|
| [AntPattern](../jmouse-web/src/main/java/org/jmouse/web/match/AntPattern.java) | 🐜 Ant-style route matched implementation. |
| [PathContainer](../jmouse-web/src/main/java/org/jmouse/web/match/PathContainer.java) | 🧭 Abstraction for a parsed request path. |
| [PathPattern](../jmouse-web/src/main/java/org/jmouse/web/match/PathPattern.java) | 🛣️ Abstraction for a route path matched. |
| [PathPatternCompiler](../jmouse-web/src/main/java/org/jmouse/web/match/PathPatternCompiler.java) | 🏭 Factory for creating PathPattern instances. |
| [RegexpPathPattern](../jmouse-web/src/main/java/org/jmouse/web/match/RegexpPathPattern.java) | 🧭 Parses path patterns like /user/{id:\d+}/status/{status} into regular expressions and extracts matched parameters into RouteMatch. |
| [Route](../jmouse-web/src/main/java/org/jmouse/web/match/Route.java) | 🧭 Immutable route definition used for mapping requests to handlers. |
| [RouteMatch](../jmouse-web/src/main/java/org/jmouse/web/match/RouteMatch.java) | 🎯 Matched route with extracted URI variables. |
| [SimplePathContainer](../jmouse-web/src/main/java/org/jmouse/web/match/SimplePathContainer.java) | 🧭 Default PathContainer implementation that parses a path into a sequence of Elements (separators and path segments with optional matrix parameters). |
| [SimplePathPattern](../jmouse-web/src/main/java/org/jmouse/web/match/SimplePathPattern.java) | 🧭 Minimal route matched implementation that plugs into PathPattern. |

#### `org.jmouse.web.match.routing`

| Type | Responsibility / contract |
|---|---|
| [MappingCriteria](../jmouse-web/src/main/java/org/jmouse/web/match/routing/MappingCriteria.java) | 🧭 A specialized MatcherCriteria that encapsulates all matching logic derived from a concrete Route definition. |
| [MappingMatcher](../jmouse-web/src/main/java/org/jmouse/web/match/routing/MappingMatcher.java) | 🧭 Unified contract for route-level matchers that both evaluate and compare HTTP routing conditions in the jMouse framework. |
| [MappingRegistration](../jmouse-web/src/main/java/org/jmouse/web/match/routing/MappingRegistration.java) | 📌 Represents a single route registration containing its MappingCriteria and handler. |
| [MappingRegistry](../jmouse-web/src/main/java/org/jmouse/web/match/routing/MappingRegistry.java) | 🗂️ Registry for mappings of type M to their registrations of type T. |
| [MatcherCriteria](../jmouse-web/src/main/java/org/jmouse/web/match/routing/MatcherCriteria.java) | 🧭 Represents a composite matcher used to evaluate whether a given RequestRoute satisfies a set of matching conditions such as HTTP method, path, headers, query parameters, and media types. |

#### `org.jmouse.web.match.routing.condition`

| Type | Responsibility / contract |
|---|---|
| [AnyCondition](../jmouse-web/src/main/java/org/jmouse/web/match/routing/condition/AnyCondition.java) | ✅ A MappingMatcher that always matches any route. |
| [ConsumesMatcher](../jmouse-web/src/main/java/org/jmouse/web/match/routing/condition/ConsumesMatcher.java) | 📥 Verifies whether the request's Content-Type matches any of the configured MediaTypes. |
| [ContextPathMatcher](../jmouse-web/src/main/java/org/jmouse/web/match/routing/condition/ContextPathMatcher.java) | 🧭 Context-path-based condition for matching routes. |
| [DispatcherTypeMatcher](../jmouse-web/src/main/java/org/jmouse/web/match/routing/condition/DispatcherTypeMatcher.java) | 🚦 Matches a request by its DispatcherType (REQUEST, FORWARD, INCLUDE, ASYNC, ERROR). |
| [HttpHeaderMatcher](../jmouse-web/src/main/java/org/jmouse/web/match/routing/condition/HttpHeaderMatcher.java) | 📡 Matches a specific HTTP header and its expected value. |
| [HttpMethodMatcher](../jmouse-web/src/main/java/org/jmouse/web/match/routing/condition/HttpMethodMatcher.java) | 📦 HTTP method matcher condition. |
| [NoneCondition](../jmouse-web/src/main/java/org/jmouse/web/match/routing/condition/NoneCondition.java) | 🚫 A MappingMatcher that never matches any route. |
| [ProducesMatcher](../jmouse-web/src/main/java/org/jmouse/web/match/routing/condition/ProducesMatcher.java) | 🎯 Matches a request's Accept header against route's produces media types. |
| [QueryParameterMatcher](../jmouse-web/src/main/java/org/jmouse/web/match/routing/condition/QueryParameterMatcher.java) | 🧭 Matches an HTTP query parameter to a required value. |
| [RequestPathMatcher](../jmouse-web/src/main/java/org/jmouse/web/match/routing/condition/RequestPathMatcher.java) | 🛣️ Path-based condition for matching routes. |
| [ServletPathMatcher](../jmouse-web/src/main/java/org/jmouse/web/match/routing/condition/ServletPathMatcher.java) | 🚏 Servlet-mapping-based condition for matching routes. |

#### `org.jmouse.web.mvc`

| Type | Responsibility / contract |
|---|---|
| [AbstractExceptionResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/AbstractExceptionResolver.java) | ⚠️ Base class for ExceptionResolver implementations. |
| [AbstractHandlerMapping](../jmouse-web/src/main/java/org/jmouse/web/mvc/AbstractHandlerMapping.java) | 🧭 Base class for handler mappings. |
| [AbstractHandlerMethodAdapter](../jmouse-web/src/main/java/org/jmouse/web/mvc/AbstractHandlerMethodAdapter.java) | ⚙️ Abstract base class for implementing HandlerAdapters. Provides a consistent flow for: |
| [AbstractHandlerPathMapping](../jmouse-web/src/main/java/org/jmouse/web/mvc/AbstractHandlerPathMapping.java) | 🧭 Abstract base class for route-based handler mappings. |
| [ArgumentResolverException](../jmouse-web/src/main/java/org/jmouse/web/mvc/ArgumentResolverException.java) | Implements/extends `RuntimeException`. |
| [BeanConfigurer](../jmouse-web/src/main/java/org/jmouse/web/mvc/BeanConfigurer.java) | ⚙️ Initializes MVC-related objects during context bootstrapping. |
| [DefaultModel](../jmouse-web/src/main/java/org/jmouse/web/mvc/DefaultModel.java) | 🗃️ Default Model implementation backed by a LinkedHashMap. |
| [DelegatingBeanFilterRegistration](../jmouse-web/src/main/java/org/jmouse/web/mvc/DelegatingBeanFilterRegistration.java) | 🎛️ Special FilterRegistrationBean for registering a DelegatingBeanFilter with a canonical, prefixed name and enabling it by default. |
| [EnvironmentInitializer](../jmouse-web/src/main/java/org/jmouse/web/mvc/EnvironmentInitializer.java) | Implements/extends `BeanConfigurer`. Operations: `configure`. |
| [ETagGenerator](../jmouse-web/src/main/java/org/jmouse/web/mvc/ETagGenerator.java) | ETag generator based on content length and last-modified timestamp. |
| [ETagProvider](../jmouse-web/src/main/java/org/jmouse/web/mvc/ETagProvider.java) | Operations: `provide`. |
| [ExceptionResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/ExceptionResolver.java) | ❗ Handles exceptions thrown during handler execution. |
| [ExecutionState](../jmouse-web/src/main/java/org/jmouse/web/mvc/ExecutionState.java) | Enum values: `HANDLED`, `UNHANDLED`. |
| [FrameworkDispatcher](../jmouse-web/src/main/java/org/jmouse/web/mvc/FrameworkDispatcher.java) | 🚦 Central dispatcher for the jMouse MVC framework. |
| [FrameworkDispatcherRegistration](../jmouse-web/src/main/java/org/jmouse/web/mvc/FrameworkDispatcherRegistration.java) | 🌀 Registers the core FrameworkDispatcher servlet. |
| [Handler](../jmouse-web/src/main/java/org/jmouse/web/mvc/Handler.java) | ⚙️ Encapsulates the execution context for a matched handler, including a list of HandlerInterceptor interceptors and the handler itself. |
| [HandlerAdapter](../jmouse-web/src/main/java/org/jmouse/web/mvc/HandlerAdapter.java) | 🧩 Strategy interface for invoking a handler (e.g., handler) within the jMouse MVC framework. |
| [HandlerAdapterException](../jmouse-web/src/main/java/org/jmouse/web/mvc/HandlerAdapterException.java) | Implements/extends `RuntimeException`. |
| [HandlerDispatcher](../jmouse-web/src/main/java/org/jmouse/web/mvc/HandlerDispatcher.java) | 🧭 Core dispatcher responsible for delegating HTTP requests to appropriate handlers. |
| [HandlerInterceptor](../jmouse-web/src/main/java/org/jmouse/web/mvc/HandlerInterceptor.java) | 🛡️ Contract for intercepting execution of a handler (e.g., handler) in the jMouse MVC framework. |
| [HandlerInterceptorRegistry](../jmouse-web/src/main/java/org/jmouse/web/mvc/HandlerInterceptorRegistry.java) | 📦 Registry for storing HandlerInterceptor instances. |
| [HandlerMapping](../jmouse-web/src/main/java/org/jmouse/web/mvc/HandlerMapping.java) | 🔗 Strategy interface used to map an incoming HttpServletRequest to a corresponding handler object that can process the request. |
| [HandlerMappingException](../jmouse-web/src/main/java/org/jmouse/web/mvc/HandlerMappingException.java) | Implements/extends `RuntimeException`. |
| [HandlerMethodWebResponder](../jmouse-web/src/main/java/org/jmouse/web/mvc/HandlerMethodWebResponder.java) | Implements/extends `WebResponder`. Operations: `applyPreconditions`, `writeHeaders`, `getHeaders`, `cleanupHeaders`. |
| [InternalFrameworkController](../jmouse-web/src/main/java/org/jmouse/web/mvc/InternalFrameworkController.java) | 🧰 Built-in MVC fallback controller for framework-level errors. |
| [LastModifiedProvider](../jmouse-web/src/main/java/org/jmouse/web/mvc/LastModifiedProvider.java) | Operations: `provide`. |
| [MappedHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/MappedHandler.java) | 🔍 Represents a resolved handler along with its route mapping metadata. |
| [MappingResult](../jmouse-web/src/main/java/org/jmouse/web/mvc/MappingResult.java) | Operations: `match`, `route`, `Default`. |
| [MatchedRoute](../jmouse-web/src/main/java/org/jmouse/web/mvc/MatchedRoute.java) | Value record: `( boolean matched, H handler, Route route, RouteMatch routeMatch )`. |
| [MethodNotAllowedException](../jmouse-web/src/main/java/org/jmouse/web/mvc/MethodNotAllowedException.java) | ❌ Exception indicating HTTP 405 Method Not Allowed. |
| [MethodNotAllowedExceptionResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/MethodNotAllowedExceptionResolver.java) | Resolves MethodNotAllowedException into an HTTP 405 Method Not Allowed response. |
| [Model](../jmouse-web/src/main/java/org/jmouse/web/mvc/Model.java) | Represents a container for model attributes used in view rendering 🧩 |
| [MVCResult](../jmouse-web/src/main/java/org/jmouse/web/mvc/MVCResult.java) | 🎯 Represents the outcome of invoking an MVC handler. |
| [NotFoundException](../jmouse-web/src/main/java/org/jmouse/web/mvc/NotFoundException.java) | Implements/extends `RuntimeException`. |
| [OptionsRequestHttpHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/OptionsRequestHttpHandler.java) | HTTP OPTIONS handler that responds with 204 No Content and an Allow header listing supported methods for the target resource. |
| [RouteMappedHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/RouteMappedHandler.java) | 🧭 A MappedHandler that encapsulates a route match, the associated route, and the actual handler to invoke. |
| [Service](../jmouse-web/src/main/java/org/jmouse/web/mvc/Service.java) | Value record: `(String name)`. |
| [ServletDispatcherConfiguration](../jmouse-web/src/main/java/org/jmouse/web/mvc/ServletDispatcherConfiguration.java) | Operations: `defaultDispatcher`, `sessionInitializer`, `requestContextListener`, `webBeanContextListener`, `sessionServletFilterRegistration`, `requestWrapperFilterRegistration`, `firewallRequestFilterRegistration`. |
| [ServletDispatcherException](../jmouse-web/src/main/java/org/jmouse/web/mvc/ServletDispatcherException.java) | Implements/extends `IOException`. |
| [ServletDispatcherProperties](../jmouse-web/src/main/java/org/jmouse/web/mvc/ServletDispatcherProperties.java) | Operations: `isEnabled`, `setEnabled`, `getMappings`, `setMappings`, `getLoadOnStartup`, `setLoadOnStartup`, `getName`, `setName`. |
| [SimpleMappedHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/SimpleMappedHandler.java) | ⚙️ Simple implementation of MappedHandler that holds a single handler instance. |
| [TooManyRequestsException](../jmouse-web/src/main/java/org/jmouse/web/mvc/TooManyRequestsException.java) | Implements/extends `RuntimeException`. |
| [View](../jmouse-web/src/main/java/org/jmouse/web/mvc/View.java) | 🖼️ Interface representing a view that can render output to a HTTP response. |
| [ViewProperties](../jmouse-web/src/main/java/org/jmouse/web/mvc/ViewProperties.java) | 🧾 Defines configurable view path settings. |
| [ViewResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/ViewResolver.java) | 🧩 Strategy interface for resolving a View by its logical path. |
| [WebApplicationLauncher](../jmouse-web/src/main/java/org/jmouse/web/mvc/WebApplicationLauncher.java) | 🚀 Default implementation of WebLauncher for bootstrapping a full web MVC application. |
| [WebContentSupport](../jmouse-web/src/main/java/org/jmouse/web/mvc/WebContentSupport.java) | 🔧 Base support for web content responses. |
| [WebMvcComponents](../jmouse-web/src/main/java/org/jmouse/web/mvc/WebMvcComponents.java) | 🧩 Defines core MVC beans for the jMouse framework. |
| [WebResponder](../jmouse-web/src/main/java/org/jmouse/web/mvc/WebResponder.java) | 🛠️ Generalized responder for HTTP responses, inspired by Spring's WebContentGenerator. |

#### `org.jmouse.web.mvc.adapter`

| Type | Responsibility / contract |
|---|---|
| [AnnotatedControllerHandlerMethodAdapter](../jmouse-web/src/main/java/org/jmouse/web/mvc/adapter/AnnotatedControllerHandlerMethodAdapter.java) | 🎬 HandlerAdapter implementation for controllers with annotated handler methods. |
| [RequestHttpHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/adapter/RequestHttpHandler.java) | ⚙️ Functional interface for handling HTTP requests in a controller-style method. |
| [RequestHttpHandlerAdapter](../jmouse-web/src/main/java/org/jmouse/web/mvc/adapter/RequestHttpHandlerAdapter.java) | 🧩 HandlerAdapter for RequestHttpHandler. |

#### `org.jmouse.web.mvc.context`

| Type | Responsibility / contract |
|---|---|
| [WebMvcControllersInitializer](../jmouse-web/src/main/java/org/jmouse/web/mvc/context/WebMvcControllersInitializer.java) | 🔎 Scans for Controller @Controller-annotated classes and registers them as beans. |
| [WebMvcInfrastructureInitializer](../jmouse-web/src/main/java/org/jmouse/web/mvc/context/WebMvcInfrastructureInitializer.java) | 🛠 Initializes core Web MVC infrastructure components. |

#### `org.jmouse.web.mvc.cors`

| Type | Responsibility / contract |
|---|---|
| [AnnotationCorsConfigurationProvider](../jmouse-web/src/main/java/org/jmouse/web/mvc/cors/AnnotationCorsConfigurationProvider.java) | Resolves effective CorsConfiguration from CorsMapping annotations present on a handler method and/or its declaring class. |
| [Cors](../jmouse-web/src/main/java/org/jmouse/web/mvc/cors/Cors.java) | 🔐 CORS utilities. |
| [CorsConfiguration](../jmouse-web/src/main/java/org/jmouse/web/mvc/cors/CorsConfiguration.java) | 🌐 CORS configuration for jMouse MVC. |
| [CorsConfigurationProvider](../jmouse-web/src/main/java/org/jmouse/web/mvc/cors/CorsConfigurationProvider.java) | Operations: `getCorsConfiguration`. |
| [CorsInterceptor](../jmouse-web/src/main/java/org/jmouse/web/mvc/cors/CorsInterceptor.java) | 🔐 MVC interceptor that applies a CorsConfiguration before handler execution. |
| [CorsMapping](../jmouse-web/src/main/java/org/jmouse/web/mvc/cors/CorsMapping.java) | 🌐 Declarative CORS mapping for controllers and handler methods. |
| [CorsMappingRegistry](../jmouse-web/src/main/java/org/jmouse/web/mvc/cors/CorsMappingRegistry.java) | 📦 Registry of CORS configurations keyed by PathPattern. |
| [CorsProcessor](../jmouse-web/src/main/java/org/jmouse/web/mvc/cors/CorsProcessor.java) | 🔐 Strategy for applying a CORS policy to an HTTP exchange. |
| [CorsProcessorBean](../jmouse-web/src/main/java/org/jmouse/web/mvc/cors/CorsProcessorBean.java) | 📦 DI adapter that exposes the default WebCorsProcessor as a bean. |
| [WebCorsProcessor](../jmouse-web/src/main/java/org/jmouse/web/mvc/cors/WebCorsProcessor.java) | 🌐 CORS processor for jMouse MVC. |

#### `org.jmouse.web.mvc.cors.preflight`

| Type | Responsibility / contract |
|---|---|
| [PreflightCorsHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/cors/preflight/PreflightCorsHandler.java) | No-op handler used for CORS preflight exchanges. |

#### `org.jmouse.web.mvc.exception`

| Type | Responsibility / contract |
|---|---|
| [ExceptionHandlerExceptionResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/exception/ExceptionHandlerExceptionResolver.java) | ⚠️ ExceptionHandlerExceptionResolver resolves exceptions by invoking controller methods annotated with ExceptionHandler. |
| [ExceptionMappingRegistry](../jmouse-web/src/main/java/org/jmouse/web/mvc/exception/ExceptionMappingRegistry.java) | 📌 Registry for mapping exception types to their corresponding handler methods. |

#### `org.jmouse.web.mvc.interceptor`

| Type | Responsibility / contract |
|---|---|
| [LoggingHandlerInterceptor](../jmouse-web/src/main/java/org/jmouse/web/mvc/interceptor/LoggingHandlerInterceptor.java) | Implements/extends `HandlerInterceptor`. Operations: `preHandle`, `postHandle`. |

#### `org.jmouse.web.mvc.mapping`

| Type | Responsibility / contract |
|---|---|
| [AnnotatedControllerHandlerMapping](../jmouse-web/src/main/java/org/jmouse/web/mvc/mapping/AnnotatedControllerHandlerMapping.java) | 📌 Detects controller beans and registers their annotated handler methods. |
| [RequestHttpHandlerMapping](../jmouse-web/src/main/java/org/jmouse/web/mvc/mapping/RequestHttpHandlerMapping.java) | 🧠 Maps RequestHttpHandler instances based on registered RequestHttpHandlerRegistration. |
| [RequestHttpHandlerRegistration](../jmouse-web/src/main/java/org/jmouse/web/mvc/mapping/RequestHttpHandlerRegistration.java) | Value record: `(Route route, RequestHttpHandler handler)`. |
| [ResourceHttpMapping](../jmouse-web/src/main/java/org/jmouse/web/mvc/mapping/ResourceHttpMapping.java) | 🧷 Path mapping for serving static resources. |

#### `org.jmouse.web.mvc.method`

| Type | Responsibility / contract |
|---|---|
| [AbstractArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/AbstractArgumentResolver.java) | ⚙️ Base class for ArgumentResolver implementations with initialization hook. |
| [AbstractReturnValueHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/AbstractReturnValueHandler.java) | 🔄 Base class for ReturnValueHandler implementations. |
| [ArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/ArgumentResolver.java) | 🧪 Strategy for resolving method parameters from the request context. |
| [ExceptionHandlerMethod](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/ExceptionHandlerMethod.java) | 🎯 Represents a method that handles a specific Throwable type. |
| [HandlerMethod](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/HandlerMethod.java) | 🎯 Represents a handler method bound to a specific bean instance. |
| [HandlerMethodContext](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/HandlerMethodContext.java) | Value record: `(RequestContext requestContext, HandlerMethod handlerMethod)`. |
| [HandlerMethodInvocation](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/HandlerMethodInvocation.java) | 🧠 Responsible for invoking a HandlerMethod with resolved arguments. |
| [HandlerMethodInvocationException](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/HandlerMethodInvocationException.java) | Implements/extends `RuntimeException`. |
| [HttpMessageReturnValueHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/HttpMessageReturnValueHandler.java) | 📡 Handles controller return values by converting them into HTTP responses using HttpMessageConverters. |
| [ReturnValueHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/ReturnValueHandler.java) | 🎯 Strategy for handling controller return values. |
| [ReturnValueProcessor](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/ReturnValueProcessor.java) | 🔌 Delegates return value processing to the appropriate ReturnValueHandler. |
| [StringValueReturnValueHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/StringValueReturnValueHandler.java) | Implements/extends `AbstractReturnValueHandler`. Operations: `supportsReturnType`. |
| [ViewResolverReturnValueHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/ViewResolverReturnValueHandler.java) | 🖼️ Handles return values that resolve to views. |

#### `org.jmouse.web.mvc.method.argument`

| Type | Responsibility / contract |
|---|---|
| [AbstractBindableArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/argument/AbstractBindableArgumentResolver.java) | Implements/extends `AbstractArgumentResolver`. Operations: `doInitialize`, `getConverterManager`. |
| [BindingResultArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/argument/BindingResultArgumentResolver.java) | Resolves BindingResult controller arguments. 🧩 |
| [ExceptionArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/argument/ExceptionArgumentResolver.java) | ⚠️ Resolves method parameters of type Exception in handler or exception handler methods. |
| [ModelArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/argument/ModelArgumentResolver.java) | Implements/extends `AbstractArgumentResolver`. Operations: `supportsParameter`, `resolveArgument`. |
| [PathVariableArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/argument/PathVariableArgumentResolver.java) | 🛣️ Resolver for method arguments annotated with PathVariable. |
| [RequestContextArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/argument/RequestContextArgumentResolver.java) | 🎯 Argument resolver for core request/response objects. |
| [RequestHeaderArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/argument/RequestHeaderArgumentResolver.java) | 📑 Argument resolver for parameters annotated with RequestHeader. |
| [RequestMethodArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/argument/RequestMethodArgumentResolver.java) | 🚦 Argument resolver for parameters annotated with RequestMethod. |
| [RequestParameterArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/argument/RequestParameterArgumentResolver.java) | 🔧 Resolves method parameters annotated with RequestParameter by extracting the corresponding HTTP request parameter value. |
| [StatusCodeArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/argument/StatusCodeArgumentResolver.java) | 📡 Argument resolver for parameters annotated with StatusCode. |
| [TryGetBeanArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/argument/TryGetBeanArgumentResolver.java) | Argument resolver that attempts to resolve method parameters from BeanContext. 🧩 |
| [WebBeanContextArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/argument/WebBeanContextArgumentResolver.java) | Implements/extends `AbstractArgumentResolver implements BeanContextAware`. Operations: `supportsParameter`, `resolveArgument`, `setBeanContext`, `getBeanContext`. |
| [WebModelBindableArgumentResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/argument/WebModelBindableArgumentResolver.java) | Resolves arguments annotated with WebModel. 🧩 |

#### `org.jmouse.web.mvc.method.converter`

| Type | Responsibility / contract |
|---|---|
| [AbstractHttpMessageConverter](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/AbstractHttpMessageConverter.java) | 📦 Base implementation of HttpMessageConverter. |
| [AbstractJacksonHttpMessageConverter](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/AbstractJacksonHttpMessageConverter.java) | 📦 Base AbstractHttpMessageConverter for JSON processing with Jackson. |
| [ByteArrayHttpMessageConverter](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/ByteArrayHttpMessageConverter.java) | 📦 AbstractHttpMessageConverter for raw byte[] payloads. |
| [HttpInputMessage](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/HttpInputMessage.java) | 📥 Represents an HTTP request or response body for reading. |
| [HttpMessage](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/HttpMessage.java) | 📦 Represents the common contract for HTTP input/output messages. |
| [HttpMessageConverter](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/HttpMessageConverter.java) | 🔄 Strategy interface for converting HTTP request/response bodies. |
| [HttpOutputMessage](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/HttpOutputMessage.java) | 📤 Contract for HTTP output messages. |
| [JacksonJsonHttpMessageConverter](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/JacksonJsonHttpMessageConverter.java) | Implements/extends `AbstractJacksonHttpMessageConverter`. Operations: `doWrite`, `doRead`. |
| [JacksonObjectMapperConfiguration](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/JacksonObjectMapperConfiguration.java) | ⚙️ Auto-configuration for Jackson com.fasterxml.jackson.databind.ObjectMapper instances across supported content types. |
| [JacksonObjectMapperRegistration](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/JacksonObjectMapperRegistration.java) | Value record: `(ObjectMapper objectMapper, Set mediaTypes)`. |
| [JacksonObjectMapperResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/JacksonObjectMapperResolver.java) | 🎨 Resolves an appropriate ObjectMapper based on the requested MediaType. |
| [JacksonXmlHttpMessageConverter](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/JacksonXmlHttpMessageConverter.java) | Implements/extends `AbstractJacksonHttpMessageConverter`. Operations: `doWrite`, `doRead`. |
| [JacksonYamlHttpMessageConverter](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/JacksonYamlHttpMessageConverter.java) | Implements/extends `AbstractJacksonHttpMessageConverter`. Operations: `doWrite`, `doRead`. |
| [Jaxb2HttpMessageConverter](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/Jaxb2HttpMessageConverter.java) | 📦 AbstractHttpMessageConverter for JAXB-annotated objects. |
| [MessageConverterManager](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/MessageConverterManager.java) | ⚙️ Manages a collection of HttpMessageConverter instances. |
| [ObjectToStringHttpMessageConverter](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/ObjectToStringHttpMessageConverter.java) | 🔄 AbstractHttpMessageConverter that bridges arbitrary objects through a Conversion service by converting them to/from String. |
| [PartialResourceHttpMessageConverter](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/PartialResourceHttpMessageConverter.java) | 🚚 Streams Resource content to a servlet response with HTTP Range support. |
| [ResourceHttpMessageConverter](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/ResourceHttpMessageConverter.java) | 📦 HttpMessageConverter for Resource bodies. |
| [ServletHttpOutputMessage](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/ServletHttpOutputMessage.java) | 🌐 HttpOutputMessage implementation backed by HttpServletResponse. |
| [StringHttpMessageConverter](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/StringHttpMessageConverter.java) | 📝 AbstractHttpMessageConverter for String payloads. |
| [UnreadableException](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/UnreadableException.java) | Implements/extends `RuntimeException`. |
| [UnsuitableException](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/UnsuitableException.java) | Implements/extends `RuntimeException`. |
| [UnwritableException](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/UnwritableException.java) | Implements/extends `RuntimeException`. |
| [WebHttpServletRequest](../jmouse-web/src/main/java/org/jmouse/web/mvc/method/converter/WebHttpServletRequest.java) | Implements/extends `WebHttpRequest implements HttpInputMessage`. Operations: `getHeaders`. |

#### `org.jmouse.web.mvc.negotiation`

| Type | Responsibility / contract |
|---|---|
| [MappingMediaTypeLookupConfigurer](../jmouse-web/src/main/java/org/jmouse/web/mvc/negotiation/MappingMediaTypeLookupConfigurer.java) | Implements/extends `BeanConfigurer`. Operations: `configure`. |
| [MappingMediaTypeLookupProperties](../jmouse-web/src/main/java/org/jmouse/web/mvc/negotiation/MappingMediaTypeLookupProperties.java) | Operations: `getMediaTypes`, `setMediaTypes`, `getKeyName`, `setKeyName`. |
| [PathVariableLookupConfigurer](../jmouse-web/src/main/java/org/jmouse/web/mvc/negotiation/PathVariableLookupConfigurer.java) | Implements/extends `MappingMediaTypeLookupConfigurer`. |
| [PathVariableLookupProperties](../jmouse-web/src/main/java/org/jmouse/web/mvc/negotiation/PathVariableLookupProperties.java) | Implements/extends `MappingMediaTypeLookupProperties`. |
| [QueryParameterLookupConfigurer](../jmouse-web/src/main/java/org/jmouse/web/mvc/negotiation/QueryParameterLookupConfigurer.java) | Implements/extends `MappingMediaTypeLookupConfigurer`. |
| [QueryParameterLookupProperties](../jmouse-web/src/main/java/org/jmouse/web/mvc/negotiation/QueryParameterLookupProperties.java) | Implements/extends `MappingMediaTypeLookupProperties`. |

#### `org.jmouse.web.mvc.proxy`

| Type | Responsibility / contract |
|---|---|
| [RateLimitMethodInterceptor](../jmouse-web/src/main/java/org/jmouse/web/mvc/proxy/RateLimitMethodInterceptor.java) | 🚦 Concrete interceptor binding AbstractRateLimitMethodInterceptor to beans marked with RateLimited. |

#### `org.jmouse.web.mvc.resource`

| Type | Responsibility / contract |
|---|---|
| [AbstractResourceResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/AbstractResourceResolver.java) | 🏗️ Base class for ResourceResolver implementations. |
| [AbstractVersionStrategy](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/AbstractVersionStrategy.java) | 🏗️ Base class for content-hash based VersionStrategy implementations. |
| [ContentHashVersionStrategy](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ContentHashVersionStrategy.java) | 📦 Content-hash versioning strategy: file name contains "/{hex}/app.js" before extension. |
| [FixedVersionStrategy](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/FixedVersionStrategy.java) | 📌 Fixed versioning strategy for static resources. |
| [HttpResource](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/HttpResource.java) | Implements/extends `Resource`. Operations: `getHeaders`. |
| [HttpResourceException](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/HttpResourceException.java) | Implements/extends `RuntimeException`. |
| [InternalResourceConfiguration](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/InternalResourceConfiguration.java) | Default internal configuration for serving static resources. |
| [LocationScanningResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/LocationScanningResolver.java) | 📂 ResourceResolver that scans a list of base locations to find the first readable resource matching the requested path. |
| [NoopComposer](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/NoopComposer.java) | 🚫 No-op implementation of ResourceComposer. |
| [PathNormalizationResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/PathNormalizationResolver.java) | 🧹 Resource resolver that normalizes incoming request paths. |
| [PathVersion](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/PathVersion.java) | 🏷️ Represents a versioned resource path. |
| [QueryParameterVersionStrategy](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/QueryParameterVersionStrategy.java) | ❓ Query-parameter based VersionStrategy. |
| [ResourceChainRegistration](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceChainRegistration.java) | Operations: `addResolvers`, `addTransformers`, `getTransformers`, `getResolvers`. |
| [ResourceComposer](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceComposer.java) | Implements/extends `Link`. Operations: `handle`. |
| [ResourceComposerChain](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceComposerChain.java) | Implements/extends `Chain`. Operations: `run`. |
| [ResourceHandlerRegistry](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceHandlerRegistry.java) | 📂 Registry for static resource handler registrations. |
| [ResourceHttpHandler](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceHttpHandler.java) | 🗂️ HTTP handler for serving static Resources. |
| [ResourceNotFoundException](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceNotFoundException.java) | Implements/extends `HttpResourceException`. |
| [ResourceQuery](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceQuery.java) | 🔍 Encapsulates a resource lookup request. |
| [ResourceRange](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceRange.java) | 📦 Utility for mapping HTTP Range requests to ResourceSegments of a Resource. |
| [ResourceRegistration](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceRegistration.java) | 📂 Defines a resource handler registration. |
| [ResourceResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceResolver.java) | 📂 Contract for resolving static resources. |
| [ResourceResolverChain](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceResolverChain.java) | ⛓️ Chain of ResourceResolvers for static resource lookup. |
| [ResourceTransformer](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceTransformer.java) | Empty public interface reserved in the resource pipeline; no transformation methods are declared in the current source. |
| [ResourceUrlResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceUrlResolver.java) | 🌐 Resolves resource URLs by applying registered ResourceHttpHandlers. |
| [ResourceValidationFailedException](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceValidationFailedException.java) | Implements/extends `HttpResourceException`. |
| [ResourceVersionProperties](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/ResourceVersionProperties.java) | Operations: `getIconPrefix`, `setIconPrefix`, `getContentHash`, `setContentHash`, `getLength`, `setLength`. |
| [SimpleResourceResolverChain](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/SimpleResourceResolverChain.java) | ⛓️ Default implementation of ResourceResolverChain. |
| [TransformationContext](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/TransformationContext.java) | Value record: `(Resource resource)`. |
| [UrlComposerContext](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/UrlComposerContext.java) | 🌐 Context object passed to a ResourceComposer. |
| [VersionalResource](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/VersionalResource.java) | 🏷️ Resource wrapper that adds version metadata. |
| [VersionalResourceResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/VersionalResourceResolver.java) | 🧩 Resolves versioned static resource paths using pluggable strategies. Examples: |
| [VersionStrategy](../jmouse-web/src/main/java/org/jmouse/web/mvc/resource/VersionStrategy.java) | 🏷️ Strategy interface for resource path versioning (using PathVersion). |

#### `org.jmouse.web.mvc.servlet`

| Type | Responsibility / contract |
|---|---|
| [FirewallBeanConfiguration](../jmouse-web/src/main/java/org/jmouse/web/mvc/servlet/FirewallBeanConfiguration.java) | 🛡️ Servlet-level firewall configuration. |
| [FirewallRequestFilter](../jmouse-web/src/main/java/org/jmouse/web/mvc/servlet/FirewallRequestFilter.java) | 🛡️ Servlet Filter that integrates the jMouse Firewall. |
| [FirewallRequestFilterRegistration](../jmouse-web/src/main/java/org/jmouse/web/mvc/servlet/FirewallRequestFilterRegistration.java) | Implements/extends `DelegatingBeanFilterRegistration`. |
| [ShallowETagHeaderFilter](../jmouse-web/src/main/java/org/jmouse/web/mvc/servlet/ShallowETagHeaderFilter.java) | 🧩 Servlet Filter that computes and adds a shallow ETag for 2xx responses. |
| [ShallowETagHeaderFilterRegistration](../jmouse-web/src/main/java/org/jmouse/web/mvc/servlet/ShallowETagHeaderFilterRegistration.java) | Implements/extends `DelegatingBeanFilterRegistration`. |

#### `org.jmouse.web.mvc.view`

| Type | Responsibility / contract |
|---|---|
| [AbstractView](../jmouse-web/src/main/java/org/jmouse/web/mvc/view/AbstractView.java) | 🧩 Base class for View implementations. |
| [AbstractViewResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/view/AbstractViewResolver.java) | Implements/extends `ViewResolver`. |

#### `org.jmouse.web.mvc.view.internal`

| Type | Responsibility / contract |
|---|---|
| [InternalView](../jmouse-web/src/main/java/org/jmouse/web/mvc/view/internal/InternalView.java) | 🧩 A concrete View implementation that renders a Template using a Renderer and an EvaluationContext. |
| [InternalViewConfiguration](../jmouse-web/src/main/java/org/jmouse/web/mvc/view/internal/InternalViewConfiguration.java) | ⚙️ Internal configuration for jMouse view layer. |
| [InternalViewExtension](../jmouse-web/src/main/java/org/jmouse/web/mvc/view/internal/InternalViewExtension.java) | 🧩 Internal view extension for the jMouse MVC framework. |
| [InternalViewProperties](../jmouse-web/src/main/java/org/jmouse/web/mvc/view/internal/InternalViewProperties.java) | 🧾 Configuration for internal view rendering. |
| [InternalViewResolver](../jmouse-web/src/main/java/org/jmouse/web/mvc/view/internal/InternalViewResolver.java) | 🛠️ Internal view engine-based ViewResolver implementation. |

#### `org.jmouse.web.mvc.view.internal.extension`

| Type | Responsibility / contract |
|---|---|
| [jMouseAssetsFunction](../jmouse-web/src/main/java/org/jmouse/web/mvc/view/internal/extension/jMouseAssetsFunction.java) | 🎨 Expression Language (EL) function for resolving asset URLs. |

#### `org.jmouse.web.negotiation`

| Type | Responsibility / contract |
|---|---|
| [AcceptHeaderLookup](../jmouse-web/src/main/java/org/jmouse/web/negotiation/AcceptHeaderLookup.java) | 📡 Resolves MediaType values from the HTTP Accept header. |
| [ContentTypeHeaderLookup](../jmouse-web/src/main/java/org/jmouse/web/negotiation/ContentTypeHeaderLookup.java) | Resolves the media type from the request's Content-Type header. |
| [FallbackMediaTypeLookup](../jmouse-web/src/main/java/org/jmouse/web/negotiation/FallbackMediaTypeLookup.java) | 🛡️ Provides a fallback MediaType when no other negotiation strategies are applicable. |
| [LookupRegistry](../jmouse-web/src/main/java/org/jmouse/web/negotiation/LookupRegistry.java) | Operations: `getLookups`, `registerLookup`. |
| [MappedMediaTypeLookup](../jmouse-web/src/main/java/org/jmouse/web/negotiation/MappedMediaTypeLookup.java) | 🗺️ Base lookup that maps a request-derived key (e.g., path extension or query parameter) to a MediaType. |
| [MediaTypeLookup](../jmouse-web/src/main/java/org/jmouse/web/negotiation/MediaTypeLookup.java) | 🔍 Strategy for extracting client-requested media types from an HTTP request. |
| [MediaTypeManager](../jmouse-web/src/main/java/org/jmouse/web/negotiation/MediaTypeManager.java) | 🧩 Media type manager (resolver chain). |
| [PathVariableLookup](../jmouse-web/src/main/java/org/jmouse/web/negotiation/PathVariableLookup.java) | 🧭 Path-variable based media type resolver. |
| [QueryParameterLookup](../jmouse-web/src/main/java/org/jmouse/web/negotiation/QueryParameterLookup.java) | 🔎 Query-parameter based media type resolver. |

#### `org.jmouse.web.security.firewall`

| Type | Responsibility / contract |
|---|---|
| [Decision](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/Decision.java) | 🛡️ Represents the result of a firewall evaluation. |
| [EvaluationInput](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/EvaluationInput.java) | Value record: `( RequestContext requestContext )`. |
| [Firewall](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/Firewall.java) | 🛡️ Firewall engine that evaluates incoming requests against a list of FirewallPolicy. |
| [FirewallConfiguration](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/FirewallConfiguration.java) | Operations: `getRateLimit`, `setRateLimit`, `getUntrustedBrowser`, `setUntrustedBrowser`, `getBlockedCountries`, `setBlockedCountries`, `getTrustedProxy`, `setTrustedProxy`, `getInspectionPolicy`, `setInspectionPolicy`. |
| [FirewallPolicy](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/FirewallPolicy.java) | Operations: `apply`. |
| [InspectionPolicies](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/InspectionPolicies.java) | 🔍 Central container for firewall inspection policies. |
| [RateLimitProperties](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/RateLimitProperties.java) | Value record: `(int refillRate, int burst)`. |
| [Reputation](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/Reputation.java) | Empty public placeholder in the firewall package; no reputation state or behavior is implemented in the current source. |

#### `org.jmouse.web.security.firewall.policy`

| Type | Responsibility / contract |
|---|---|
| [AbstractInspectionPolicy](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/policy/AbstractInspectionPolicy.java) | Abstract base class for inspection policies. |
| [PathTraversalPolicy](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/policy/PathTraversalPolicy.java) | 🔒 Firewall policy that protects against path traversal attacks . |
| [RequestLimitPolicy](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/policy/RequestLimitPolicy.java) | 🚦 Firewall policy that enforces request rate limiting . |
| [SqlInjectionPolicy](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/policy/SqlInjectionPolicy.java) | 🛡️ Firewall policy that protects against SQL Injection attacks. |
| [SuspiciousUserAgentPolicy](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/policy/SuspiciousUserAgentPolicy.java) | 🕵️ Firewall policy that detects suspicious User-Agent headers. |
| [XssInjectionPolicy](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/policy/XssInjectionPolicy.java) | 🛡️ Firewall policy that protects against Cross-Site Scripting (XSS) attacks. |

#### `org.jmouse.web.security.firewall.policy.inspection`

| Type | Responsibility / contract |
|---|---|
| [InspectionRule](../jmouse-web/src/main/java/org/jmouse/web/security/firewall/policy/inspection/InspectionRule.java) | Represents a single inspection rule. |

#### `org.jmouse.web.server`

| Type | Responsibility / contract |
|---|---|
| [WebServer](../jmouse-web/src/main/java/org/jmouse/web/server/WebServer.java) | A generic interface for managing a web server lifecycle and configuration. |
| [WebServerConfig](../jmouse-web/src/main/java/org/jmouse/web/server/WebServerConfig.java) | Configuration record for a web server instance. |
| [WebServerException](../jmouse-web/src/main/java/org/jmouse/web/server/WebServerException.java) | Implements/extends `RuntimeException`. |
| [WebServerFactory](../jmouse-web/src/main/java/org/jmouse/web/server/WebServerFactory.java) | Factory interface for creating WebServer instances. |
| [WebServers](../jmouse-web/src/main/java/org/jmouse/web/server/WebServers.java) | Enum values: `NO_SERVER`, `TOMCAT`. |

#### `org.jmouse.web.server.tomcat`

| Type | Responsibility / contract |
|---|---|
| [TomcatWebServer](../jmouse-web/src/main/java/org/jmouse/web/server/tomcat/TomcatWebServer.java) | Implements/extends `WebServer`. Operations: `start`, `stop`, `server`, `name`. |
| [TomcatWebServerConfigurer](../jmouse-web/src/main/java/org/jmouse/web/server/tomcat/TomcatWebServerConfigurer.java) | Configures an Apache Tomcat server instance. Implements the WebServer.Configurer interface to adjust various properties of the Tomcat server (e.g., port, document base directory, etc.). |
| [TomcatWebServerFactory](../jmouse-web/src/main/java/org/jmouse/web/server/tomcat/TomcatWebServerFactory.java) | Implements/extends `WebServerFactory, BeanContextAware`. Operations: `getBeanContext`, `setBeanContext`, `createWebServer`. |

#### `org.jmouse.web.servlet`

| Type | Responsibility / contract |
|---|---|
| [BufferingOnlyResponseWrapper](../jmouse-web/src/main/java/org/jmouse/web/servlet/BufferingOnlyResponseWrapper.java) | 📦 Buffer-only HttpServletResponseWrapper. |
| [CapturingServletOutputStream](../jmouse-web/src/main/java/org/jmouse/web/servlet/CapturingServletOutputStream.java) | 🧲 ServletOutputStream that captures all written bytes into a backing ByteArrayOutputStream. |
| [DelegatingBeanFilter](../jmouse-web/src/main/java/org/jmouse/web/servlet/DelegatingBeanFilter.java) | 💡 A servlet Filter that delegates its behavior to a target filter bean resolved from a BeanContext. |
| [OnCommitResponseWrapper](../jmouse-web/src/main/java/org/jmouse/web/servlet/OnCommitResponseWrapper.java) | 🔔 OnCommitResponseWrapper |
| [PerRequestLimiterFilter](../jmouse-web/src/main/java/org/jmouse/web/servlet/PerRequestLimiterFilter.java) | ⏳ PerRequestLimiterFilter Servlet filter that limits the number of executions per HTTP request. |
| [RequestContextListener](../jmouse-web/src/main/java/org/jmouse/web/servlet/RequestContextListener.java) | 🧩 Servlet listener that manages RequestAttributesHolder for each HTTP request. |
| [ServletDispatcher](../jmouse-web/src/main/java/org/jmouse/web/servlet/ServletDispatcher.java) | Base servlet for handling HTTP requests in jMouse framework 🌐 |
| [SessionConfigurationInitializer](../jmouse-web/src/main/java/org/jmouse/web/servlet/SessionConfigurationInitializer.java) | 🛠️ Configures the servlet session cookie and tracking settings at application startup using values from SessionProperties. |
| [SessionProperties](../jmouse-web/src/main/java/org/jmouse/web/servlet/SessionProperties.java) | 📦 Session configuration properties used to control cookie, timeout, and tracking behavior. |
| [WebBeanContextListener](../jmouse-web/src/main/java/org/jmouse/web/servlet/WebBeanContextListener.java) | Listener for loading and managing the WebBeanContext within a ServletContext. It initializes the web application structured context during the servlet context initialization phase and cleans it up during the destruction phase. |

#### `org.jmouse.web.servlet.filter`

| Type | Responsibility / contract |
|---|---|
| [BeanFilter](../jmouse-web/src/main/java/org/jmouse/web/servlet/filter/BeanFilter.java) | 🫘 Integration point between Servlet Filter and jMouse bean lifecycle. |
| [MultipartRequestFilter](../jmouse-web/src/main/java/org/jmouse/web/servlet/filter/MultipartRequestFilter.java) | Implements/extends `Filter`. Operations: `doFilter`. |
| [MultipartRequestFilterRegistration](../jmouse-web/src/main/java/org/jmouse/web/servlet/filter/MultipartRequestFilterRegistration.java) | Implements/extends `DelegatingBeanFilterRegistration`. |
| [RequestWrapperFilter](../jmouse-web/src/main/java/org/jmouse/web/servlet/filter/RequestWrapperFilter.java) | Implements/extends `Filter`. Operations: `doFilter`. |
| [RequestWrapperFilterRegistration](../jmouse-web/src/main/java/org/jmouse/web/servlet/filter/RequestWrapperFilterRegistration.java) | Implements/extends `DelegatingBeanFilterRegistration`. |
| [SessionServletFilter](../jmouse-web/src/main/java/org/jmouse/web/servlet/filter/SessionServletFilter.java) | 📶 Servlet Filter that binds current HTTP request to session-scoped RequestAttributes. |
| [SessionServletFilterRegistration](../jmouse-web/src/main/java/org/jmouse/web/servlet/filter/SessionServletFilterRegistration.java) | 🔐 Registers the SessionServletFilter as a delegating filter using DelegatingBeanFilterRegistration. |

#### `org.jmouse.web.servlet.initializer`

| Type | Responsibility / contract |
|---|---|
| [jMouseServletContainerInitializer](../jmouse-web/src/main/java/org/jmouse/web/servlet/initializer/jMouseServletContainerInitializer.java) | A ServletContainerInitializer implementation for initializing application-specific components. Automatically detects and initializes implementations of WebApplicationInitializer during the servlet container startup. |

#### `org.jmouse.web.servlet.registration`

| Type | Responsibility / contract |
|---|---|
| [AbstractDynamicRegistrationBean](../jmouse-web/src/main/java/org/jmouse/web/servlet/registration/AbstractDynamicRegistrationBean.java) | Base class for dynamic registration beans (servlets, filters, etc.) that produce a Registration.Dynamic handle. Handles common configuration steps such as async support and init parameters. |
| [AbstractRegistrationBean](../jmouse-web/src/main/java/org/jmouse/web/servlet/registration/AbstractRegistrationBean.java) | Base class for all dynamic registration beans (servlets, filters, listeners). |
| [FilterRegistrationBean](../jmouse-web/src/main/java/org/jmouse/web/servlet/registration/FilterRegistrationBean.java) | RegistrationBean implementation for programmatic registration of a Filter. |
| [RegistrationBean](../jmouse-web/src/main/java/org/jmouse/web/servlet/registration/RegistrationBean.java) | Common interface for programmatic registration of web components (servlets, filters, listeners) into a ServletContext. |
| [ServletListenerRegistrationBean](../jmouse-web/src/main/java/org/jmouse/web/servlet/registration/ServletListenerRegistrationBean.java) | Implements/extends `AbstractRegistrationBean`. Operations: `register`, `getListener`, `setListener`, `getDescription`, `toString`. |
| [ServletRegistrationBean](../jmouse-web/src/main/java/org/jmouse/web/servlet/registration/ServletRegistrationBean.java) | RegistrationBean implementation for programmatic registration of a Servlet. |

### Packaged resources

- [META-INF/services/jakarta.servlet.ServletContainerInitializer](../jmouse-web/src/main/resources/META-INF/services/jakarta.servlet.ServletContainerInitializer).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [Smoke.java](../jmouse-web/src/main/java/org/jmouse/web/binding/smoke/Smoke.java).
- [SmokeNested.java](../jmouse-web/src/main/java/org/jmouse/web/binding/smoke/SmokeNested.java).
- [SimplePathPatternSmoke.java](../jmouse-web/src/main/java/org/jmouse/web/mvc/SimplePathPatternSmoke.java).
