# 🐭 jMouse Expression Language (EL)

jMouse EL is a lightweight, expressive, and extensible expression language designed for concise evaluation and transformation of Java data at runtime.

It supports:
- 🧮 Math and logic operations
- 🔗 Property navigation and null-safe access
- 🧰 Built-in functions (`set`, `isEmpty`, `type`, `join`, etc.)
- 🔁 Lambda expressions and function pipelines
- 🧬 Type coercion and filtering
- 🧠 Context-aware evaluation with dynamic variables
- 🌍 i18n-ready and fully extensible

---

## 🚀 Getting Started

```java
ExpressionLanguage el = new ExpressionLanguage();
EvaluationContext ctx = el.newContext();

User user = new User();
user.setName("John");
ctx.setValue("user", user);

Object result = el.evaluate("user.name ?? 'Guest'", ctx); // -> "Ivan"
```

---

## 🔥 Expression Examples

Below are examples of real expressions and use cases.

---

### 💡 Conditional Evaluation

```javascript
user.name ?? 'Guest'               # null-safe fallback
user.age >= 18 ? 'Adult' : 'Minor'
```

---

### 🔄 Loops, Lambdas & Filters

```javascript
(1..10) | filter(n -> n is odd)    # [1, 3, 5, 7, 9]
[1, 2, 3, 4] | map(i -> i * 2)     # [2, 4, 6, 8]
```

---

### 🧮 Math & Type Operations

```javascript
22f / 7                             # 3.14...
1 + (2 * 2) > 5 and 4 < 5 - 2 / 3
123 | double / 7                    # Floating point division
[1, 2, 3] + 4                       # [1, 2, 3, 4]
[1, 2, 3] - 2                       # [1, 3]
```

---

### 🔤 String Manipulation

```javascript
user.name ~ '22' | upper           # Concatenate and uppercase
'name' * 3                         # Repeat string 3 times
[10, 20, 30] | join(':', '<', '>') # Join elements into <10:20:30>
```

---

### 🧪 Type Checks

```javascript
list is type('collection')        # true
123 is type('collection')         # false
```

---

### 🌐 Maps & Collections

```javascript
{ user.name | upper : [1, 2, 3], '_default' : [0, 1] }
```

---

### 🛠️ Custom Functions

```javascript
set('tag', (name, value) -> '<' ~ name ~ '>' ~ value ~ '</' ~ name ~ '>')
tag('title', 'jMouse')            # -> <title>jMouse</title>
```

```javascript
set('toString', (v) -> v | string)
set('getNumberType', v -> v | int is even ? 'Even' : 'Odd')
getNumberType(5)                  # 'Odd'
```

---

### 🔂 Anonymous Functions

```javascript
() -> 'hello'                     # string 'hello'
() -> {}                          # null
() -> {{}}                        # empty map
['John', 'Kratos', 'Jarvis'] | filter(s -> s is starts('K')) | first # Kratos
```

---

### 🧩 Dynamic Data

```javascript
set('a', [1, 2f, 3d, 44c, 5])
a[3]                              # -> 44 (char)
```

```javascript
set('pi', 22 / 7)
set('username', user.name)
```

---

### 🔍 Context Manipulation

```javascript
set('result', isEmpty(''))              # true
set(user.status.status, isEmpty(''))    # dynamic key
```

---


---

## 🧩 DSL Features

- `??` — null-coalescing fallback
- `|` — pipe operator
- `~` — string concatenation
- `..` — range creation
- `? :` — ternary expression
- `set` — declare or mutate context variable
- `is` — comparison or function match (e.g., `is odd`, `is ends('e')`)
- `type('...')` — type check
- `filter`, `map`, `join`, `cut`, `length`, `charAt`, `upper`, etc.

---

## 📚 Extension-Friendly

You can import custom method classes or bind lambdas at runtime:

```java
MethodImporter.importMethod(Strings.class, context.getExtensions());
```

---

## 📦 License

MIT © Ivan Hontarenko (Mr. Jerry Mouse)  
📧 ihontarenko@gmail.com