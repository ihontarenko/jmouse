# jmouse-access-el — стан

Усе з початкового списку закрито. `Smoke` проходить: **143 перевірки, всі тримаються**.

```
mvn -o -pl jmouse-access-el -am -DskipTests compile
java -cp "jmouse-access-el/target/classes;<deps>" org.jmouse.access.el.Smoke
```

## Що було в списку

- [x] **Пріоритизація** — перевірена, правильна. `GRANT(2000) < ROLE_ASSIGNMENT(2100) <
      SCOPE_DECLARATION(2200) < PERMISSION_DECLARATION(2300) < INCLUDE(2400)`, фрагменти
      (`SINGLE_SCOPE`, `PERMISSION_VALUE`) — останні. Довша форма завжди питається раніше за свій
      власний префікс. Матчери до того ж диз'юнктні, тож порядок тут — страховка, а не механізм; це
      вже записано в javadoc `ParserPriority`.
- [x] **Javadoc + код-ревʼю** — див. «Що виправлено» нижче.
- [x] **Синтаксис зі смоук-файла** — розібраний повністю, включно з `${…}`, `@SCOPE:*`, `form:*`,
      `when`, коментарями в кінці рядка.
- [x] **Смоук перевіряє, що все вмапилось** — поле за полем, а не «розпарсилось». Плюс round-trip:
      файл переписується через `toSource()`, читається знову і перевіряється тим самим набором.

## Що виправлено

| Де | Що було |
|---|---|
| `PolicyDocumentParser` *(новий)* | ⚠️ **Файл без обгортки `policy { }` читався до першої декларації і мовчки губив решту.** `role` + `subject` у корені файлу давали тільки `role`. Тепер корінь мови — свій парсер документа, який читає до кінця файлу; зайве після `policy { }` — гучна відмова |
| `SourceWriter` *(новий)* | `toSource()` друкував імена без лапок: `@SPACE:'my-space'` → `@SPACE:my-space`, який лексер читає як віднімання. Тепер ім'я пишеться голим лише там, де лексер прочитає його назад цілим |
| `GrantParser` | `when` читався `OperatorParser`'ом, який зупиняється перед `?` — тернарна умова обрізалась мовчки. Тепер повний `ExpressionParser` |
| `ConditionDialect` | тести з аргументами (`is starts('admin')`, `is hasAny('DRAFT','LIVE')`) не компілювались — бракувало `ArgumentsParser`. Це половина сенсу тестів |
| `ScopeDeclarationParser` | невідомий атрибут (`@SPACE place param=x`) тихо не споживався і падав рядком далі. Тепер називає атрибут, який не приймає |
| `PolicyNode` та інші | `evaluate()` кастив `(List<PolicyScopeDeclaration>)` з `@SuppressWarnings`. Тепер типізовані `toDocument()` / `toRole()` / `toSubject()` / `toScopeDeclarations()` / `toPermissionDeclarations()`, кастів немає |
| `PolicyBlockNode` | `%n` у рендері — платформозалежний перенос у тексті `.jmp` файлу. Тепер `\n` |
| `AbstractParser` → `PolicyBlockParser` | у пакеті було два `AbstractParser` — свій і фреймворковий, половина класів успадковувала кожен |
| `ExpressionEvaluator` | `parse(source, documentName)` для файлу без заголовка, і `rewrite(source)` — нормалізований round-trip для контрольної кімнати |

## Індекс у квадратних дужках

Правильна форма — **без лапок**: `resource[status]`, `resource.tags[0]`. Обидві працюють.

`resource['status']` — не працює: воно **компілюється**, а падає на кожному запиті
(`NullPointerException: Required value must be non-null: 'getter'`). Умова, яка проходить старт і
кидає на запиті — найгірший спосіб помилитися в правилі доступу, тож `ConditionVocabulary` тепер
відмовляє в ній лексично, на завантаженні, і в повідомленні пише, як треба:

```
condition 'resource['status'] == …' quotes an index: write '[status]' rather than '['status']'.
The quoted form loads and then fails on every request, which is a worse answer than this one
```

Це єдина форма, у якій усі токени дозволені, а сама вона — ні.
