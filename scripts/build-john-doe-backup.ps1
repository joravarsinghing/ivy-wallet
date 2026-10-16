param(
    [string]$FixturePath = (Join-Path (Split-Path -Parent $PSScriptRoot) "docs\fixtures\john-doe-render-fixture.json"),
    [string]$OutputPath = (Join-Path (Split-Path -Parent $PSScriptRoot) "artifacts\fixtures\John_Doe_Jvy_Backup.zip")
)

$ErrorActionPreference = "Stop"

function Convert-ToEpochMillis([object]$value) {
    if ($null -eq $value -or [string]::IsNullOrWhiteSpace([string]$value)) {
        return $null
    }
    if ($value -is [DateTime]) {
        return ([DateTimeOffset]$value).ToUnixTimeMilliseconds()
    }
    return [DateTimeOffset]::Parse([string]$value).ToUnixTimeMilliseconds()
}

if (-not (Test-Path -LiteralPath $FixturePath)) {
    throw "Fixture not found: $FixturePath"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$outputDirectory = Split-Path -Parent $OutputPath
$stagingDirectory = Join-Path $repoRoot "artifacts\fixtures\john-doe-backup-staging"
$jsonPath = Join-Path $stagingDirectory "data.json"
$rawJsonPath = Join-Path $outputDirectory "John_Doe_Jvy_Backup.json"
$manifestPath = Join-Path $outputDirectory "John_Doe_Jvy_Backup.manifest.json"

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
if (Test-Path -LiteralPath $stagingDirectory) {
    Remove-Item -LiteralPath $stagingDirectory -Recurse -Force
}
New-Item -ItemType Directory -Path $stagingDirectory -Force | Out-Null

$fixture = Get-Content -Raw -LiteralPath $FixturePath | ConvertFrom-Json

$transactions = @($fixture.transactions | ForEach-Object {
    $item = $_ | Select-Object *
    foreach ($property in @("dateTime", "dueDate", "paidForDateTime")) {
        if ($item.PSObject.Properties.Name -contains $property) {
            $item.$property = Convert-ToEpochMillis $item.$property
        }
    }
    $item
})

$plannedPayments = @($fixture.plannedPaymentRules | ForEach-Object {
    $item = $_ | Select-Object *
    $item.startDate = Convert-ToEpochMillis $item.startDate
    $item
})

$loans = @($fixture.loans | ForEach-Object {
    $item = $_ | Select-Object *
    # Keep the legacy Ivy backup envelope. Older importers do not expect the
    # optional loan dateTime field, even though newer database entities have it.
    if ($item.PSObject.Properties.Name -contains "dateTime") {
        $item.PSObject.Properties.Remove("dateTime")
    }
    $item
})

$loanRecords = @($fixture.loanRecords | ForEach-Object {
    $item = $_ | Select-Object *
    $item.dateTime = Convert-ToEpochMillis $item.dateTime
    $item
})

$settings = @($fixture.settings | ForEach-Object {
    $_ | Select-Object *
})

# This intentionally uses the conservative legacy envelope used by Ivy's
# existing import path. Goals, allocations, tags, and category rules are
# render-fixture data today; they are not part of IvyWalletCompleteData.
$backup = [ordered]@{
    accounts = @($fixture.accounts)
    budgets = @($fixture.budgets)
    categories = @($fixture.categories)
    loanRecords = $loanRecords
    loans = $loans
    plannedPaymentRules = $plannedPayments
    settings = $settings
    transactions = $transactions
    sharedPrefs = $fixture.sharedPrefs
}

$json = $backup | ConvertTo-Json -Depth 30 -Compress
Set-Content -LiteralPath $jsonPath -Value $json -Encoding Unicode
Copy-Item -LiteralPath $jsonPath -Destination $rawJsonPath -Force

if (Test-Path -LiteralPath $OutputPath) {
    Remove-Item -LiteralPath $OutputPath -Force
}
Compress-Archive -LiteralPath $jsonPath -DestinationPath $OutputPath -CompressionLevel Optimal

$zipEntries = Get-Item -LiteralPath $OutputPath
$hash = (Get-FileHash -LiteralPath $OutputPath -Algorithm SHA256).Hash
$summary = [ordered]@{
    artifact = $zipEntries.FullName
    sizeBytes = $zipEntries.Length
    sha256 = $hash
    jsonEncoding = "UTF-16 LE"
    jsonFiles = 1
    rawJson = $rawJsonPath
    accounts = @($backup.accounts).Count
    categories = @($backup.categories).Count
    budgets = @($backup.budgets).Count
    transactions = @($backup.transactions).Count
    plannedPaymentRules = @($backup.plannedPaymentRules).Count
    loans = @($backup.loans).Count
    tags = 0
}
$summary | ConvertTo-Json | Set-Content -LiteralPath $manifestPath -Encoding utf8

Remove-Item -LiteralPath $stagingDirectory -Recurse -Force
$summary | ConvertTo-Json
