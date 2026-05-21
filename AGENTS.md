# Agent Instructions

## Project overview
- Spring Boot backend under package `ptit.tmdt.lop6nhom7.baodientu`.
- Typical layers: controller, service, repository, dto, entity, security.

## Build and test
- Use Maven wrapper; Java version is 25.
- CI compile: `./mvnw -B -ntp clean compile` (Windows: `mvnw.cmd -B -ntp clean compile`).
- Tests (if needed): `./mvnw -B -ntp test`.

## Conventions and references
- App config: `src/main/resources/application.yaml`.
- Do not edit generated artifacts under `target/`.
- Team workflow rules: [README](README.md).
- CI workflow: [.github/workflows/ci.yml](.github/workflows/ci.yml).
