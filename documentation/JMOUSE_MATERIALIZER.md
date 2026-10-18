# jmouse-materializer

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-materializer`. Build/dependency declaration: [pom.xml](../jmouse-materializer/pom.xml).

## What it is

A structured template materialization system producing jMouse DOM or XML DOM trees from a model. Templates, value resolution, directives, transformations and hooks are separate stages, with submission decoration and form rules supplied for HTML use.

This module owns template-to-tree materialization and tree rendering. It does not require a web controller. The source packages intentionally retain the historical spelling meterializer in several Java package names; copy the actual package, not an inferred spelling.

## The map

| Type | Responsibility |
| --- | --- |
| [NodeTemplate](../jmouse-materializer/src/main/java/org/jmouse/meterializer/NodeTemplate.java) | Declarative node/template representation consumed by materializers. |
| [TemplateRegistry](../jmouse-materializer/src/main/java/org/jmouse/meterializer/TemplateRegistry.java) | Stores and resolves registered templates. |
| [ValueResolver](../jmouse-materializer/src/main/java/org/jmouse/meterializer/ValueResolver.java) | Resolves model-backed values during materialization. |
| [AbstractTemplateMaterializer](../jmouse-materializer/src/main/java/org/jmouse/meterializer/AbstractTemplateMaterializer.java) | Shared traversal/materialization mechanics for different output node types. |
| [DOMMaterializer](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/DOMMaterializer.java) | Creates jMouse DOM nodes and flattens temporary wrapper containers. |
| [DOMRenderingPipeline](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/DOMRenderingPipeline.java) | Wires resolution, hooks and materialization for jMouse Node output. |
| [XmlDomMaterializer](../jmouse-materializer/src/main/java/org/jmouse/xml/materializer/XmlDomMaterializer.java) | Materializer variant producing XML DOM output. |
| [RenderingRequest](../jmouse-materializer/src/main/java/org/jmouse/meterializer/RenderingRequest.java) | Per-render attributes, typed lookup and parent/local overlays. |
| [RenderingHook](../jmouse-materializer/src/main/java/org/jmouse/meterializer/hooks/RenderingHook.java) | Hook seam around rendering stages. |
| [SubmissionDecorationHook](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/submission/SubmissionDecorationHook.java) | Applies submission values and feedback to generated form controls. |
| [RuleBasedTransformer](../jmouse-materializer/src/main/java/org/jmouse/meterializer/transform/RuleBasedTransformer.java) | Applies template changes through traversal-aware matching rules. |
| [RenderingProcessor](../jmouse-materializer/src/main/java/org/jmouse/dom/renderer/RenderingProcessor.java) | Converts a materialized DOM tree through registered renderers. |

## Execution and ownership

1. Register templates and choose value resolution and output materializer for the model shape.
2. Build a DOMRenderingPipeline or XmlRenderingPipeline with a template resolver, materializer and required hooks.
3. Call render(templateKey, model), optionally customizing a new RenderingRequest with submission or theme data.
4. Consume the returned tree directly or render it through the DOM renderer registry; template transformation and final text serialization remain separate steps.

## Public API and behavior

Rendering<T> is the narrow public rendering facade; T is the produced tree/output type.

RenderingRequest.setAttribute is the concrete mutator. Typed findAttribute refuses an incompatible stored value.

overlayOn provides local-over-parent lookup, useful for request-specific settings without rewriting shared template configuration.

DOMMaterializer accepts a ValueResolver and creates ElementNode/TextNode output. WrapperNode is an internal multi-node container flattened into the parent.

## Wiring and extension points

- Implement TemplateMaterializer for another output tree, or ValueResolver for another model expression mechanism.
- Use ThemeModule/ThemeAssembly and TemplateTransformer for reusable theme composition.
- Use ControlValueApplier and TargetSelector implementations to extend form submission decoration rather than embedding all form policy in element construction.

## Pitfalls

- Do not confuse the template tree, materialized DOM tree and final serialized text; hooks and transformers act at different boundaries.
- RenderingRequest is invocation state. Sharing one mutable request across concurrent renders leaks attributes.
- A blank qualified node name is rejected by DOMMaterializer; namespace handling for XML belongs to NamespaceConfig/NamespaceScope.
- Java package names include org.jmouse.meterializer and org.jmouse.dom.meterializer despite the correctly spelled Maven module name.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-materializer/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

Existing executable examples/smokes: [Smoke4](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/smoke/Smoke4.java).

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-materializer/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.dom`

| Type | Responsibility / contract |
|---|---|
| [CommentInfoCorrector](../jmouse-materializer/src/main/java/org/jmouse/dom/CommentInfoCorrector.java) | Corrector that injects diagnostic comments before and/or after each node. 🏷️ |
| [CorrectNodeDepth](../jmouse-materializer/src/main/java/org/jmouse/dom/CorrectNodeDepth.java) | Corrector that recalculates and normalizes node depth values. 📏 |
| [Corrector](../jmouse-materializer/src/main/java/org/jmouse/dom/Corrector.java) | Functional interface for DOM tree correction or normalization. 🛠️ |
| [FormMetadata](../jmouse-materializer/src/main/java/org/jmouse/dom/FormMetadata.java) | Value record: `(String action, String method, Map hidden)`. |
| [InputType](../jmouse-materializer/src/main/java/org/jmouse/dom/InputType.java) | Enumeration of supported HTML types. ⌨️ |
| [NbspReplacerCorrector](../jmouse-materializer/src/main/java/org/jmouse/dom/NbspReplacerCorrector.java) | Corrector implementation that replaces whitespace in text nodes with entities. ␠➡️ |
| [Node](../jmouse-materializer/src/main/java/org/jmouse/dom/Node.java) | Core DOM node abstraction used by the jMouse DOM model. 🌳 |
| [NodeType](../jmouse-materializer/src/main/java/org/jmouse/dom/NodeType.java) | Enum values: `COMMENT`, `TEXT`, `CDATA`, `ELEMENT`, `NULL`. |
| [TagName](../jmouse-materializer/src/main/java/org/jmouse/dom/TagName.java) | Enum values: `HTML`, `HEAD`, `BODY`, `TITLE`, `BASE`, `LINK`, `META`, `STYLE`, `SCRIPT`, `NOSCRIPT`, `DIV`, `SPAN`, `H1`, `H2`, `H3`, `H4`, `H5`, `H6`, `HEADER`, `FOOTER`, `MAIN`, `SECTION`, `ARTICLE`, `ASIDE`, `DETAILS`. |

#### `org.jmouse.dom.constructor`

| Type | Responsibility / contract |
|---|---|
| [AbstractNodeConstructorRegistry](../jmouse-materializer/src/main/java/org/jmouse/dom/constructor/AbstractNodeConstructorRegistry.java) | Implements/extends `NodeConstructorRegistry`. Operations: `setConstructor`, `getConstructor`. |
| [NodeConstructor](../jmouse-materializer/src/main/java/org/jmouse/dom/constructor/NodeConstructor.java) | Operations: `construct`. |
| [NodeConstructorContext](../jmouse-materializer/src/main/java/org/jmouse/dom/constructor/NodeConstructorContext.java) | Implements/extends `AbstractAttributesContext`. Operations: `getDataProvider`, `setDataProvider`, `getRegistry`, `setRegistry`. |
| [NodeConstructorRegistry](../jmouse-materializer/src/main/java/org/jmouse/dom/constructor/NodeConstructorRegistry.java) | Operations: `setConstructor`, `getConstructor`. |

#### `org.jmouse.dom.helper`

| Type | Responsibility / contract |
|---|---|
| [NodeTraversal](../jmouse-materializer/src/main/java/org/jmouse/dom/helper/NodeTraversal.java) | Utility class for traversing a DOM tree and selecting nodes. 🔎 |

#### `org.jmouse.dom.meterializer`

| Type | Responsibility / contract |
|---|---|
| [BootstrapTemplates](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/BootstrapTemplates.java) | Bootstrap-styled template presets built on top of NodeTemplate. 🎨 |
| [BootstrapThemeModule](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/BootstrapThemeModule.java) | Implements/extends `ThemeModule`. Operations: `contributeTemplates`, `transformers`, `hooks`. |
| [DefaultTemplates](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/DefaultTemplates.java) | Operations: `defaultForm`, `button`, `submitButton`, `inputButton`, `input`, `inputText`, `inputNumber`, `inputEmail`, `inputPassword`, `select`, `checkboxSingle`, `checkboxInline`. |
| [DOMMaterializer](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/DOMMaterializer.java) | DOM-oriented TemplateMaterializer that turns NodeTemplates into jMouse Nodes. |
| [DOMRenderingPipeline](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/DOMRenderingPipeline.java) | Rendering pipeline specialization for DOM output. 🌳 |
| [NodeRule](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/NodeRule.java) | Contract for DOM post-processing rules applied after materialization. 🔧 |
| [NodeRuleSet](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/NodeRuleSet.java) | Container and executor for NodeRule definitions. 🧠 |

#### `org.jmouse.dom.meterializer.hooks`

| Type | Responsibility / contract |
|---|---|
| [NodeRuleHook](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/hooks/NodeRuleHook.java) | Applies NodeRule rules to the materialized DOM tree. 🧩 |
| [OptionSelectionHook](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/hooks/OptionSelectionHook.java) | Populates option selection flags (option.checked) for checkbox/radio controls using SubmissionState. ✅ |

#### `org.jmouse.dom.meterializer.rules`

| Type | Responsibility / contract |
|---|---|
| [AddHiddenInput](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/rules/AddHiddenInput.java) | NodeRule that injects hidden elements into a . |
| [AutocompleteOff](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/rules/AutocompleteOff.java) | NodeRule that disables browser autocomplete for form fields. |
| [ComplementForm](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/rules/ComplementForm.java) | NodeRule that enriches elements using FormMetadata. |
| [SelectSelected](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/rules/SelectSelected.java) | NodeRule that marks the selected in a based on submitted form values. |

#### `org.jmouse.dom.meterializer.submission`

| Type | Responsibility / contract |
|---|---|
| [AncestorTargetSelector](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/submission/AncestorTargetSelector.java) | TargetSelector that searches for an ancestor node matching the given predicate. 🔎 |
| [CheckboxBooleanApplier](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/submission/CheckboxBooleanApplier.java) | ControlValueApplier for boolean-like checkbox values. ☑️ |
| [ControlValueApplier](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/submission/ControlValueApplier.java) | Applies submission values to DOM controls. 🧩 |
| [DefaultFieldKeyResolver](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/submission/DefaultFieldKeyResolver.java) | Default FieldKeyResolver based on the name attribute. 🧩 |
| [FieldKeyResolver](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/submission/FieldKeyResolver.java) | Resolves submission keys for form fields. 🧩 |
| [InputValueApplier](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/submission/InputValueApplier.java) | ControlValueApplier for elements. 🧩 |
| [LastSiblingTargetSelector](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/submission/LastSiblingTargetSelector.java) | TargetSelector that places feedback after the last sibling branch representing the same logical field. 🧭 |
| [SelectValueApplier](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/submission/SelectValueApplier.java) | ControlValueApplier for elements. 🧩 |
| [SelfTargetSelector](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/submission/SelfTargetSelector.java) | TargetSelector that returns the control node itself. 🎯 |
| [SubmissionDecorationHook](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/submission/SubmissionDecorationHook.java) | RenderingHook that decorates form controls using submission state. 🧩 |
| [TargetSelector](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/submission/TargetSelector.java) | Selects the DOM node used as the error target. 🎯 |
| [TextareaValueApplier](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/submission/TextareaValueApplier.java) | ControlValueApplier for elements. 🧩 |

#### `org.jmouse.dom.node`

| Type | Responsibility / contract |
|---|---|
| [AbstractNode](../jmouse-materializer/src/main/java/org/jmouse/dom/node/AbstractNode.java) | Base implementation of Node providing common DOM tree mechanics. 🧱 |
| [CDataNode](../jmouse-materializer/src/main/java/org/jmouse/dom/node/CDataNode.java) | Implements/extends `TextNode`. Operations: `getNodeType`. |
| [CommentNode](../jmouse-materializer/src/main/java/org/jmouse/dom/node/CommentNode.java) | DOM node representing an HTML/XML comment. 💬 |
| [ElementNode](../jmouse-materializer/src/main/java/org/jmouse/dom/node/ElementNode.java) | Concrete DOM element node implementation. 🧱 |
| [EmptyNode](../jmouse-materializer/src/main/java/org/jmouse/dom/node/EmptyNode.java) | Implements/extends `NonElementNode`. |
| [HTMLElementNode](../jmouse-materializer/src/main/java/org/jmouse/dom/node/HTMLElementNode.java) | Specialized ElementNode for HTML elements. 🌐 |
| [InputElementNode](../jmouse-materializer/src/main/java/org/jmouse/dom/node/InputElementNode.java) | Specialized HTML element node representing elements. ⌨️ |
| [NonElementNode](../jmouse-materializer/src/main/java/org/jmouse/dom/node/NonElementNode.java) | Base class for non-element DOM nodes. 📄 |
| [TextNode](../jmouse-materializer/src/main/java/org/jmouse/dom/node/TextNode.java) | DOM node representing plain text content. 📝 |
| [WrapperNode](../jmouse-materializer/src/main/java/org/jmouse/dom/node/WrapperNode.java) | Implements/extends `ElementNode`. |

#### `org.jmouse.dom.renderer`

| Type | Responsibility / contract |
|---|---|
| [CDataRenderer](../jmouse-materializer/src/main/java/org/jmouse/dom/renderer/CDataRenderer.java) | Renderer for CDataNode instances. 📦 |
| [CommentRenderer](../jmouse-materializer/src/main/java/org/jmouse/dom/renderer/CommentRenderer.java) | Renderer for CommentNode instances. 💬 |
| [ElementRenderer](../jmouse-materializer/src/main/java/org/jmouse/dom/renderer/ElementRenderer.java) | Renders NodeType#ELEMENT nodes into markup. 🧱 |
| [Renderer](../jmouse-materializer/src/main/java/org/jmouse/dom/renderer/Renderer.java) | Strategy interface for rendering a Node into textual output. 🎨 |
| [RendererContext](../jmouse-materializer/src/main/java/org/jmouse/dom/renderer/RendererContext.java) | Immutable rendering configuration used by DOM renderers. ⚙️ |
| [RendererRegistry](../jmouse-materializer/src/main/java/org/jmouse/dom/renderer/RendererRegistry.java) | Registry responsible for resolving a Renderer for a given Node. 🗂️ |
| [Renderers](../jmouse-materializer/src/main/java/org/jmouse/dom/renderer/Renderers.java) | Factory methods for commonly used RendererRegistry configurations. 🧰 |
| [RenderingProcessor](../jmouse-materializer/src/main/java/org/jmouse/dom/renderer/RenderingProcessor.java) | Rendering dispatcher that converts a DOM Node tree into a string. 🧵 |
| [TextRenderer](../jmouse-materializer/src/main/java/org/jmouse/dom/renderer/TextRenderer.java) | Renderer for TextNode instances. 📝 |

#### `org.jmouse.meterializer`

| Type | Responsibility / contract |
|---|---|
| [AbstractRenderingPipeline](../jmouse-materializer/src/main/java/org/jmouse/meterializer/AbstractRenderingPipeline.java) | Base rendering pipeline implementation combining: template resolution, execution context creation, materialization, and hooks. 🧬 |
| [AbstractTemplateMaterializer](../jmouse-materializer/src/main/java/org/jmouse/meterializer/AbstractTemplateMaterializer.java) | Base TemplateMaterializer with built-in traversal algorithm over NodeTemplate. |
| [ConfigurableValueResolver](../jmouse-materializer/src/main/java/org/jmouse/meterializer/ConfigurableValueResolver.java) | Default ValueResolver implementation with configurable resolution scope. 🧩 |
| [DefaultValueResolver](../jmouse-materializer/src/main/java/org/jmouse/meterializer/DefaultValueResolver.java) | Default implementation of ValueResolver. ⚙️ |
| [DirectiveOutcome](../jmouse-materializer/src/main/java/org/jmouse/meterializer/DirectiveOutcome.java) | Value record: `( boolean omitted, T node, T root )`. |
| [ElementType](../jmouse-materializer/src/main/java/org/jmouse/meterializer/ElementType.java) | Predefined element kinds used for template selection. 🏷️ |
| [ElementTypeProvider](../jmouse-materializer/src/main/java/org/jmouse/meterializer/ElementTypeProvider.java) | Strategy interface for exposing an ElementType from a custom model object. 🧩 |
| [ModelReference](../jmouse-materializer/src/main/java/org/jmouse/meterializer/ModelReference.java) | Simple holder linking a template reference with a model object. 🔗 |
| [NodeDirective](../jmouse-materializer/src/main/java/org/jmouse/meterializer/NodeDirective.java) | Structural and behavioral directive applied to a materialized node. 🧩 |
| [NodeTemplate](../jmouse-materializer/src/main/java/org/jmouse/meterializer/NodeTemplate.java) | Intermediate immutable blueprint representing a markup tree. 🏗️ |
| [NodeTemplateResolver](../jmouse-materializer/src/main/java/org/jmouse/meterializer/NodeTemplateResolver.java) | Operations: `resolve`, `nonNull`, `Implementation`, `IllegalStateException`. |
| [PathValueResolver](../jmouse-materializer/src/main/java/org/jmouse/meterializer/PathValueResolver.java) | Resolves path expressions against root accessor and scoped variables using PropertyPath. |
| [PipelineBuilder](../jmouse-materializer/src/main/java/org/jmouse/meterializer/PipelineBuilder.java) | Operations: `templateRegistry`, `transformer`, `transformerChain`, `templateResolver`, `materializer`, `accessorWrapper`, `hook`, `hooks`, `instanceFactory`, `build`. |
| [PredicateEvaluator](../jmouse-materializer/src/main/java/org/jmouse/meterializer/PredicateEvaluator.java) | Evaluates TemplatePredicate instances against a RenderingExecution. 🧠 |
| [QName](../jmouse-materializer/src/main/java/org/jmouse/meterializer/QName.java) | Immutable representation of a qualified name (QName). 🏷️ |
| [Rendering](../jmouse-materializer/src/main/java/org/jmouse/meterializer/Rendering.java) | Contract for template rendering. 🎨 |
| [RenderingExecution](../jmouse-materializer/src/main/java/org/jmouse/meterializer/RenderingExecution.java) | Operations: `resolver`, `accessorWrapper`, `rootAccessor`, `request`, `variables`, `valueNavigator`. |
| [RenderingRequest](../jmouse-materializer/src/main/java/org/jmouse/meterializer/RenderingRequest.java) | Rendering request context used during template materialization. |
| [ResolutionMode](../jmouse-materializer/src/main/java/org/jmouse/meterializer/ResolutionMode.java) | Enum values: `FULL`, `CONSTANT_ONLY`. |
| [SubmissionState](../jmouse-materializer/src/main/java/org/jmouse/meterializer/SubmissionState.java) | Holds submitted form state during rendering. |
| [TemplateKeyMapping](../jmouse-materializer/src/main/java/org/jmouse/meterializer/TemplateKeyMapping.java) | Operations: `builder`, `resolve`, `map`, `build`. |
| [TemplateKeyProvider](../jmouse-materializer/src/main/java/org/jmouse/meterializer/TemplateKeyProvider.java) | Operations: `templateKey`. |
| [TemplateMaterializer](../jmouse-materializer/src/main/java/org/jmouse/meterializer/TemplateMaterializer.java) | Strategy responsible for converting a NodeTemplate into a concrete rendered representation. ⚙️ |
| [TemplatePredicate](../jmouse-materializer/src/main/java/org/jmouse/meterializer/TemplatePredicate.java) | Declarative predicate used by blueprint nodes to control rendering flow. 🔎 |
| [TemplateRegistry](../jmouse-materializer/src/main/java/org/jmouse/meterializer/TemplateRegistry.java) | Catalog that resolves templates by key. |
| [TemplateRewrite](../jmouse-materializer/src/main/java/org/jmouse/meterializer/TemplateRewrite.java) | Operations: `rewrite`. |
| [TemplateTransformer](../jmouse-materializer/src/main/java/org/jmouse/meterializer/TemplateTransformer.java) | Pre-materialization transformation step for NodeTemplate. 🔄 |
| [ThemeAssembly](../jmouse-materializer/src/main/java/org/jmouse/meterializer/ThemeAssembly.java) | Operations: `forTheme`, `overrideTemplate`, `addTransformer`, `addHook`, `build`, `templateRegistry`. |
| [ThemeModule](../jmouse-materializer/src/main/java/org/jmouse/meterializer/ThemeModule.java) | Operations: `contributeTemplates`, `transformers`, `hooks`. |
| [TransformerChain](../jmouse-materializer/src/main/java/org/jmouse/meterializer/TransformerChain.java) | Applies ordered blueprint transformers. |
| [TransformValueResolver](../jmouse-materializer/src/main/java/org/jmouse/meterializer/TransformValueResolver.java) | Minimal ValueResolver used during transformation phase. 🔄 |
| [ValueExpression](../jmouse-materializer/src/main/java/org/jmouse/meterializer/ValueExpression.java) | Declarative value expression used during rendering. 🧩 |
| [ValueResolver](../jmouse-materializer/src/main/java/org/jmouse/meterializer/ValueResolver.java) | Resolves a ValueExpression into a concrete runtime value. 🧩 |

#### `org.jmouse.meterializer.build`

| Type | Responsibility / contract |
|---|---|
| [AttributeMapBuilder](../jmouse-materializer/src/main/java/org/jmouse/meterializer/build/AttributeMapBuilder.java) | Builder for blueprint attributes. |
| [ElementTemplateBuilder](../jmouse-materializer/src/main/java/org/jmouse/meterializer/build/ElementTemplateBuilder.java) | Builder for NodeTemplate.Element. |
| [TemplateNodeCollectionBuilder](../jmouse-materializer/src/main/java/org/jmouse/meterializer/build/TemplateNodeCollectionBuilder.java) | Builder for lists of blueprint nodes. |

#### `org.jmouse.meterializer.hooks`

| Type | Responsibility / contract |
|---|---|
| [RenderingHook](../jmouse-materializer/src/main/java/org/jmouse/meterializer/hooks/RenderingHook.java) | Intercepts the rendering pipeline. |
| [RenderingHookChain](../jmouse-materializer/src/main/java/org/jmouse/meterializer/hooks/RenderingHookChain.java) | Value record: `(List > hooks)`. |
| [RenderingStage](../jmouse-materializer/src/main/java/org/jmouse/meterializer/hooks/RenderingStage.java) | Pipeline stages that can be intercepted. |

#### `org.jmouse.meterializer.query`

| Type | Responsibility / contract |
|---|---|
| [TemplateTraversal](../jmouse-materializer/src/main/java/org/jmouse/meterializer/query/TemplateTraversal.java) | Operations: `walk`, `find`, `findFirst`, `anyMatch`, `findElementsByTag`, `findElementsWithAttribute`, `findIncludes`, `childrenOf`. |

#### `org.jmouse.meterializer.transform`

| Type | Responsibility / contract |
|---|---|
| [AdvancedChange](../jmouse-materializer/src/main/java/org/jmouse/meterializer/transform/AdvancedChange.java) | Advanced blueprint changes. |
| [BootstrapTransformers](../jmouse-materializer/src/main/java/org/jmouse/meterializer/transform/BootstrapTransformers.java) | Operations: `create`. |
| [Change](../jmouse-materializer/src/main/java/org/jmouse/meterializer/transform/Change.java) | Factory for common template changes. |
| [ContextAwareMatcher](../jmouse-materializer/src/main/java/org/jmouse/meterializer/transform/ContextAwareMatcher.java) | Matcher that can use traversal context (ancestors, depth, parent). |
| [ContextMatch](../jmouse-materializer/src/main/java/org/jmouse/meterializer/transform/ContextMatch.java) | Factory for context-aware matchers. |
| [Match](../jmouse-materializer/src/main/java/org/jmouse/meterializer/transform/Match.java) | Operations: `tagName`, `element`, `hasAttribute`, `attributeValue`, `insideAncestor`, `parentTag`, `and`, `or`, `not`, `constantAttributeValues`. |
| [RuleBasedTransformer](../jmouse-materializer/src/main/java/org/jmouse/meterializer/transform/RuleBasedTransformer.java) | Implements/extends `TemplateTransformer`. Operations: `transform`, `Rule`, `builder`, `rule`, `build`. |
| [TemplateChange](../jmouse-materializer/src/main/java/org/jmouse/meterializer/transform/TemplateChange.java) | Applies a change to a matched blueprint node. |
| [TemplateTraversal](../jmouse-materializer/src/main/java/org/jmouse/meterializer/transform/TemplateTraversal.java) | Operations: `traverse`. |
| [TraversalContext](../jmouse-materializer/src/main/java/org/jmouse/meterializer/transform/TraversalContext.java) | Operations: `empty`, `enter`, `exit`, `getDepth`, `isParent`, `ancestors`, `ancestor`, `hasAncestor`. |

#### `org.jmouse.xml`

| Type | Responsibility / contract |
|---|---|
| [NamespaceConfig](../jmouse-materializer/src/main/java/org/jmouse/xml/NamespaceConfig.java) | Operations: `getNamespaceFor`. |
| [NamespaceScope](../jmouse-materializer/src/main/java/org/jmouse/xml/NamespaceScope.java) | Operations: `push`, `pop`, `declareIfNeeded`. |
| [XmlPrinter](../jmouse-materializer/src/main/java/org/jmouse/xml/XmlPrinter.java) | Operations: `toString`, `toPrettyString`. |

#### `org.jmouse.xml.materializer`

| Type | Responsibility / contract |
|---|---|
| [XmlDomMaterializer](../jmouse-materializer/src/main/java/org/jmouse/xml/materializer/XmlDomMaterializer.java) | Implements/extends `AbstractTemplateMaterializer`. Operations: `document`. |
| [XmlRenderingPipeline](../jmouse-materializer/src/main/java/org/jmouse/xml/materializer/XmlRenderingPipeline.java) | Implements/extends `AbstractRenderingPipeline`. Operations: `builder`. |
| [XmlThemeModule](../jmouse-materializer/src/main/java/org/jmouse/xml/materializer/XmlThemeModule.java) | Implements/extends `ThemeModule`. Operations: `contributeTemplates`, `transformers`, `hooks`. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [DemoModels.java](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/smoke/DemoModels.java).
- [Smoke4.java](../jmouse-materializer/src/main/java/org/jmouse/dom/meterializer/smoke/Smoke4.java).
