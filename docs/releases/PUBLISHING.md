# Publishing the v0.7.0 prerelease

The owner authorized commit, push and release on 8 November 2026. The README
must remain untouched. Publish the existing verified binary; do not rebuild it
or allocate another version merely to upload assets.

## Recorded remote state

The release preparation was committed as
`8ad55eb94e24b42139a746fabed9db6ae3181251` and pushed successfully to
`joravar/wave-0-control-plane`. Annotated tag `v0.7.0` was pushed atomically
and resolves to that same commit. Remote refs were read back to verify both.
`main` remains at `710733dbe4729a437d573cdfb8429c50e617ded1`.
The README matches the previously built source commit exactly.

**GitHub release publication/assets are not completed or verified.** REST and
GraphQL requests fail at the environment proxy with HTTP 403 before GitHub
authentication. The existing APK/checksum are intact in `artifacts/apk/`;
allowing the API and upload hosts is the next required external action.

## Access and identity

- Repository: `joravarsinghing/ivy-wallet`.
- Project branch: `joravar/wave-0-control-plane`; do not overwrite `main`.
- Commit/tag identity: `Joravar Singh <joravarsinghing@users.noreply.github.com>`.
- GitHub publishing must use the personal `joravarsinghing` identity with release
  write permission. Configure credentials securely, never in source or chat.
- This cloud environment currently permits Git transport to `github.com`, but
  denies CONNECT requests to `api.github.com` and `uploads.github.com`. Allow
  those two hosts through the environment/settings review flow before retrying.
  Do not bypass the outbound policy or export credentials to another host.

## Prepared files

- [Changelog](../../CHANGELOG.md): accumulated build milestones since v0.2.15.
- [Release body](0.7.0.md): highlights, compatible update paths and limitations.
- [Public artifact metadata](0.7.0-manifest.json): exact APK hash, size and source.
- [Build evidence](0.7.0-candidate.md): signing and test verification.
- Fastlane changelog `232.txt` and the Jvy-specific fallback text are updated;
  preparing these files does not publish to Google Play.

## Publish once access is ready

Confirm `gh api user --jq .login` returns `joravarsinghing`. Confirm the tag
`v0.7.0` is present on the remote and points to the release-preparation commit.
Do not recreate, move or force-push an existing published tag.

From the repository root, first check the existing APK:

```sh
(cd artifacts/apk && sha256sum -c Jvy_wallet_v0.7.0.sha256)
gh release view v0.7.0 --repo joravarsinghing/ivy-wallet
```

If a release already exists, inspect its tag, state and assets before deciding
whether anything is missing. Do not replace an existing APK with a different
binary. If no release exists, create a **draft prerelease**, upload the complete
bundle, and only then publish:

```sh
gh release create v0.7.0 \
  --repo joravarsinghing/ivy-wallet \
  --verify-tag --draft --prerelease \
  --title 'Jvy Wallet v0.7.0 — a clearer, more visual wallet' \
  --notes-file docs/releases/0.7.0.md \
  artifacts/apk/Jvy_wallet_v0.7.0.apk \
  artifacts/apk/Jvy_wallet_v0.7.0.zip \
  artifacts/apk/Jvy_wallet_v0.7.0.sha256 \
  artifacts/apk/Jvy_wallet_v0.7.0.json \
  artifacts/apk/Jvy_wallet_v0.7.0.verification.json

gh release view v0.7.0 --repo joravarsinghing/ivy-wallet --json assets,isDraft,isPrerelease,url
gh release edit v0.7.0 --repo joravarsinghing/ivy-wallet --draft=false --prerelease
```

Verify all five assets before publishing. Download the published APK into a
fresh temporary directory and compare its SHA-256 with the committed public
manifest. Confirm the release remains a prerelease, record its URL and actual
publication result in the execution ledger, and leave device/cohort acceptance
pending. Do not claim stable certification or update the README.

Only APK-related public artifacts belong in release assets. Never upload
keystores, signing variables, wallet backups, private screenshots or broad
verification/security-review directories. APKs and generated manifests remain
outside Git, in the ignored `artifacts/apk/` directory.
