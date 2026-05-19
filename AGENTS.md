# AGENTS.md

## Purpose

This file guides coding agents working on this repository.  
The project is a Fabric mod targeting Minecraft `1.21.x` and should use Java `21`.

Primary goals:

- Keep changes safe, small, and maintainable.
- Preserve a low dependency footprint.
- Prefer readable logging and strong troubleshooting signals.
- Keep security in scope for all features.
- Maintain test coverage and green CI on GitHub Actions.

## Canonical Tech Constraints

- **Minecraft target:** `1.21.x`
- **Java target/runtime:** `21` (source, target, toolchain, and CI)
- **Mod loader stack:** Fabric Loader + Fabric API + Yarn mappings
- **Build tool:** Gradle + Fabric Loom
- **Mixin framework:** Sponge Mixin

If any file conflicts with these constraints (for example Java 25 settings), treat that as technical drift and align to Java 21 unless the user explicitly asks otherwise.

## Current Project Layout

- `src/main/java/org/bruneel/detector/DetectorMod.java` - main Fabric mod initializer.
- `src/client/java/org/bruneel/detector/client/DetectorModClient.java` - client-only initializer.
- `src/client/java/org/bruneel/detector/client/command/DetectorClientCommands.java` - client command registration (`/detector ...`).
- `src/client/java/org/bruneel/detector/client/detect/` - detection models, scanners, config persistence, and engine.
- `src/main/java/org/bruneel/detector/mixin/ServerLifecycleMixin.java` - server mixin.
- `src/client/java/org/bruneel/detector/client/mixin/MinecraftClientMixin.java` - client mixin.
- `src/test/java/org/bruneel/detector/client/detect/` - unit tests for detection state/registry/parsing.
- `src/main/resources/fabric.mod.json` - mod metadata and entrypoints.
- `src/main/resources/detector.mixins.json` - common mixin config.
- `src/client/resources/detector.client.mixins.json` - client mixin config.
- `.github/workflows/build.yml` - CI build pipeline.

Note: keep naming aligned to `detector` and `org.bruneel.detector` for future additions.

## Agent Workflow

1. Read relevant files first; avoid broad, noisy edits.
2. Make minimal, focused changes that satisfy the request.
3. Prefer explicit failure paths and clear log messages.
4. Run tests/lint/build after substantive edits.
5. Report what changed, why, and how it was validated.

## Coding Standards

- Use Java 21 language features only when they improve readability.
- Prefer straightforward, explicit logic over clever abstractions.
- Keep methods short and side effects obvious.
- Use `final` where it improves clarity.
- Avoid global mutable state unless lifecycle constraints require it.
- Keep package boundaries clear (`main` vs `client`).

## Logging Guidelines

- Use the mod logger (`LoggerFactory.getLogger(MOD_ID)`).
- Log context-rich messages: what happened, where, and important IDs/state.
- Use levels consistently:
  - `info`: lifecycle milestones and expected major events.
  - `warn`: recoverable but unexpected states.
  - `error`: failures requiring attention.
- Never log secrets, tokens, session material, or personal data.
- Prefer structured placeholders (`LOGGER.info("Loaded feature {} for {}", feature, playerId)`).

## Security Requirements

- Validate all external input (network payloads, commands, config values).
- Fail closed on malformed or untrusted data.
- Avoid dynamic classloading/reflection from untrusted sources.
- Keep network surface minimal and explicit.
- Do not introduce telemetry or outbound calls without clear user-facing rationale.
- If adding crypto/hash or secure random behavior, use JDK primitives first.
- Track third-party dependencies and update for security advisories.

## Dependency Policy (Low Footprint First)

Default approach:

1. **JDK/Fabric first** (no new dependency).
2. Add a small library only when there is a clear benefit.
3. Prefer lower-footprint libraries (example: OkHttp over Apache HttpClient when both satisfy requirements).

Rules:

- If functionality is available via `java.`* / `java.net.http`, prefer that.
- If adding non-Fabric libraries that ship in the mod jar, **shade + relocate** them.
- Avoid pulling large transitive dependency trees for simple tasks.
- Keep dependency count and jar size in PR notes when dependencies change.

### Shading Guidance (Gradle)

When a third-party runtime dependency is required:

- Use the Shadow plugin and relocate packages to avoid classpath conflicts.
- Ensure remapping still works with Loom (`remapJar` should consume the shaded jar artifact).
- Verify no duplicate/invalid classes in final output.

If shading creates complexity, propose and evaluate a no-dependency or JDK-native alternative before merging.

## Mixins and Compatibility

- Keep mixins narrowly targeted and justified.
- Prefer `@Inject` at stable points over heavy overwrites.
- Document why each injection point is safe.
- Include guardrails for side-specific logic (`client` vs dedicated server).
- Re-test mixins on both client and dedicated server paths when relevant.

## Testing Expectations

Minimum for behavior changes:

- Add or update unit tests for pure logic.
- Run `./gradlew test` locally.
- Keep command input parsing and detector state transitions in pure classes so they can be tested without a running client.

For Minecraft-integrated behavior:

- Use GameTest or integration-style validation when unit tests cannot cover behavior.
- At minimum, provide reproducible manual verification steps in PR notes.

Bug fix quality bar:

- Add a regression test when feasible.
- If not feasible, explain why and define manual validation clearly.

## CI/CD Expectations

- CI runs through GitHub Actions (`.github/workflows/build.yml`).
- Ensure CI Java version aligns with project target (Java 21 for this repo).
- Keep pipeline fast and deterministic:
  - CI pipeline:
    - `./gradlew check`
    - `./gradlew remapJar`
  - Local one-command equivalent: `./gradlew build`
  - tests enabled
  - artifact upload only when useful

Any workflow update should preserve branch/PR safety checks and avoid unnecessary complexity.

## Definition of Done (for Agents)

A change is complete when:

- Code compiles and formatting is clean.
- Tests are added/updated and pass (or limitation is clearly documented).
- Logging is useful and not noisy.
- Security implications were considered.
- Dependency impact is minimal and justified.
- CI remains green or required changes are included.
- User-facing behavior changes are documented in the response.

## Useful References

- [Fabric docs hub](https://docs.fabricmc.net/)
- [Fabric setup guide](https://docs.fabricmc.net/develop/getting-started/setting-up)
- [Fabric project creation details](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up)
- [Fabric wiki tutorials](https://wiki.fabricmc.net/)

## Cursor Cloud specific instructions

This is a pure Java/Gradle project with no external services (no databases, Docker, or web APIs).

**Environment:** Java 21 (OpenJDK) is pre-installed. Gradle 9.4.1 is fetched automatically by the wrapper on first run. The first `./gradlew` invocation downloads Minecraft jars, Yarn mappings, and Fabric API (~1-2 min on a cold cache); subsequent runs are fast.

**Key commands** (all documented in README and CI workflow):
- `./gradlew test` — run 24 unit tests (JUnit 5)
- `./gradlew check` — tests + verification tasks (mirrors CI `check` job)
- `./gradlew build` — full build: compile + test + remapJar + assemble
- `./gradlew runClient` — launch Minecraft client with the mod (requires a GUI/display; not available in headless Cloud VMs)

**Gotchas:**
- The Fabric Loom plugin uses a shared file lock (`~/.gradle/caches/fabric-loom`). If two Gradle daemons run concurrently (e.g. from parallel tmux sessions), one will block waiting for the lock. Avoid running multiple Gradle commands in parallel.
- `BlockScanner.java` emits a deprecation warning during compilation (`uses or overrides a deprecated API`). This is a known upstream Fabric API issue and is not a build failure.
- `./gradlew runClient` cannot be used in headless Cloud VM environments since it requires a Minecraft client with a display. Unit tests (`./gradlew test`) are the primary validation path for Cloud agents.
- There is no separate lint tool; `./gradlew check` is the equivalent.