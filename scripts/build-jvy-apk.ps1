param(
    [ValidateSet("demo", "debug", "release")]
    [string]$Variant = "demo",

    [ValidateSet("patch", "minor", "major")]
    [string]$Bump = "patch"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $repoRoot

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
$gradle = Join-Path $repoRoot "gradlew.bat"

Write-Host "Building Jvy Wallet $nextVersion (versionCode $nextCode, $Variant)..."
$gradleArguments = @(
    $gradleTask,
    "-PjvyVersionName=$nextVersion",
    "-PjvyVersionCode=$nextCode",
    "--no-daemon",
    "--no-configuration-cache",
    "--max-workers=2",
    "--console=plain"
)
if ($Variant -eq "demo") {
    $gradleArguments += @("-x", ":app:uploadCrashlyticsMappingFileDemo")
}
& $gradle @gradleArguments

if ($LASTEXITCODE -ne 0) {
    throw "APK build failed. Version $nextVersion was not allocated or persisted."
}

$propertiesText = @(
    "# Authoritative Jvy Wallet APK release baseline."
    "# Updated only after a successful official APK build."
    "versionName=$nextVersion"
    "versionCode=$nextCode"
)
Set-Content -LiteralPath $versionFile -Value $propertiesText -Encoding utf8

$apkSource = Join-Path $repoRoot "app\build\outputs\apk\$Variant\app-$Variant.apk"
if (-not (Test-Path -LiteralPath $apkSource)) {
    throw "Gradle succeeded but the expected APK was not found: $apkSource"
}

$artifactDirectory = Join-Path $repoRoot "artifacts\apk"
New-Item -ItemType Directory -Path $artifactDirectory -Force | Out-Null
$artifactName = "Jvy_wallet_v$nextVersion.apk"
$artifactPath = Join-Path $artifactDirectory $artifactName
Copy-Item -LiteralPath $apkSource -Destination $artifactPath -Force

$sha256 = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
$manifest = [ordered]@{
    product = "Jvy Wallet"
    version = $nextVersion
    versionCode = $nextCode
    variant = $Variant
    artifact = $artifactName
    sha256 = $sha256
    builtAt = (Get-Date).ToUniversalTime().ToString("o")
}
$manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $artifactDirectory "Jvy_wallet_v$nextVersion.json") -Encoding utf8

Write-Host "Created: $artifactPath"
Write-Host "SHA-256: $sha256"
Write-Host "Commit version.properties with the source changes before distributing the APK."
