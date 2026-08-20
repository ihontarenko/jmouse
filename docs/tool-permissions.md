# Tool permissions · Пермішини тулів

> How an agent is authorized in every jMouse product. One page, two languages — the Ukrainian half is
> below and says the same thing.
>
> Applies to: `jmouse-ai`, `jmouse-access-spring-boot`, and the products that consume them —
> Tessera, Innoventa, WiQ. Identity is not converted yet.

---

## English

### The one sentence

**A tool is the agent's endpoint, the way a controller is a person's.** Each surface has its own
vocabulary, and neither asks the other's question.

| Surface | Who uses it | What gates it | Declared in |
|---|---|---|---|
| HTTP controller | a person, in a browser | `issue:create`, `page:write`, `entry:delete` … | the product's own `.jmp` |
| Tool (MCP) | an agent, or a person at the assistant | `tool:issues_create`, `tool:pages_update` … | `policy/tools.jmp` |

A tool call asks **one** permission: the tool one. It does **not** also have to satisfy the product
permission for whatever the action happens to touch. So a hypothetical `entries_manage_all` costs
exactly `tool:entries_manage_all` — not `entry:delete` plus `form:delete` plus `file:delete` one by
one. That is the point of a second vocabulary rather than a second check.

### The name is derived, never written in Java

An action declares its arguments and its handler and says nothing about authorization:

```java
ToolAction.builder()
        .toolName(toolName())      // "issues"
        .name("create")            // → tool:issues_create
        .title("Raise an issue")
        .inputSchema(…)
        .handler(this::handleCreate)
        .build();
```

`ToolAction.Builder` derives `tool:<toolName>_<name>` when nothing is declared. So the name exists in
exactly one written place: `policy/tools.jmp`.

⚠️ **Forgetting the line there stops the application.** `ToolCatalog` refuses to build over a
vocabulary that lacks what its actions ask for, and the refusal names the action, the permission and
the exact line to add. The alternative would be an action published with a permission nobody can hold:
visible to a model, planned with, refused on the last step, forever, with nothing saying why.

### Where the vocabulary comes from

`declare permissions` in the policy documents **is** the list of permissions.
`AccessPolicyAutoConfiguration` reads `PermissionCatalog` off the merged documents, and
`AiAccessAutoConfiguration` bridges that into the tool library's `PermissionVocabulary`.

⚠️ **A product must never declare a `PermissionCatalog` or `PermissionVocabulary` bean.** Both existed
in two products and **shadowed** the library's, so the access engine and the tool library held two
different ideas of the vocabulary — and adding an axis to one of them failed a boot naming twenty-four
permissions "that do not exist" while they sat, declared, in a file two directories away.

### An agent's authority

Two values, and they are **different accounts** rather than two points on one scale.

| Authority | Whose permissions are asked | Notes |
|---|---|---|
| `INHERITED` | **the owner's** | The agent *is* that person for the length of a call. ⚠️ So the owner must hold the tool permissions themselves — assigning `MCP_AGENT_TOOLS` to *yourself* is what makes your inherited agent work. |
| `RESTRICTED` | **the agent's own** | Its own roles and permissions, **not capped by the owner**. It may hold what the person it acts for does not. |

⚠️ There is no ceiling and no intersection. `AgentCallers` reads the flag once, in the library, and
everything downstream sees one ordinary subject.

### What decides *which rows*

The tool permission says which **action** may be reached. It never says which records.

Which records is the caller's ordinary access, resolved by the product's scope resolver — a project it
cannot browse, or a section it was not granted, never becomes a scope at all. So the two knobs are
independent, both are rows, and both are edited on a screen at runtime:

```
"close this section to everybody except her"   → a grant or deny on the branch    (access screen)
"no model may rewrite pages at all"            → tool:pages_update                (agents screen)
```

Neither can override the other.

⚠️ **A `RESTRICTED` agent holding only tool permissions therefore sees nothing** — no project, no
section. That is not a bug: restricting an agent means giving it its own ordinary permissions too.

### The trade-off, stated plainly

An agent can be given far-reaching access with one checkbox, and **nothing subtracts it afterwards**.
That is deliberate: what was bought is that there are no ceilings and no non-obvious behaviour — the
answer to "why did this not work" is always *a permission is missing, and the refusal names it*.

⚠️ **What replaced the owner ceiling is the trail, and nothing else.** `ai_tool_calls` records every
call with its caller, its action and its outcome. A mistake in grants used to be caught by the
intersection with the owner; now it is caught by somebody reading the log.

### Adding a tool

1. Write the action. Declare no permission.
2. Add one line to `policy/tools.jmp`: `tool:<toolname>_<action>  "What it does"`.
3. Decide whether it joins `MCP_AGENT_TOOLS` — the "everything at once" bundle — or is granted on its
   own. Anything that loses work is granted on its own.

Skip step 2 and the application will not start, and will tell you what to paste.

---

## Українською

### Одним реченням

**Тул — це ендпоінт для агента так само, як контролер — ендпоінт для людини.** Кожна поверхня має
власний словник, і жодна не питає питання іншої.

| Поверхня | Хто користується | Чим гейтиться | Де оголошено |
|---|---|---|---|
| HTTP-контролер | людина, у браузері | `issue:create`, `page:write`, `entry:delete` … | продуктовий `.jmp` |
| Тул (MCP) | агент, або людина в асистенті | `tool:issues_create`, `tool:pages_update` … | `policy/tools.jmp` |

Виклик тула питає **один** пермішин — тульний. Він **не мусить** додатково задовольняти продуктові
пермішини всього, чого дія торкається. Тобто гіпотетичний `entries_manage_all` коштує рівно
`tool:entries_manage_all`, а не `entry:delete` плюс `form:delete` плюс `file:delete` поштучно. У цьому
й сенс другого словника, а не другої перевірки.

### Ім'я виводиться, у Java його немає

Дія оголошує аргументи й хендлер і не каже про авторизацію нічого:

```java
ToolAction.builder()
        .toolName(toolName())      // "issues"
        .name("create")            // → tool:issues_create
        .title("Raise an issue")
        .inputSchema(…)
        .handler(this::handleCreate)
        .build();
```

`ToolAction.Builder` виводить `tool:<toolName>_<name>`, якщо нічого не вказано. Тож ім'я написане
рівно в одному місці — `policy/tools.jmp`.

⚠️ **Забути там рядок = застосунок не стартує.** `ToolCatalog` відмовляється будуватись над словником,
у якому немає того, що просять його дії, і у відмові названо дію, пермішин і готовий рядок для
вставки. Альтернатива — тул із пермішином, якого ніхто не може тримати: модель його бачить, планує з
ним і отримує відмову на останньому кроці. Назавжди й без пояснень.

### Звідки береться словник

`declare permissions` у полісі **і є** списком пермішинів. `AccessPolicyAutoConfiguration` читає
`PermissionCatalog` зі злитих документів, а `AiAccessAutoConfiguration` пробрасує це в
`PermissionVocabulary` тул-бібліотеки.

⚠️ **Продукт ніколи не оголошує свій `PermissionCatalog` чи `PermissionVocabulary`.** Обидва існували
у двох продуктах і **затіняли** бібліотечні — рушій доступу і тул-бібліотека тримали два різні
уявлення про словник, і додавання осі в один із них поклало старт із повідомленням про двадцять чотири
пермішини, «яких не існує», поки вони лежали оголошені у файлі двома теками далі.

### Authority агента

Два значення, і це **різні акаунти**, а не дві точки на одній шкалі.

| Authority | Чиї пермішини питаються | Примітка |
|---|---|---|
| `INHERITED` | **власника** | Агент *є* цією людиною на час виклику. ⚠️ Тож власник має тримати тульні пермішини сам — щоб успадкований агент працював, `MCP_AGENT_TOOLS` призначається **собі**. |
| `RESTRICTED` | **власні агента** | Свої ролі й пермішини, **без стелі власника**. Може тримати те, чого немає в людини, від імені якої діє. |

⚠️ Жодної стелі й жодного перетину. `AgentCallers` читає прапорець один раз, у бібліотеці, і все нижче
бачить звичайного суб'єкта.

### Що вирішує, **які саме рядки**

Тульний пермішин каже, яку **дію** можна дістати. Він ніколи не каже, які записи.

Які записи — це звичайний доступ того, хто викликає, і його резолвить резолвер скоупу продукту:
проєкт, якого людина не бачить, або секція, якої їй не дали, взагалі не стає скоупом. Тож два ключі
незалежні, обидва — рядки, обидва редагуються на екрані в рантаймі:

```
«закрити цю секцію всім, окрім неї»      → грант або deny на гілці   (екран доступу)
«жодна модель не переписує сторінки»     → tool:pages_update          (екран агентів)
```

Жоден не перебиває другий.

⚠️ **`RESTRICTED`-агент, що тримає лише тульні пермішини, не бачить нічого** — ні проєкту, ні секції.
Це не баг: обмежити агента означає дати йому і власні звичайні пермішини теж.

### Компроміс, названий прямо

Агенту можна дати дуже широкий доступ однією галочкою, і **ніщо потім цього не відніме**. Це свідомо:
куплено те, що немає стель і немає неочевидної поведінки — відповідь на «чому не спрацювало» завжди
одна: *немає пермішина, і у відмові написано якого*.

⚠️ **Замість стелі власника лишився трейл, і більше нічого.** `ai_tool_calls` пише кожен виклик із
викликачем, дією і результатом. Помилку в грантах раніше ловив перетин із власником — тепер її ловить
тільки той, хто подивиться в журнал.

### Як додати тул

1. Написати дію. Пермішин не оголошувати.
2. Додати один рядок у `policy/tools.jmp`: `tool:<toolname>_<action>  "Що воно робить"`.
3. Вирішити, чи входить у `MCP_AGENT_TOOLS` — бандл «усе одразу» — чи видається окремо. Усе, що
   втрачає роботу, видається окремо.

Пропустити крок 2 — застосунок не підніметься і покаже, що вставити.
