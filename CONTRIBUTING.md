# Contributing to TaskMaster

Thank you for your interest in contributing to TaskMaster! This document outlines our development workflows, coding standards, and branch policies.

---

## 1. Branch Strategy

We follow a trunk-based development workflow with short-lived feature branches:

- **`main`**: Production-ready code. Direct commits to `main` are restricted.
- **Feature Branches**: `feat/phase-<phase-number>-<feature-description>` (e.g. `feat/phase-1-foundation`)
- **Bug Fix Branches**: `fix/<issue-description>`
- **Documentation Branches**: `docs/<topic>`

### Workflow
1. Branch off the latest `main`.
2. Implement your changes following code quality and architectural conventions.
3. Ensure all tests and style checks pass (`./gradlew check`).
4. Submit a Pull Request targeting `main`.

---

## 2. Commit Message Conventions

We adhere to the [Conventional Commits](https://www.conventionalcommits.org/) standard:

```
<type>(<scope>): <short summary>

[optional body]

[optional footer(s)]
```

### Allowed Types
- **`feat`**: A new feature
- **`fix`**: A bug fix
- **`docs`**: Documentation only changes
- **`style`**: Changes that do not affect the meaning of the code (white-space, formatting)
- **`refactor`**: Code changes that neither fix a bug nor add a feature
- **`perf`**: Performance improvements
- **`test`**: Adding missing tests or correcting existing tests
- **`chore`**: Changes to the build process, dependency updates, or auxiliary tools

---

## 3. Code Standards & Architecture

1. **Hexagonal Architecture**: Keep business logic domain models independent of framework/adapter dependencies.
2. **ArchUnit Compliance**: ArchUnit tests run as part of the build to enforce architectural layer boundaries.
3. **Google Java Format & Checkstyle**: Keep code style clean and properly formatted.
4. **Error Handling**: Use the standardized `GlobalExceptionHandler` returning RFC 7807 `ProblemDetail` responses.
5. **Database Migrations**: Always use Flyway versioned migrations (`V<version>__<description>.sql`). Never alter existing applied migrations.

---

## 4. Submitting a Pull Request

1. Ensure the PR title clearly states the intent and follows conventional commits.
2. Link any related issues or phase milestones.
3. Update `CHANGELOG.md` under `[Unreleased]`.
4. Verify all tests pass locally:
   ```bash
   ./gradlew test
   ./gradlew checkstyleMain checkstyleTest
   ```
