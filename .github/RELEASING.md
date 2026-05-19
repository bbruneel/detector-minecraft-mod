# Creating a release

Releases are built and published automatically when you push a version tag.

## Prerequisites

- `mod_version` in `gradle.properties` matches the version you are releasing (without the `v` prefix).
- `main` is green in CI (`.github/workflows/build.yml`).

## Steps

1. Update `mod_version` in `gradle.properties` if this is a new version.
2. Commit the version bump (and any other release changes) to `main`.
3. Create and push an annotated tag whose name is `v` plus the same version:

   ```bash
   git tag -a v1.0.0 -m "Detector 1.0.0"
   git push origin v1.0.0
   ```

4. GitHub Actions runs [release.yml](workflows/release.yml), which:
   - runs `./gradlew check remapJar`
   - verifies the tag matches `mod_version`
   - creates a GitHub Release and uploads `detector-<version>.jar`

5. Confirm the release on the [Releases](https://github.com/bbruneel/detector-minecraft-mod/releases) page.

## Tag rules

- Tags must look like `v1.0.0`, `v1.2.3`, etc. (leading `v` is required).
- The part after `v` must be semver-shaped (`major.minor.patch`, optional `-prerelease` suffix).
- That version must equal `mod_version` in `gradle.properties`.
- Tag the commit on `main` that contains the version bump; the workflow checks out the tagged commit.

## Re-running a failed release

Delete the broken release and tag in GitHub (if created), fix the issue, delete the local/remote tag if needed, then push the tag again:

```bash
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0
git tag -a v1.0.0 -m "Detector 1.0.0"
git push origin v1.0.0
```
