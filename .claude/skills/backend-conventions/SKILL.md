---
name: backend-conventions
description: Conventions for the Spring Boot backend of the AFC Gym platform (layered architecture by technical layer, English naming, no comments, no wildcard imports). Use it whenever creating or editing any file in this backend, before writing code.
---

# Backend conventions

This backend follows a classic layered architecture (entity / repository / service / controller, organized by technical layer across the whole app, not one folder per feature), built with Spring Boot 4 / Java 21 / JPA / Flyway. Read this file completely before creating or editing anything in `backend/`.

Context: backend of a gym management platform (AFC Athlete Functional Center). End-user roles: `ADMIN` (reception/management), `SOCIO` (member). The frontend (Expo/React Native) already exists as a UI-only mock with no backend; this API replaces its hardcoded data.

## 1. Language rule

- English for everything that is not shown to the end user: variables, methods, classes, packages, files, DB tables/columns/constraints/indexes, enum constants, JSON fields, URL paths, configuration keys, test names, log messages, Swagger text, commit messages.
- Spanish only for text the end user sees: exception messages returned by the API and catalog data displayed in the UI.
- `README.md` is documentation for the Spanish-speaking team and is written in Spanish. `AGENTS.md` and this skill are in English.
- Enum constants and stored codes are English (`ADMIN`, `ACTIVE`); the frontend translates them for display.

## 2. Hard rules

1. No comments anywhere: no `//`, `/* */`, Javadoc, `<!-- -->`, `#` in YAML, `--` in SQL. Names must make the code self-explanatory.
2. No wildcard imports, including static ones (`import x.*;` is forbidden). One explicit import per type.
3. One public type per file. Indent with 4 spaces.
4. Constructor injection only. Controllers, services and adapters use Lombok `@RequiredArgsConstructor` on `private final` fields. Never write a constructor that only assigns fields. No `@Autowired` in `src/main` (field injection is forbidden; Spring injects a single constructor on its own). `@Autowired` fields are allowed only in tests.
5. Never expose a JPA entity over HTTP. Controllers only ever receive `...Request` records and return `...Response` records.
6. Never log secrets, credentials, tokens or connection strings.
7. No `SELECT *`.
8. Never edit an applied Flyway migration. Add `V<next>__description.sql`.
9. Do not mention anything about reviewers or approvals in code or docs.
10. Never write getters, setters or all-args constructors by hand: use Lombok (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) with explicit imports (`lombok.Getter`, never `lombok.*`).
    - HTTP DTOs (`Response`/`Request`) are `record`s (accessors come free). Add `@Builder` to a record when it has 4 or more fields or when it is built in several places.
    - Entities (`<Name>Entity`, in `persistance/entities`) are never records: `@Entity`, `@Table`, `@Getter`, `@Builder`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`, every field `private`. There is no separate "domain entity"; the JPA entity is the only entity class and is the object services work with directly. An entity that needs to change a field after creation adds `@Setter(AccessLevel.PRIVATE)` on that one field (note: `@Setter`'s access-level parameter is unnamed — `@Setter(AccessLevel.PRIVATE)`, not `@Setter(access = AccessLevel.PRIVATE)`, unlike `@NoArgsConstructor`/`@AllArgsConstructor` which do name it `access`) plus an explicit behaviour method that calls that generated private setter (`deactivate`, `markAsUsed`) instead of a public setter. Read-only fields need no `@Setter` at all. Enums attached to an entity (`Role`, `CheckInMethod`) live in `persistance/entities/enums`.
    - Build objects with `X.builder()...build()` in tests, mappers and response factories instead of long positional constructors.
    - Services are classes with Lombok `@RequiredArgsConstructor`. Write an explicit constructor only when it contains logic.
11. No hand-written query strings anywhere in `src/main` — no `JdbcTemplate`, no `@Query` (neither `nativeQuery = true` nor JPQL text), no `EntityManager.createNativeQuery`. We own the schema and design it to fit the ORM, so there is never a reason to write a query by hand. Every read and write goes through Spring Data JPA using derived query methods (method names Spring parses on its own) for anything they can express. When a query needs optional/dynamic filters that a derived method name cannot express, use `JpaSpecificationExecutor<Entity>` with a `Specification<Entity>` built with the JPA Criteria API (`CriteriaBuilder`/`Root`, composed with `Specification.where(...).and(...)`) — still no query text, fully typed, still Spring Data JPA. `@Query` is reserved for a legacy or externally-owned schema that cannot be reshaped to fit derived methods or Specifications; that is not the case here. Keep each filter predicate in its own `<Entity>Specifications` class under `persistance/repositories/specifications` (see section 4) rather than as private methods on the repository, so filters stay reusable and readable as the project grows.
12. A stateless, reusable helper with no field and no Spring annotation (a random-token generator, a formatter) goes in `utils` as a `final` class with a private constructor and `public static` methods — never duplicated inline inside a service.

## 3. Architecture: layered, organized by technical layer

Decision: the whole application is split by technical layer, not by feature — this mirrors the user's own reference project (`AaaS_API`) folder for folder: `persistance` (entities + repositories), `service` (+ `service/impl`), `web` (`controllers` + `dto`), `configuration` (including `configuration/security`), a top-level `exception` package and `utils`. There is no `domain`/`application`/`infrastructure` split and no per-feature folder.

Conversions between an entity and its HTTP DTOs are done by MapStruct mappers (compile-time generated, no reflection; the reference project maps by hand instead, but this project keeps MapStruct), one per entity, living in `mapper`:

- `<Name>Mapper`: `<Name>Entity` <-> `<Name>Response`, and `<Name>Request` -> `<Name>Entity` or a service command. Mappers are interfaces annotated `@Mapper(componentModel = "spring")`; add `@Mapping` only when field names differ.

Never write mapping code by hand: no `toResponse()`, `toEntity()` or builder chains that copy fields inside entities, services or controllers.

```
com.afcgym.platform
├── AfcGymPlatformApplication.java
├── configuration
│   ├── OpenApiConfiguration.java
│   ├── Bootstrap*.java            startup runners/properties (e.g. the first admin account)
│   └── security
│       ├── Role, SecurityProperties, SecurityConfiguration, RestAuthenticationEntryPoint, RestAccessDeniedHandler, AuthenticatedUser
│       └── jwt
│           └── RevokedTokenValidator    OAuth2TokenValidator<Jwt> used by the JwtDecoder for logout
├── exception                      NotFoundException, BadRequestException, UnauthorizedException, ForbiddenException, ConflictException
├── persistance
│   ├── entities
│   │   ├── <Name>Entity            every JPA entity in the app, flat, no sub-packages
│   │   └── enums                   Role, CheckInMethod and any other entity-attached enum
│   └── repositories
│       ├── I<Name>Repository       Spring Data JPA interface, extends JpaRepository<<Name>Entity, Long>
│       └── specifications          <Name>Specifications (only when a repository needs dynamic/optional-filter queries)
├── service
│   ├── I<Name>Service              interface, one per aggregate, every use case for it as a method
│   ├── dto                         small command/result records a service needs beyond its entity (CreateUserCommand, LoginResult)
│   └── impl
│       └── <Name>ServiceImpl       @Service, @RequiredArgsConstructor, implements I<Name>Service, @Transactional per method that writes
├── utils                           stateless static helpers (SecureTokenGenerator, ...), no Spring annotations
├── mapper                          <Name>Mapper (MapStruct)
└── web
    ├── controllers
    │   ├── <Name>sController        one per aggregate/resource, one method per endpoint
    │   └── advice
    │       ├── ErrorResponse
    │       └── GlobalExceptionHandler
    └── dto
        ├── request                 <Name>Request records (plain, no logic)
        └── response                <Name>Response records (plain, no logic)
```

Dependency rule: `web.controllers -> service -> persistance.repositories -> persistance.entities`. A service never depends on a controller or a DTO from `web.dto`; it takes and returns entities (or a small command/result record from `service.dto` when a request needs more shaping than the entity itself, e.g. `CreateUserCommand`). A repository never contains business logic.

- Services are plain `@Service` classes (not an interface-free class): `I<Name>Service` declares the contract, `<Name>ServiceImpl` implements it. There is one service pair per aggregate, not one class per use case — every operation on that aggregate (create, list, update, a specific business action) is a method on the same interface/impl.
- A repository is a Spring Data JPA interface that extends `JpaRepository<<Name>Entity, Long>` (and `JpaSpecificationExecutor<<Name>Entity>` when it needs dynamic filters). No separate adapter or port: the service talks to the repository interface directly.
- Controllers are thin: they call the service and convert with the entity's `<Name>Mapper`. One controller per resource (Spring idiom), not one per endpoint.
- Transactions (`@Transactional`) are declared on the service implementation's methods that write; read-only methods that only need one repository call do not need it.
- Business validation lives in the service (required fields, normalization, business rules) and throws exceptions from `exception`.
- Routes are declared in the controllers (`@RequestMapping`); there is no separate `routes` folder.

## 4. Naming

| Element | Pattern | Example |
|---|---|---|
| Entity | `<Name>Entity` (in `persistance/entities`) | `MemberEntity` |
| Entity-attached enum | plain enum (in `persistance/entities/enums`) | `Role`, `CheckInMethod` |
| Repository | `I<Name>Repository` (in `persistance/repositories`) | `IMemberRepository` |
| Dynamic query filters | `<Name>Specifications` (in `persistance/repositories/specifications`), `public static` methods, one per filter, each returning `Specification<@NonNull <Name>Entity>`; no declared constructor | `MemberSpecifications.statusEquals(status)` |
| Service interface | `I<Name>Service` (in `service/`) | `IMemberService` |
| Service command/result | plain record (in `service/dto`) | `CreateUserCommand`, `LoginResult` |
| Service implementation | `<Name>ServiceImpl` (in `service/impl`) | `MemberServiceImpl` |
| Utility helper | `final` class, private constructor, `public static` methods (in `utils/`) | `SecureTokenGenerator` |
| Controller | `<NamePlural>Controller`, one per resource (in `web/controllers`) | `MembersController` |
| Mapper | `<Name>Mapper` (in `mapper/`, MapStruct) | `MemberMapper` |
| HTTP DTO | `<Name>Response`, `<Name>Request` (records, in `web/dto/response` or `.../request`) | `MemberResponse` |
| Exception | `<Meaning>Exception` (in top-level `exception/`) | `NotFoundException` |
| Test | `<ClassUnderTest>Test`, methods `given<Condition>_when<Action>_then<Result>` | `LoginUserServiceTest` |
| DB | `snake_case`, singular tables | `member`, `check_in` |
| URL | `/v1/<resource-plural>/...` | `/v1/members/{id}` |

## 5. API

- Version prefix `/v1`. Resource names plural and English.
- Correct status codes: 200, 201 (create), 204, 400 (invalid input), 401, 403, 404, 409. Never answer 500 for validation errors.
- Error body: `{"status": <int>, "message": "<Spanish text>"}` produced by `GlobalExceptionHandler`. Add handlers there; do not add a catch-all `Exception` handler (it would turn Spring's own 404 into 500). Handlers return `ErrorResponse` annotated with `@ResponseStatus`; do not use `ResponseEntity<T>` (Spring 7 null-safety makes IntelliJ warn "Non-null type argument is expected").
- Swagger: `@Tag` per controller and `@Operation(summary = ...)` per method, in English.
- JSON fields are camelCase.

## 6. Persistence

- Schema only through Flyway (`src/main/resources/db/migration`). `ddl-auto` stays `validate`.
- SQL must run on H2 (PostgreSQL mode) and PostgreSQL: `BIGINT GENERATED BY DEFAULT AS IDENTITY`, `VARCHAR`, `TIMESTAMP`, `DATE`, `BOOLEAN`, named `CHECK` constraints. Avoid reserved words (`group`, `order`, `position`, `text`, `number`).
- Enums are stored as `VARCHAR` with a `CHECK` constraint and mapped with `@Enumerated(EnumType.STRING)`.
- N:M and 1:N relations are mapped with `@ManyToMany`/`@OneToMany` (`fetch = FetchType.LAZY`) directly on the `<Name>Entity`. Controllers call the `<Name>Mapper` after the service's transaction has closed (`open-in-view: false`), so every repository method whose result is mapped with a lazy collection must fetch it with `@EntityGraph(attributePaths = "...")` (for example `findWithItemsById`). Endpoint tests run inside `@Transactional` and will not catch a missing graph: verify against the running app.
- Queries come from derived method names or `JpaSpecificationExecutor` + a `<Name>Specifications` class. `@EntityGraph` and `@Lock` are allowed because they carry no query text; `@Query`, native SQL and `JdbcTemplate` are not.
- Stock or balance changes that must be atomic read the row with `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`findWithLockById`). When several rows are locked in one transaction, lock them in ascending id order to avoid deadlocks.
- Human-readable codes built from the id (`SOC-000001`, `V-000001`) are saved first with a random temporary value (`"TMP-" + SecureTokenGenerator.generate(8)`), then assigned through the entity's behaviour method. Never derive the temporary value from the clock: two inserts in the same millisecond collide on the `UNIQUE` constraint.
- Uploaded files go through `IVideoStorageService` (today `LocalVideoStorageServiceImpl`, a folder set by `app.storage.videos-directory`); services never touch the file system directly. Store the new file first and save only its generated name in the entity; old files are removed with `deleteAfterCommit` so a rolled-back transaction never loses a file still referenced, and a stored file is deleted again if its transaction rolls back.
- External HTTP services (today the Expo push API) sit behind a service interface (`IPushDeliveryService` / `ExpoPushDeliveryServiceImpl`) that uses a `RestClient` bean built in `configuration` (timeouts, `BufferingClientHttpRequestFactory` so the body goes with `Content-Length`). The implementation never throws on a network failure: it logs the exception class (never tokens or response bodies) and reports what was accepted. Unit-test it with `MockRestServiceServer.bindTo(RestClient.builder())`.
- Side effects that must happen only after a write is committed (a push notification after a payment is approved) are published as an event (`ApplicationEventPublisher`, record in `service.dto`) and handled with `@TransactionalEventListener` plus `@Transactional(propagation = Propagation.REQUIRES_NEW)`. `@Transactional` endpoint tests never commit, so cover that wiring with a non-transactional `@SpringBootTest` that uses its own H2 database (`@TestPropertySource` datasource URL), `@MockitoBean` for the external service and deletes its rows in `@AfterEach`.
- Scheduled jobs are `@Component`s in `configuration` with `@Scheduled(cron = "${app...}")` that only call a service method taking the date and settings as arguments, so the service stays testable. The test profile disables every cron with `"-"`.
- Dev profile: H2 file database. Test profile: H2 in memory. Prod profile: PostgreSQL through `DB_URL`, `DB_USER`, `DB_PASSWORD`.

## 7. Errors

- Services throw `NotFoundException` (404), `BadRequestException` (400), `UnauthorizedException` (401), `ForbiddenException` (403) or `ConflictException` (409); all have a handler in `GlobalExceptionHandler`. Add a new exception plus its handler for any other status code.
- Role-level access lives in `SecurityConfiguration`. Ownership checks ("only the member who owns this booking") live in the service: the controller turns the JWT into a `service.dto.Requester` (user id + role), so services never depend on `configuration.security`. A `SOCIO` user is linked to its member through `member.user_id` (`IMemberRepository.findByUserId`).
- Messages are Spanish because the user sees them.

## 8. Tests

Style taken from the user's previous projects (Mockito + given/when/then):

- Service tests (the unit tests of the business logic): no Spring. `@ExtendWith(MockitoExtension.class)`, `@InjectMocks` on the `<Name>ServiceImpl`, `@Mock` on every repository/collaborator it uses (`private`).
- Test data ("dummies") is built in `@BeforeEach void init()` calling private `create<Thing>Dummy()` methods that use the entity builder. Keep dummy fields `private`.
- Method names: `given<Condition>_when<Action>_then<Result>` in English (underscores allowed here), package-private, no `test` prefix. Examples: `givenAnUnknownId_whenExecute_thenThrowNotFoundException`.
- Body order: stubbing with `when(...).thenReturn(...)`, call, then `verify(mock, times(1))` / `verify(mock, never())`, then assertions (`assertNotNull`, `assertEquals`, `assertThrows`).
- Cover every service method: the happy path, each validation error (assert the Spanish message) and each exception, and verify that repositories are not called when validation fails (`never()`).
- Mockito runs with strict stubs: do not stub calls the test does not make.
- Endpoint tests: `@SpringBootTest` + `@ActiveProfiles("test")` + `MockMvcBuilders.webAppContextSetup(...)`, hitting the real H2 database through the repository and MapStruct mapper. Repositories and mappers are covered by these tests; do not write mock-only tests for them.
- Proportion: service tests are many and cover every business rule; endpoint tests are few. One happy-path test per endpoint (route, JSON, wiring) is the minimum. The 404/400 JSON mapping of `GlobalExceptionHandler` only needs new endpoint error tests when a feature introduces a new exception handler or status code. Never repeat business rules in endpoint tests.
- Schema and seed data are covered by `DatabaseSchemaTests`; extend it when a migration changes the model.
- Every new service method needs unit tests; every new endpoint needs an endpoint test. All test methods follow the given/when/then naming.

## 9. Security

Where things live:

- `persistance/entities/enums/Role`: enum `ADMIN`, `SOCIO`.
- `configuration/security`: `SecurityProperties`, `SecurityConfiguration` (filter chain, `JwtEncoder`/`JwtDecoder`, `PasswordEncoder`, CORS), `RestAuthenticationEntryPoint` (401 JSON), `RestAccessDeniedHandler` (403 JSON), `AuthenticatedUser` (reads `userId`/`role` off the current `Jwt`) and, in `configuration/security/jwt`, `RevokedTokenValidator` (logout support — see below). Messages are Spanish.
- `configuration/OpenApiConfiguration`: Swagger "Authorize" button with the bearer scheme.

Rules:

- Spring Security validates the JWT (OAuth2 resource server, HS256, secret from `SECRET_KEY`, 1 hour, claims `role`, `userId` and `jti`). Do not write custom authentication filters; a revoked-token check is added as an `OAuth2TokenValidator<Jwt>` passed to the `JwtDecoder`, which is the supported extension point, never a custom `OncePerRequestFilter`.
- Roles are the `Role` enum. Authorization is declared in one place, the `authorizeHttpRequests` block of `SecurityConfiguration`, with `Role.X.name()` (never loose strings). The default is `anyRequest().authenticated()`: a new endpoint is protected for every role until a rule narrows it.
- When adding an endpoint, decide its roles explicitly in `SecurityConfiguration` (`hasRole`/`hasAnyRole`) and add the endpoint test for 401 (no token) and 403 (wrong role) when the endpoint restricts roles.
- Public routes are only `POST /v1/auth/login`, `POST /v1/auth/password-resets`, `POST /v1/auth/password-resets/confirm`, `/actuator/health`, Swagger (`/swagger-ui.html`, `/v3/api-docs`) and `/h2-console` (disabled outside dev). In `prod`, Swagger and API docs are disabled.
- Logout with a stateless JWT: `POST /v1/auth/logout` reads the `jti` claim of the caller's own token and stores it (with its expiry) through `IJwtBlocklistRepository` (`persistance.repositories`); `RevokedTokenValidator` (`configuration.security.jwt`) rejects any token whose `jti` is blocklisted. A background cleanup is not required at this size; expired rows can be pruned later if the table grows.
- Passwords: BCrypt behind `PasswordEncoder` injected straight into the service that needs it (no extra hashing port/adapter indirection at this architecture). Strength 12 (4 in tests). Responses never contain password hashes. Login answers a generic 401 "Usuario o contraseña incorrectos" for unknown user, wrong password or inactive user.
- Configuration: `app.security.*` and `app.bootstrap-admin.*` in YAML. `SECRET_KEY` (at least 32 bytes) is mandatory in `prod`; dev and test have their own fixed values. The first `ADMIN` user is created at startup only if there are no users and `BOOTSTRAP_ADMIN_PASSWORD` is set (dev default `Admin12345!`, development only). `ALLOWED_ORIGINS` configures CORS.
- Endpoint tests use `MockMvcBuilders.webAppContextSetup(...).apply(springSecurity())` and `.with(asRole(Role.X))` from `TestAuthentication`. `AuthControllerTest` and `SecurityConfigurationTest` use real tokens.
- Never log tokens, passwords, hashes or the JWT secret.

## 10. Creating a new feature

1. Add the migration if the schema changes (`V<next>__...sql`, no comments).
2. `persistance/entities`: the `<Name>Entity` (Lombok `@Entity @Getter @Builder` etc., enums in `persistance/entities/enums`), then its `I<Name>Repository` in `persistance/repositories`.
3. `service`: `I<Name>Service` with every operation as a method, any supporting command/result record in `service/dto`, `<Name>ServiceImpl` in `service/impl` with validation and the top-level `exception` package.
4. `mapper`: `<Name>Mapper` (MapStruct) for entity <-> `Request`/`Response`.
5. `web/controllers`: one `<NamePlural>Controller` with one method per endpoint and the plain `Response`/`Request` records in `web/dto`.
6. Service unit tests (Mockito, given/when/then) and endpoint tests; extend `DatabaseSchemaTests` if tables changed.
7. Run the self-check below.

## 11. Self-check before finishing

Run from `backend/` and fix every hit, every command must print nothing:

```bash
grep -rnE "^\s*(//|/\*|\*|<!--)" src pom.xml
grep -rn "import .*\*;" src
grep -rnE "^\s*(#|--)" src/main/resources src/test/resources
grep -rnE "JdbcTemplate|nativeQuery|createNativeQuery|@Query" src/main/java
grep -rnE "@Autowired" src/main/java
```

Also verify that no identifier, table or column name is in Spanish, that no entity is returned by a controller, and that `./mvnw test` passes.

## 12. Environment

- The user runs everything from IntelliJ IDEA (no terminal commands for them); give IntelliJ instructions.
- JDK 21. Local port 8091 (8090 is used by the Ámbar Rojo backend on this machine; 8080, 8081 and 8087 are taken by WSL); hosting injects `PORT`.
- If Maven fails with `PKIX path building failed`, set the Maven importer and runner VM options to `-Djavax.net.ssl.trustStoreType=Windows-ROOT` (IntelliJ: Settings > Build Tools > Maven). For terminal runs: `MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"`. Never commit it to `.mvn/jvm.config`.
- MapStruct 1.6.3 and Lombok work together only with `lombok-mapstruct-binding` in `annotationProcessorPaths`, in this order: `lombok`, `lombok-mapstruct-binding`, `mapstruct-processor` (both `default-compile` and `default-testCompile`). Do not change the order. In IntelliJ, after editing the pom, reload Maven and rebuild; annotation processing must be enabled.
- The Maven wrapper is pinned to 3.9.11. Do not upgrade it to 3.10+ or 4.
- When smoke testing the packaged jar, the `Started AfcGymPlatformApplication` log line is printed before the `ApplicationRunner` that creates the bootstrap admin finishes: wait until `POST /v1/auth/login` succeeds, not until that line appears.
- When IntelliJ has not imported the Maven project yet, "Optimize Imports" or reformat actions can delete valid imports (classes look unresolved). After any IntelliJ session, compile before assuming the sources are intact.
- IntelliJ's "Non-null type argument is expected" warning (Spring 7's move to JSpecify null-safety) shows on every type argument passed to a Spring Data repository interface, e.g. `extends JpaRepository<MemberEntity, Long>`, because those interfaces declare their type parameters with a `@NonNull` bound. Fix it with code, not suppression: add the JSpecify type-use annotation directly on each type argument, e.g. `extends JpaRepository<@NonNull MemberEntity, @NonNull Long>` (import `org.jspecify.annotations.NonNull`; the artifact is already on the classpath transitively via Spring, no pom.xml change needed).

## 13. Git workflow

- `develop` is the real integration branch (not `main`, which lags behind and is kept for releases). One feature branch per HU/task, cut from `develop`, named `Feature/<kebab-case-english-name>` (capital `F`, English words even though the HU itself is in Spanish) — e.g. `Feature/member-check-in`, `Feature/password-reset`. Commit and push that branch, then open the PR against `develop` on GitHub; do not commit feature work straight to `develop` or `main`.
- Independent HUs/tasks each get their own `Feature/...` branch cut from `develop` (not from another unmerged feature branch), so their PRs don't depend on each other landing in order.
- Before cutting a new branch, `git fetch origin develop` and branch from `origin/develop` so it starts from the latest merged work; a branch cut from a stale local `develop`/`main` can pick up merge conflicts later that an up-to-date branch would have avoided.
