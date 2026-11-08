# Isolated contract tests: the fake Gradle launcher never packages an Android app.
$ErrorActionPreference = "Stop"
$fixtureRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("jvy-build-contract-" + [guid]::NewGuid())
$fixtureScripts = Join-Path $fixtureRoot "scripts"
New-Item -ItemType Directory -Path $fixtureScripts -Force | Out-Null
$fixtureScript = Join-Path $fixtureScripts "build-jvy-apk.ps1"
Copy-Item -LiteralPath (Join-Path $PSScriptRoot "build-jvy-apk.ps1") -Destination $fixtureScript
$versionFile = Join-Path $fixtureRoot "version.properties"
$artifactDirectory = Join-Path $fixtureRoot "artifacts/apk"
$pwsh = (Get-Process -Id $PID).Path
$onWindows = $env:OS -eq "Windows_NT"
$launcher = Join-Path $fixtureRoot $(if ($onWindows) { "gradlew.bat" } else { "gradlew" })
$fakeGradle = if ($onWindows) {
    "@echo off`r`nexit /b %JVY_CONTRACT_EXIT%`r`n"
} else {
    '#!/bin/sh' + "`n" + 'exit "$JVY_CONTRACT_EXIT"' + "`n"
}
Set-Content -LiteralPath $launcher -Value $fakeGradle -Encoding ascii
if (-not $onWindows) { & chmod +x $launcher }

function Reset-Version {
    Set-Content -LiteralPath $versionFile -Value @("versionName=1.2.3", "versionCode=10") -Encoding utf8
}
function Assert-True([bool]$condition, [string]$description) {
    if (-not $condition) { throw "FAILED: $description. Fixture: $fixtureRoot" }
    Write-Host "PASS: $description"
}
function Invoke-Fixture([string]$bump = "patch", [bool]$acknowledgeTestKey = $true, [string]$variant = "demo") {
    $process = [System.Diagnostics.ProcessStartInfo]::new($pwsh)
    foreach ($argument in @("-NoProfile", "-File", $fixtureScript, "-Bump", $bump, "-Variant", $variant)) {
        $process.ArgumentList.Add($argument)
    }
    if ($acknowledgeTestKey) { $process.ArgumentList.Add("-AllowPublicTestKey") }
    if ($variant -eq "release") {
        foreach ($name in @("SIGNING_STORE_FILE", "SIGNING_STORE_PASSWORD", "SIGNING_KEY_ALIAS", "SIGNING_KEY_PASSWORD")) {
            $process.Environment.Remove($name) | Out-Null
        }
    }
    $process.UseShellExecute = $false
    $process.RedirectStandardOutput = $true
    $process.RedirectStandardError = $true
    $child = [System.Diagnostics.Process]::Start($process)
    $outputTask = $child.StandardOutput.ReadToEndAsync()
    $errorTask = $child.StandardError.ReadToEndAsync()
    $child.WaitForExit()
    $null = $outputTask.GetAwaiter().GetResult()
    $script:lastFixtureError = $errorTask.GetAwaiter().GetResult()
    return $child.ExitCode
}

$previousExit = $env:JVY_CONTRACT_EXIT
try {
    Reset-Version
    $baseline = Get-Content -LiteralPath $versionFile -Raw
    Assert-True ((Invoke-Fixture "patch" $false) -ne 0) "Demo signing requires explicit public test-key acknowledgement"
    Assert-True ($script:lastFixtureError -match "publicly available test signing key") "The public-key blocker explains its risk"
    Assert-True ((Get-Content -LiteralPath $versionFile -Raw) -eq $baseline) "Signing refusal does not allocate a version"
    Assert-True ((Invoke-Fixture "patch" $false "release") -ne 0) "Private release requires signing credentials"
    Assert-True ($script:lastFixtureError -match "SIGNING_STORE_PASSWORD") "Missing release credentials fail before Gradle"
    Assert-True ((Invoke-Fixture "patch" $false "debug") -ne 0) "Numbered workflow rejects debug APK variants"
    $env:JVY_CONTRACT_EXIT = "1"
    Assert-True ((Invoke-Fixture) -ne 0) "Gradle failure is reported"
    Assert-True ((Get-Content -LiteralPath $versionFile -Raw) -eq $baseline) "Failed build does not allocate a version"
    $env:JVY_CONTRACT_EXIT = "0"
    Assert-True ((Invoke-Fixture) -ne 0) "Missing APK is rejected"
    Assert-True ((Get-Content -LiteralPath $versionFile -Raw) -eq $baseline) "Missing APK does not allocate a version"
    $sourceDirectory = Join-Path $fixtureRoot "app/build/outputs/apk/demo"
    New-Item -ItemType Directory -Path $sourceDirectory -Force | Out-Null
    [System.IO.File]::WriteAllBytes((Join-Path $sourceDirectory "app-demo.apk"), [byte[]]@())
    Assert-True ((Invoke-Fixture) -ne 0) "Empty APK is rejected"
    [System.IO.File]::WriteAllBytes((Join-Path $sourceDirectory "app-demo.apk"), [byte[]]@(1, 2, 3))
    Assert-True ((Invoke-Fixture "minor") -eq 0) "Minor build completes with native launcher"
    $manifest = Get-Content -LiteralPath (Join-Path $artifactDirectory "Jvy_wallet_v1.3.0.json") -Raw | ConvertFrom-Json
    Assert-True ($manifest.version -eq "1.3.0" -and $manifest.versionCode -eq 11) "Feature version and code increase exactly once"
    Assert-True ($manifest.signingKeyType -eq "legacy-public-test-only") "Manifest labels public test-key artifacts explicitly"
    $hash = (Get-FileHash -LiteralPath (Join-Path $artifactDirectory "Jvy_wallet_v1.3.0.apk") -Algorithm SHA256).Hash
    Assert-True ($hash -eq $manifest.sha256) "Numbered artifact checksum matches manifest"
    Reset-Version
    Assert-True ((Invoke-Fixture "minor") -ne 0) "Existing artifact cannot be overwritten"
    Assert-True ((Get-Content -LiteralPath $versionFile -Raw) -eq $baseline) "Collision preserves baseline"
    $lock = [System.IO.File]::Open((Join-Path $fixtureRoot ".gradle/jvy-apk-build.lock"), "Open", "ReadWrite", "None")
    try {
        Assert-True ((Invoke-Fixture) -ne 0) "Concurrent allocation is blocked"
    } finally { $lock.Dispose() }
    Assert-True ((Invoke-Fixture) -eq 0) "Patch build succeeds after lock release"
    Write-Host "All build workflow contract tests passed. Fixtures: $fixtureRoot"
} finally {
    $env:JVY_CONTRACT_EXIT = $previousExit
}
