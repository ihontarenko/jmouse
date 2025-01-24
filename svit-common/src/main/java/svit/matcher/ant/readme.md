# AntMatcher

AntMatcher — це утиліта для порівняння рядків за Ant-подібними шаблонами. Вона дозволяє перевіряти шляхи, що містять спеціальні символи, такі як `*` та `**`. Ось покрокове пояснення, як працює перевірка:

## Приклад

### Шаблон:
```
/repository/**/app/**/a/*/c/*.java
```

### Рядок:
```
/repository/com/app/a/b/c/a/b/c/a/b/c/Main.java
```

### Результат:
```
true
```

---

## Як працює перевірка

1. **Розбиття шаблону та рядка на сегменти**:
    - Шаблон розбивається на сегменти: `["repository", "**", "app", "**", "a", "*", "c", "*.java"]`.
    - Рядок розбивається на сегменти: `["repository", "com", "app", "a", "b", "c", "a", "b", "c", "a", "b", "c", "Main.java"]`.

2. **Покрокове порівняння**:
    - **`repository`**: збігається точно.
    - **`**`**: пропускає будь-яку кількість сегментів (`com`).
    - **`app`**: збігається з наступним сегментом `app`.
    - **`**`**: пропускає сегменти до `a`.
    - **`a`**: збігається з `a`.
    - **`*`**: збігається з наступним сегментом `b`.
    - **`c`**: збігається з `c`.
    - **`*.java`**: перевіряє, чи рядок закінчується на `.java`. У нашому випадку це `Main.java`.

3. **Повернення результату**:
    - Всі сегменти шаблону були успішно порівняні з сегментами рядка.
    - Повертається `true`.

---

# AntMatcher

The AntMatcher utility allows you to compare strings against Ant-style patterns. It supports path matching with special symbols like `*` and `**`. Below is a step-by-step explanation of how the matching works:

## Example

### Test Code
```java
public static void main(String[] args) {
   Matcher<String> ant    = new AntMatcher("/repository/**/app/**/a/*/c/*.java");
   boolean         result = ant.matches("/repository/com/app/a/b/c/a/b/c/a/b/c/Main.java");
   System.out.println(result); // true
}
```

### Pattern:
```
/repository/**/app/**/a/*/c/*.java
```

### String:
```
/repository/com/app/a/b/c/a/b/c/a/b/c/Main.java
```

### Result:
```
true
```

---

## How Matching Works

1. **Splitting the Pattern and String into Segments**:
    - Pattern is split into segments: `["repository", "**", "app", "**", "a", "*", "c", "*.java"]`.
    - String is split into segments: `["repository", "com", "app", "a", "b", "c", "a", "b", "c", "a", "b", "c", "Main.java"]`.

2. **Step-by-Step Comparison**:
    - **`repository`**: matches exactly.
    - **`**`**: skips any number of segments (`com`).
    - **`app`**: matches the next segment `app`.
    - **`**`**: skips segments until it finds `a`.
    - **`a`**: matches `a`.
    - **`*`**: matches the next segment `b`.
    - **`c`**: matches `c`.
    - **`*.java`**: checks if the string ends with `.java`. In this case, it’s `Main.java`.

3. **Returning the Result**:
    - All pattern segments are successfully matched with string segments.
    - Returns `true`.

