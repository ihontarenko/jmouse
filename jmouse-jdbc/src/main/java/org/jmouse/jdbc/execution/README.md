# Declarative JDBC execution

Цей пакет містить незавершений напрямок для декларативного виконання JDBC-запитів: запит описується окремим класом або інтерфейсом, а виклик має вигляд:

```java
List<User> users = jdbc.query(GetUsersQuery.class, userId, groupName);
```

Мета — приховати SQL preparation, binding, mapper, cardinality та JDBC lifecycle за невеликим інтерфейсом. Клас запиту є декларацією/adapter-ом, а не місцем, де повторюється JDBC-код.

## Поточний стан

У пакеті вже є такі заготовки:

- `ExecutionDescriptor` — runtime descriptor із operation, SQL, binder, configurer, handler та mapper/extractor;
- `JdbcDescriptorOperations` — перший фасад для виконання descriptor-а;
- `SelfAnnotatedExecutionDescriptor` — задум для descriptor-ів, які отримують SQL з annotation;
- `GetUsers` — приклад descriptor-а.

Це поки лише skeleton:

- `ExecutionDescriptor.sql()` за замовчуванням повертає `null`;
- `JdbcDescriptorOperations` фактично виконує всі операції як `QUERY`;
- `switch` за `JdbcOperation` не реалізований;
- descriptor API не типізований результатом;
- немає resolver-а, instantiation policy, annotation contract, bean registration або tests;
- у модулі немає `src/test` для перевірки цього механізму.

Поточний код успішно компілюється, але його не слід вважати готовим public contract.

## Рекомендований public contract

Для query-ів краще мати окремий малий контракт, а не один descriptor для query/update/call/batch:

```java
public interface JdbcQuery<R> {
    QuerySpec<R> prepare(Object... arguments);
}

public record QuerySpec<R>(
        String sql,
        ParameterSource parameters,
        ResultSetExtractor<R> extractor
) {}

public interface QueryTemplate {
    <R> R query(Class<? extends JdbcQuery<R>> type, Object... arguments)
            throws SQLException;
}
```

Приклад конкретного неанотованого класу:

```java
public final class GetUsersQuery implements JdbcQuery<List<User>> {

    @Override
    public QuerySpec<List<User>> prepare(Object... arguments) {
        return new QuerySpec<>(
                "select id, name from users "
                        + "where id = ? and group_name = ?",
                new ArrayParameterSource(arguments),
                new ListResultSetExtractor<>(BeanRowMapper.of(User.class))
        );
    }
}
```

Виклик типізується generic-параметром класу:

```java
List<User> users = queries.query(GetUsersQuery.class, 42L, "admins");
```

`R` має описувати фактичну cardinality результату:

```java
JdbcQuery<List<User>>     // zero or more rows
JdbcQuery<Optional<User>> // zero or one row
JdbcQuery<User>            // exactly one row
JdbcQuery<Long>            // scalar/extracted result
```

Тому cardinality не повинна бути прихованою домовленістю всередині назви класу.

## Варіант з annotations

Анотації повинні описувати тільки metadata, а не містити runtime execution logic:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Sql {
    String value();
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Rows {
    Class<?> value();
}

@Sql("select id, name from users where id = ? and group_name = ?")
@Rows(User.class)
public interface GetUsersQuery extends JdbcQuery<List<User>> {
}
```

Такий інтерфейс не можна інстанціювати звичайним constructor reflection. Для нього потрібен `QueryResolver`, який:

1. читає annotation та generic result type;
2. будує `QuerySpec`;
3. створює `ParameterSource` з arguments;
4. вибирає `BeanRowMapper`, scalar mapper або custom extractor;
5. кешує immutable metadata за `Class<?>`.

Неанотований інтерфейс без default method, implementation або registry entry не має достатньо інформації для виконання. Це потрібно перевіряти одразу й завершувати зрозумілим exception, а не дозволяти `null` SQL.

## Параметри

### Позиційні parameters

Для API з `varargs` SQL використовує `?`, а resolver створює `ArrayParameterSource`:

```java
queries.query(GetUsersQuery.class, userId, groupName);
```

Потрібно перевіряти кількість аргументів до відкриття JDBC statement. `ArrayParameterSource` вже підтримує 1-based JDBC positions.

### Named parameters

Для SQL із `:userId` і `:groupName` краще приймати один record або bean:

```java
public record GetUsersArgs(long userId, String groupName) {}

@Sql("select * from users where id = :userId and group_name = :groupName")
public interface GetUsersQuery extends JdbcQuery<List<User>> {
}

List<User> users = queries.query(
        GetUsersQuery.class,
        new GetUsersArgs(userId, groupName)
);
```

Це використовує наявний `NamedTemplate`, `BeanParameterSource` та `NamedSqlPreparedExecutionFactory`. Для `varargs` named binding імена втрачаються, тому автоматично виводити `userId/groupName` із довільного `Object...` не слід.

## Розділення implementation

Рекомендований внутрішній pipeline:

```text
Class<?> + arguments
        |
        v
QueryDefinitionResolver
        |
        v
ResolvedQuery / QuerySpec
        |
        v
NamedTemplate or JdbcTemplate
        |
        v
JdbcExecutor + interceptors + transaction
```

`QueryTemplate` має бути composition-ом над існуючим `NamedTemplate`, а не ще одним великим інтерфейсом, який успадковує всі overload-и `JdbcTemplate`. Це зберігає малу seam для caller-а і locality для implementation.

Resolver повинен відповідати за:

- constructor/factory instantiation неанотованих query-класів;
- annotation lookup для query-інтерфейсів і класів;
- generic result type та mapper selection;
- параметри й перевірку їх кількості/імен;
- immutable metadata cache;
- нормалізацію exception до JDBC-модуля.

JDBC execution, transactions, SQL guards, timeouts та exception translation повинні залишитися в існуючому executor/interceptor pipeline.

## Інші operation-и

Після стабілізації query contract можна додати окремі типи:

```java
interface JdbcUpdate<R> { UpdateSpec<R> prepare(Object... args); }
interface JdbcCall<R>   { CallSpec<R> prepare(Object... args); }
```

Не варто повертати `Object` і перемикати всі operation-и в одному `ExecutionDescriptor`. Для update потрібно окремо описати update count/generated keys, для batch — список parameter sets, для call — IN/OUT bindings.

## Error contract

Resolver має fail fast у таких випадках:

- annotation відсутня, але клас не реалізує executable contract;
- відсутній SQL або mapper/extractor;
- кількість positional arguments не збігається з SQL;
- named parameter не знайдений у bean/map;
- query-інтерфейс має неоднозначні або несумісні annotations;
- неможливо створити неанотований class;
- generic result type не можна визначити.

Повідомлення мають містити query class, SQL name/annotation та конкретний parameter або result type.

## План реалізації

1. Зафіксувати `JdbcQuery<R>`, `QuerySpec<R>` та `QueryTemplate`.
2. Реалізувати `ReflectiveQueryResolver` для класів із no-arg constructor або factory.
3. Додати `@Sql`, `@Rows` і cardinality annotations лише після стабілізації programmatic contract.
4. Реалізувати positional і named parameter adapters через уже наявні `ArrayParameterSource`/`BeanParameterSource`.
5. Додати cache resolved metadata з thread-safe immutable entries.
6. Зареєструвати `QueryTemplate` у JDBC configuration як окремий bean.
7. Додати tests на class query, annotated interface, named parameters, scalar/optional/list cardinality, invalid metadata та cache.
8. Лише потім вирішувати, чи залишати `ExecutionDescriptor`, перейменувати його на internal `ResolvedQuery`, або адаптувати до нового contract-а.

## Критерії готовності

Механізм можна вважати готовим, коли один caller знає лише `QueryTemplate.query(Class, arguments)`, а всі SQL preparation, binding, mapper, cardinality, validation, caching та JDBC lifecycle виконуються всередині module. Додавання нового query не повинно вимагати змін у `JdbcTemplate`, `JdbcExecutor` або global switch.

