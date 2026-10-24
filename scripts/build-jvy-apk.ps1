param(
    [ValidateSet("demo", "release")]
    [string]$Variant = "demo",

    [ValidateSet("patch", "minor", "major")]
    [string]$Bump = "patch",

    # Only for explicitly accepted, legacy-compatible local testing. Never a public release.
    [switch]$AllowPublicTestKey
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $repoRoot

if ($Variant -eq "demo" -and -not $AllowPublicTestKey) {
    throw "Demo APKs use a publicly available test signing key. For an explicitly accepted legacy test update, pass -AllowPublicTestKey. Public distribution requires -Variant release and your private keystore. No APK or version was generated."
}
if ($Variant -eq "release") {
    foreach ($name in @("SIGNING_STORE_PASSWORD", "SIGNING_KEY_ALIAS", "SIGNING_KEY_PASSWORD")) {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
            throw "Private release signing requires $name. Configure it securely; do not paste passwords into chat. No APK or version was generated."
        }
    }
    $privateStore = if ($env:SIGNING_STORE_FILE) { $env:SIGNING_STORE_FILE } else { Join-Path $repoRoot "sign.jks" }
    if (-not (Test-Path -LiteralPath $privateStore -PathType Leaf)) {
        throw "Private keystore not found. Set SIGNING_STORE_FILE. No APK or version was generated."
    }
}

# Use the same authoritative workflow on Windows and cloud/Linux hosts.
# A process-level file lock prevents two builds allocating the same version.
$lockDirectory = Join-Path $repoRoot ".gradle"
New-Item -ItemType Directory -Path $lockDirectory -Force | Out-Null
$buildLock = [System.IO.File]::Open(
    (Join-Path $lockDirectory "jvy-apk-build.lock"),
    [System.IO.FileMode]::OpenOrCreate,
    [System.IO.FileAccess]::ReadWrite,
    [System.IO.FileShare]::None
)
try {

$versionFile = Join-Path $repoRoot "version.properties"
if (-not (Test-Path -LiteralPath $versionFile)) {
    throw "Missing authoritative version file: $versionFile"
}

$properties = [System.Collections.Specialized.OrderedDictionary]::new()
Get-Content -LiteralPath $versionFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $parts = $line.Split("=", 2)
        $properties[$parts[0].Trim()] = $parts[1].Trim()
    }
}

$currentVersion = $properties["versionName"]
$currentCode = [int]$properties["versionCode"]
if ($currentVersion -notmatch '^(\d+)\.(\d+)\.(\d+)$') {
    throw "versionName must use major.minor.patch format, found: $currentVersion"
}

$major = [int]$Matches[1]
$minor = [int]$Matches[2]
$patch = [int]$Matches[3]
switch ($Bump) {
    "major" { $major++; $minor = 0; $patch = 0 }
    "minor" { $minor++; $patch = 0 }
    "patch" { $patch++ }
}

$nextVersion = "$major.$minor.$patch"
$nextCode = $currentCode + 1
$variantTitle = $Variant.Substring(0, 1).ToUpperInvariant() + $Variant.Substring(1)
$gradleTask = ":app:assemble$variantTitle"
$gradle = if ($env:OS -eq "Windows_NT") {
    Join-Path $repoRoot "gradlew.bat"
} else {
    Join-Path $repoRoot "gradlew"
}

$artifactDirectory = Join-Path $repoRoot "artifacts/apk"
$artifactName = "Jvy_wallet_v$nextVersion.apk"
$artifactPath = Join-Path $artifactDirectory $artifactName
$manifestPath = Join-Path $artifactDirectory "Jvy_wallet_v$nextVersion.json"
if ((Test-Path -LiteralPath $artifactPath) -or (Test-Path -LiteralPath $manifestPath)) {
    throw "Version $nextVersion already has an artifact. Refusing to overwrite it."
}

Write-Host "Building Jvy Wallet $nextVersion (versionCode $nextCode, $Variant)..."
$gradleArguments = @(
    $gradleTask,
    "-PjvyVersionName=$nextVersion",
    "-PjvyVersionCode=$nextCode",
    "--no-daemon",
    "--no-configuration-cache",
    "--max-workers=2",
    "--console=plain"
    "-Pkotlin.compiler.execution.strategy=in-process"
)
# A local numbered APK build must not publish mapping files to Firebase.
$gradleArguments += @("-x", ":app:uploadCrashlyticsMappingFile$variantTitle")
& $gradle @gradleArguments

if ($LASTEXITCODE -ne 0) {
    throw "APK build failed. Version $nextVersion was not allocated or persisted."
}

$apkSource = Join-Path $repoRoot "app/build/outputs/apk/$Variant/app-$Variant.apk"
if (-not (Test-Path -LiteralPath $apkSource)) {
    throw "Gradle succeeded but the expected APK was not found: $apkSource"
}
if ((Get-Item -LiteralPath $apkSource).Length -eq 0) {
    throw "Gradle produced an empty APK. Version was not persisted."
}

$propertiesText = @(
    "# Authoritative Jvy Wallet APK release baseline."
    "# Updated only after a successful official APK build."
    "versionName=$nextVersion"
    "versionCode=$nextCode"
)
New-Item -ItemType Directory -Path $artifactDirectory -Force | Out-Null
Copy-Item -LiteralPath $apkSource -Destination $artifactPath

$sha256 = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
$manifest = [ordered]@{
    product = "Jvy Wallet"
    version = $nextVersion
    versionCode = $nextCode
    variant = $Variant
    signingKeyType = $(if ($Variant -eq "demo") { "legacy-public-test-only" } else { "private-release" })
    artifact = $artifactName
    sha256 = $sha256
    builtAt = (Get-Date).ToUniversalTime().ToString("o")
}
$manifest | ConvertTo-Json | Set-Content -LiteralPath $manifestPath -Encoding utf8
# Persist only after both deliverables exist and the checksum has been calculated.
Set-Content -LiteralPath $versionFile -Value $propertiesText -Encoding utf8

Write-Host "Created: $artifactPath"
Write-Host "SHA-256: $sha256"
Write-Host "Commit version.properties with the source changes before distributing the APK."
} finally {
    $buildLock.Dispose()
}
