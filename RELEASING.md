# Release process

This lab prepares releases on `dev` and merges to `main` via PR. Versions follow semantic versioning.

**Version sources (must match):**

- Root `build.gradle.kts` → `version`
- `CHANGELOG.md` latest released section (e.g. `## [0.0.3]`)

Tags use the bare version (e.g. `0.0.3`) with **no** `v` prefix.

There may also be a long-lived `release` branch on the remote; **this flow tags from `main`**. Do not invent a parallel process without updating this file.

## Branch roles

| Branch | Role |
|--------|------|
| `dev` | Day-to-day lab work |
| `main` | Stable line; tags and GitHub Releases land here |

After any PR merges into `main`, sync `dev` so it is **not behind** `main`. Default: **merge `main` → `dev`**. Do not force-push `main` over remote `dev` by default.

Post-release work on `dev` goes into **Unreleased** / the next changelog section, not into an already tagged version.

## Steps

1. Finish work on `dev`. Prefer a green Gradle smoke/test pass for touched modules.
2. Bump `version` in root `build.gradle.kts`. Update `CHANGELOG.md` (move items out of Unreleased as needed).
3. Self-check: no secrets; no accidental local/DB credentials in resources.
4. Push `dev` and open a PR to `main`.
5. Merge the PR.
6. Tag and release from merged `main`:

   ```bash
   git checkout main
   git pull origin main
   # tip is this merge; build.gradle.kts version and CHANGELOG agree
   git tag <version>          # e.g. 0.0.3, no v prefix
   git push origin <version>
   gh release create <version> --generate-notes --title "<version> short title"
   # or --notes-file / GitHub UI; no drafts
   ```

7. **Merge `main` back into `dev`** (after release PRs and other `dev → main` PRs):

   ```bash
   git checkout dev
   git pull origin dev
   git merge origin/main
   git push origin dev
   ```

   Confirm **behind is 0** vs `origin/main` (ahead OK).

8. (Optional) Rebuild local `dev` only:

   ```bash
   git checkout main
   git branch -D dev
   git checkout -b dev origin/dev
   ```

## Checklist

- [ ] `build.gradle.kts` version and `CHANGELOG.md` match the tag
- [ ] Tag / Release match; no `v` prefix; Release not a draft
- [ ] `main` and `dev` pushed; `dev` not behind `main`
- [ ] No secrets or local credentials committed
