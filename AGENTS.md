# Backend: AFC Gym platform

Spring Boot 4 / Java 21 / JPA / Flyway. Layered architecture organized by technical layer (not by feature), mirroring the folder-for-folder structure of the user's own reference project (`AaaS_API`): `persistance` (`entities` + `repositories`), `service` (+ `service.impl`), `web` (`controllers` + `dto`), `configuration` (incl. `configuration.security`), a top-level `exception` package and `utils`.

Before creating or editing any file here, read `.claude/skills/backend-conventions/SKILL.md` and follow it.

Non-negotiable rules:

- Everything in English (identifiers, packages, DB names, JSON fields, URLs, tests, logs). Spanish only for text the end user sees (API error messages, catalog data).
- No comments of any kind (Java, SQL, YAML, XML).
- No wildcard imports.
- Package layout: `persistance.entities` (+ `persistance.entities.enums`), `persistance.repositories`, `service` (+ `service.dto`, `service.impl`), `mapper`, `web.controllers` (+ `web.controllers.advice`), `web.dto.request`/`web.dto.response`, `configuration` (+ `configuration.security`, `configuration.security.jwt`), top-level `exception`, and `utils` for stateless helpers. One entity class per table (no separate domain/JPA split). One `I<Name>Service` interface + `<Name>ServiceImpl` per aggregate, holding every use case for that aggregate as a method. MapStruct mappers for every entity<->DTO conversion (never hand-written mapping). Constructor injection only (Lombok `@RequiredArgsConstructor`), never field `@Autowired` in `src/main`.
- The user runs the project from IntelliJ IDEA: give IntelliJ instructions, not terminal commands.
