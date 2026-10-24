# Task REST API — SIS 1 + SIS 2

Spring Boot REST API для управления задачами (`Task`).

- **SIS 1** — CRUD, DTO + Bean Validation, единый формат ошибок, корректные HTTP-статусы, Maven-профили, HTTP-client коллекция.
- **SIS 2** — слоистая архитектура с маппингом DTO ↔ domain ↔ entity и тесты: unit (сервис), MockMvc slice (веб), интеграционный (репозиторий на настоящей БД).

## Стек

Java 17, Spring Boot 3.3.4, Spring Web, Spring Data JPA, Bean Validation, Lombok, springdoc (Swagger UI),
H2 (dev/test), PostgreSQL (prod и интеграционный тест репозитория), JUnit 5, Mockito, AssertJ, MockMvc, JaCoCo.

## Архитектура (SIS 2)

```
HTTP ──► web ──────────► service ───────────► persistence ──► БД
         (DTO)           (domain-объекты)     (JPA entity)
```

| Слой | Пакет | Отвечает за | Знает о |
|---|---|---|---|
| **web** | `web.controller`, `web.dto`, `web.mapper`, `web.exception` | HTTP: статусы, JSON, валидация (`@Valid`), единый формат ошибок | `domain`, `service` |
| **service** | `service`, `service.impl` | бизнес-логика: дубли title, значения по умолчанию, транзакции | `domain`, `persistence` |
| **domain** | `domain.model`, `domain.exception` | чистые Java-объекты `Task`, `TaskStatus`, `TaskPriority`, исключения `ResourceNotFound/Conflict` | ничего (ни HTTP, ни JPA) |
| **persistence** | `persistence.entity`, `persistence.repository`, `persistence.mapper` | хранение: JPA-сущность `TaskEntity`, `TaskRepository` | `domain` |

**Три модели и два маппера** — каждый слой видит только «свой» объект:

```
TaskRequestDto ──TaskDtoMapper──► Task (domain) ──TaskEntityMapper──► TaskEntity ──► tasks (таблица)
TaskResponseDto ◄─TaskDtoMapper── Task (domain) ◄─TaskEntityMapper── TaskEntity
```

- `TaskDtoMapper` (`web.mapper`) — граница web ↔ service.
- `TaskEntityMapper` (`persistence.mapper`) — граница service ↔ persistence.
- Контроллер не содержит логики; сервис ничего не знает про DTO и JSON; entity не выходит за пределы persistence.

## Тесты (SIS 2)

| Вид | Класс | Что проверяет | Поднимает Spring? |
|---|---|---|---|
| Unit, сервис | `service.TaskServiceImplTest` | бизнес-логика: дефолты, 404, 409, update/delete; репозиторий — Mockito-мок | нет |
| MockMvc slice, web | `web.controller.TaskControllerTest` (`@WebMvcTest`) | все эндпоинты, коды 200/201/204/400/404/409, `Location`, формат `ApiError`, валидация; сервис — `@MockBean` | только web-слой |
| Интеграционный, репозиторий | `persistence.repository.TaskRepositoryIntegrationTest` (`@DataJpaTest`) | запросы и unique-ограничение на **настоящем PostgreSQL** (локальный сервер, база `sis1_test`) | только JPA |
| Unit, мапперы | `TaskDtoMapperTest`, `TaskEntityMapperTest` | корректность преобразований между слоями | нет |
| Smoke | `Sis1ApplicationTests` | контекст приложения поднимается | да (H2) |

```bash
mvn clean test                       # все тесты + отчёт покрытия
mvn test -Dtest=TaskServiceImplTest  # только unit-тесты сервиса
mvn test -Dtest=TaskControllerTest   # только MockMvc-тесты
mvn test -Dtest=TaskRepositoryIntegrationTest   # только интеграционный (нужен запущенный PostgreSQL)
```

- **PostgreSQL.** `TaskRepositoryIntegrationTest` подключается к локальному PostgreSQL (`localhost:5432`, пользователь/пароль
  `postgres`/`postgres`) и сам создаёт отдельную базу `sis1_test` — данные `sis1db` не затрагиваются. Параметры можно
  переопределить переменными `TEST_DB_HOST`, `TEST_DB_PORT`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`. Если сервер недоступен,
  класс **пропускается** (`skipped`), сборка не падает. Для защиты убедись, что PostgreSQL запущен и тест показывает `Skipped: 0`.
- **Покрытие.** После `mvn test` откройте `target/site/jacoco/index.html`.

## Эндпоинты

| Метод | URL | Успех | Ошибки |
|---|---|---|---|
| POST | `/api/tasks` | `201 Created` (+ `Location`) | `400`, `409` (дубль `title`) |
| GET | `/api/tasks` | `200 OK` (страница) | — |
| GET | `/api/tasks/{id}` | `200 OK` | `404`, `400` (id не число) |
| PUT | `/api/tasks/{id}` | `200 OK` | `404`, `400`, `409` |
| PATCH | `/api/tasks/{id}/status` | `200 OK` | `404`, `400` |
| DELETE | `/api/tasks/{id}` | `204 No Content` | `404` |

`GET /api/tasks` поддерживает `?status=NEW`, `?page=0&size=10&sort=dueDate,asc`.

## Единый формат ошибок

```json
{
  "timestamp": "2026-10-20T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "path": "/api/tasks",
  "errors": [ { "field": "title", "message": "Title length must be between 3 and 100 characters" } ]
}
```

Поле `errors` есть только при ошибках валидации. `404` и `409` — та же форма без `errors`.

## Maven-профили и запуск

```bash
mvn spring-boot:run                   # dev (по умолчанию): H2 in-memory, /h2-console, /swagger-ui/index.html
mvn clean package -P prod             # prod: PostgreSQL-драйвер (см. application-prod.yml)
mvn clean test -P test                # test-профиль
java -jar target/sis1-task-api.jar
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`. HTTP-коллекция: `http-client/tasks.http`.
