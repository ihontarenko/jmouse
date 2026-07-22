# 🐭 jMouse Expression Language (EL)

jMouse EL is a lightweight, expressive, and extensible expression language for evaluating and transforming Java data at runtime. It ships in two complementary parts:

- **Expression evaluator** — evaluate expressions programmatically against a Java context
- **Template engine** — a Twig/Jinja-style template engine built on top of the same EL core

---

## 🚀 Getting Started — Expression Evaluator

```java
ExpressionLanguage el = new ExpressionLanguage();
EvaluationContext ctx = el.newContext();

User user = new User();
user.setName("John");
ctx.setValue("user", user);

Object result = el.evaluate("user.name ?? 'Guest'", ctx); // -> "John"
```

### Evaluation overloads

```java
// Untyped — returns Object
Object value = el.evaluate("1 + 2", ctx);

// Typed — converts result to target type
String name = el.evaluate("user.name | upper", ctx, String.class);

// With inline data
Integer sum = el.evaluate("x + y", ctx, Map.of("x", 3, "y", 4), Integer.class);

// No context needed (pure expressions)
Object pi = el.evaluate("22f / 7");

// Reuse a pre-parsed AST — skips parsing on every call
Expression ast = el.compile("user.name | upper");
Object result2 = ast.evaluate(ctx);

// Shared singleton (no new extensions can be added after startup)
ExpressionLanguage shared = ExpressionLanguage.getSingleton();
```

---

## 📖 Expression Syntax

### Literals

```
42            // int
42L           // long
3.14          // double (decimal point)
22f           // float
7d            // double (suffix d)
44c           // char (suffix c)
10B           // byte
5S            // short
'hello'       // String (single-quoted)
"hello"       // String (double-quoted)
true / false
null / none
```

### Collections

```
[1, 2, 3]                         // array / list literal
{ 'key': value, 'x': 42 }        // map literal
{{}}                              // empty map
```

### Property access

```
user.name                         // bean getter (getName())
user.address.city                 // nested
map['key']                        // map subscript
list[0]                           // list index
```

### Variable Alias

```
$A1:long-variable-name
```

equivalent

```
set('A1', get('long-variable-name'))
```

### Bean access — `@`

Reach out to a container-managed bean by name and either call a method, read a
static constant, or read an instance field. The bean is resolved at runtime from
a `BeanLookup` attached to the evaluation context (see wiring below).

```
@securityService.hasRole('ADMIN')    // method call — invokes hasRole("ADMIN")
@cart.getTotal()                     // no-argument method call
@pricing#TAX_RATE                    // constant access — reads the static field TAX_RATE
@user:$email                         // field access — reads the instance field `email`
```

| Syntax | Meaning | Resolves to |
|--------|---------|-------------|
| `@bean.method(args)` | Method call | `bean.method(args)` |
| `@bean#NAME` | Constant access | value of the static field `NAME` |
| `@bean:$name` | Field access | value of the instance field `name` |

Bean access composes with the rest of the language — pipe it, do arithmetic, etc.:

```
@cart.getTotal() * @pricing#TAX_RATE | round
@securityService.hasRole('ADMIN') ? 'Welcome' : 'Access denied'
```

**Wiring up the bean lookup.** The default `EvaluationContext` implements
`BeanLookupContext`. In a real application you delegate to the DI container; the
example below adapts a `BeanContext`:

```java
ExpressionLanguage el = new ExpressionLanguage();
EvaluationContext context = el.newContext();

if (context instanceof BeanLookupContext lookupContext) {
    lookupContext.setBeanLookup(new BeanLookup() {
        @Override
        public <T> T getBean(Class<T> beanClass) {
            return beanContext.getBean(beanClass);
        }

        @Override
        public <T> T getBean(String beanName, Class<T> beanClass) {
            return beanContext.getBean(beanName, beanClass);
        }
    });
}

Boolean isAdmin = el.evaluate("@securityService.hasRole('ADMIN')", context, Boolean.class);
String  email   = el.evaluate("@user:$email", context, String.class);
Double  taxRate = el.evaluate("@pricing#TAX_RATE", context, Double.class);
```

> 💡 If the named field, constant, or method does not exist on the resolved bean,
> a `BeanAccessException` is thrown.

### Arithmetic

| Operator | Meaning      | Precedence |
|----------|-------------|-----------|
| `+`      | add          | 600 |
| `-`      | subtract     | 600 |
| `*`      | multiply     | 700 |
| `/`      | divide       | 700 |
| `%`      | modulus      | 700 |
| `**`     | exponentiate | 800 |

```
2 ** 10             // -> 1024
22f / 7             // -> 3.142857 (float division)
10 % 3              // -> 1
```

### Comparison

All aliases are interchangeable tokens:

```
a == b   a = b    a eq b    a equals b
a != b   a <> b   a ne b    a neq b
a > b    a gt b
a >= b   a gte b  a ge b
a < b    a lt b
a <= b   a lte b  a le b
```

### Logical

```
a and b   a && b
a or b    a || b
!a
a ^ b       // XOR
```

### String concatenation — `~`

```
'Hello' ~ ', ' ~ user.name ~ '!'
user.name ~ '22' | upper          // concat then pipe to filter
```

### Range — `..`

```
1..10                             // integer range [1 .. 10]
(1..10) | filter(n -> n is odd)   // [1, 3, 5, 7, 9]
```

### Null coalescing — `??`

```
user.name ?? 'Guest'              // returns 'Guest' when name is null
```

### Ternary

```
age >= 18 ? 'Adult' : 'Minor'
```

### Increment / Decrement

```
i++
i--
```

### Pipe — `|`

Passes a value through a filter (left-to-right):

```
name | upper
[1, 2, 3] | join(', ')
list | filter(x -> x > 0) | first
123 | double / 7
```

### Test — `is`

Runs a boolean test against a value:

```
42 is even
list is type('collection')
name is starts('J')
name is ends('e')
value is null
```

### Lambdas

```
x -> x * 2                        // single parameter
(a, b) -> a + b                   // multiple parameters
() -> 'hello'                     // no parameters
() -> {}                          // returns null
() -> {{}}                        // returns empty map
n -> n is odd                     // using a test
```

### Maps with dynamic keys

```
{ user.name | upper : [1, 2, 3], '_default' : [0, 1] }
```

### Array arithmetic

```
[1, 2, 3] + 4                     // append -> [1, 2, 3, 4]
[1, 2, 3] - 2                     // remove -> [1, 3]
'name' * 3                        // repeat string -> 'namename­name'
```

---

## 🛠️ Built-in Functions

| Function | Description |
|----------|-------------|
| `set(name, value)` | Declare or update a context variable; returns the value |
| `min(a, b)` | Minimum of two numbers |
| `max(a, b)` | Maximum of two numbers |
| `ucfirst(str)` | Uppercase first character |
| `uclast(str)` | Uppercase last character |
| `lcfirst(str)` | Lowercase first character |
| `lclast(str)` | Lowercase last character |
| `class('fqcn')` | Resolve a Java `Class` by fully-qualified name |

### `set` — declare a variable or a lambda

```
set('pi', 22 / 7)
set('username', user.name)

set('tag', (name, value) -> '<' ~ name ~ '>' ~ value ~ '</' ~ name ~ '>')
tag('title', 'jMouse')            // -> <title>jMouse</title>

set('getNumberType', v -> v | int is even ? 'Even' : 'Odd')
getNumberType(5)                  // -> 'Odd'
```

### `set` with dynamic context key

```
set(user.status.status, true)     // key is resolved from the context at runtime
```

---

## 🔍 Built-in Filters

Applied via `value | filterName` or `value | filterName(args)`.

### String

| Filter | Example | Result |
|--------|---------|--------|
| `upper` | `'hello' \| upper` | `HELLO` |
| `lower` | `'HELLO' \| lower` | `hello` |
| `trim` | `'  hi  ' \| trim` | `hi` |
| `sub(begin)` | `'hello' \| sub(2)` | `llo` |
| `sub(begin, end)` | `'hello' \| sub(1, 3)` | `el` |
| `split(delimiter)` | `'a,b,c' \| split(',')` | `['a','b','c']` |
| `length` | `'hello' \| length` | `5` |

### Collection / iteration

| Filter | Example | Result |
|--------|---------|--------|
| `filter(lambda)` | `(1..10) \| filter(n -> n is odd)` | `[1,3,5,7,9]` |
| `map(lambda)` | `[1,2,3] \| map(n -> n * 2)` | `[2,4,6]` |
| `join(sep)` | `[1,2,3] \| join(', ')` | `1, 2, 3` |
| `join(sep, prefix, suffix)` | `[10,20,30] \| join(':', '<', '>')` | `<10:20:30>` |
| `first` | `['a','b'] \| first` | `a` |
| `last` | `['a','b'] \| last` | `b` |
| `length` | `[1,2,3] \| length` | `3` |

### Utility

| Filter | Description |
|--------|-------------|
| `type` | Fully-qualified class name of the value |
| `class` | `Class` object of the value |
| `default(fallback)` | Returns fallback when value is null or empty |
| `sout` | Prints value to stdout and passes it through (debug) |

### Type conversion

```
value | boolean
value | byte
value | short
value | int
value | long
value | float
value | double
value | char
value | bigint
value | bigdecimal
value | string
value | list
value | array
value | iterator
```

---

## 🧪 Built-in Tests

Applied via `value is testName` or `value is testName(args)`.

| Test | Example |
|------|---------|
| `even` | `4 is even` → `true` |
| `odd` | `3 is odd` → `true` |
| `null` | `value is null` |
| `array` | `value is array` |
| `collection` | `value is collection` |
| `iterable` | `value is iterable` |
| `map` | `value is map` |
| `starts('prefix')` | `name is starts('J')` |
| `ends('suffix')` | `name is ends('e')` |
| `type('typeName')` | `value is type('collection')` |
| `containsAll(v1, v2)` | `list is containsAll(1, 2)` |
| `containsAny(v1, v2)` | `list is containsAny(5, 6)` |
| `containsNone(v1, v2)` | `list is containsNone(99)` |

Type names accepted by `is type(...)`: `string`, `numeric`, `iterable`, `collection`, `array`, `enum`, `byte`, `short`, `char`, `int`, `long`, `float`, `double`, `boolean`, `list`, `set`, `map`.

---

## 🧩 Extending the EL

### Import a class of static methods

```java
// All public static methods of Strings become top-level EL functions
MethodImporter.importMethod(Strings.class, ctx);

// With a namespace — callable as "math:sqrt(x)" inside expressions
MethodImporter.importMethod(MathUtils.class, "math", ctx);

// Instance methods
MethodImporter.importMethod(myService, MyService.class, ctx);
```

### Implement a custom Extension

```java
public class MyExtension implements Extension {
    @Override public List<Function> getFunctions() { return List.of(new MyFunction()); }
    @Override public List<Filter>   getFilters()   { return List.of(); }
    @Override public List<Test>     getTests()     { return List.of(); }
    @Override public List<Operator> getOperators() { return List.of(); }
}

el.getExtensions().importExtension(new MyExtension());
```

### i18n extension (optional, not loaded by default)

```java
el.getExtensions().importExtension(new i18nExtension());
// Then in expressions:
// i18n('messages', 'greeting.hello', user.name)
```

---

## 🌐 Template Engine

The template engine uses the same EL core and adds a Twig/Jinja-style tag system.

### Bootstrap

```java
TemplateEngine engine = new TemplateEngine();
engine.setLoader(new ClasspathLoader());   // loads from classpath

Template template = engine.getTemplate("templates/page.html");

EvaluationContext context = template.newContext();
context.setValue("user", myUser);

Renderer renderer = new TemplateRenderer(engine);
Content content = renderer.render(template, context);
String html = content.toString();
```

Load from a plain string instead of a file:

```java
engine.setLoader(new StringLoader());
Template template = engine.getTemplate("<h1>{{ title }}</h1>");
```

### Delimiters

| Delimiter | Purpose |
|-----------|---------|
| `{{ expr }}` | Print the result of an expression |
| `{% tag ... %}` | Control tag |
| `{# comment #}` | Comment — not rendered |
| `{! code !}` | Java code block |

### Tag reference

#### `set` — assign a variable

```twig
{% set greeting = 'Hello, ' ~ user.name %}
{{ greeting }}
```

#### `do` — evaluate without output

```twig
{% do set('counter', 0) %}
```

#### `if` / `elseif` / `else` / `endif`

```twig
{% if user.age >= 18 %}
  <p>Welcome, adult.</p>
{% elseif user.age >= 13 %}
  <p>Welcome, teen.</p>
{% else %}
  <p>Too young.</p>
{% endif %}
```

#### `for` / `else` / `endfor`

```twig
{% for item in items %}
  <li>{{ loop.index }}. {{ item.name }}</li>
{% else %}
  <li>No items found.</li>
{% endfor %}
```

Loop variable properties available inside `for`:

| Variable | Description |
|----------|-------------|
| `loop.index` | 1-based iteration counter |
| `loop.index0` | 0-based iteration counter |
| `loop.first` | `true` on the first iteration |
| `loop.last` | `true` on the last iteration |
| `loop.key` | Current key when iterating a Map |
| `loop.value` | Current value when iterating a Map |

#### `include`

```twig
{% include 'partials/header.html' %}
```

#### Template inheritance — `extends` / `block` / `parent`

Base template (`base.html`):
```twig
<html>
<body>
  {% block content %}Default content{% endblock %}
  {% block footer %}Default footer{% endblock %}
</body>
</html>
```

Child template:
```twig
{% extends 'base.html' %}

{% block content %}
  <h1>Hello, {{ user.name }}</h1>
  {% parent content %}
{% endblock %}
```

`{% parent blockName %}` renders the parent template's block content inline.

#### `macro` / `endmacro`

```twig
{% macro input(name, value='', type='text') %}
  <input type="{{ type }}" name="{{ name }}" value="{{ value }}">
{% endmacro %}

{{ input('email', user.email, 'email') }}
```

#### `use` — import macros or blocks from another template

```twig
{% use 'forms.html' get input %}
{% use 'forms.html' get block as forms_block %}
{% use 'forms.html' get macro import input, button %}
```

#### `apply` — apply filters to a block

```twig
{% apply upper | trim %}
  hello world
{% endapply %}
```

#### `embed` — embed with block overrides

```twig
{% embed 'card.html' with { title: 'My Card' } %}
  {% block body %}Custom body content{% endblock %}
{% endembed %}
```

#### `scope` — scoped variable block

```twig
{% scope with { name: 'jMouse' } %}
  <p>{{ name }}</p>
{% endscope %}
```

#### `render` — force-render a named block

```twig
{% render 'sidebar' %}
```

#### `cache` — cache rendered block output

```twig
{% cache 'sidebar-' ~ user.id %}
  {% include 'partials/sidebar.html' %}
{% endcache %}
```

---

## 📦 License

MIT © Ivan Hontarenko (Mr. Jerry Mouse)  
📧 ihontarenko@gmail.com
