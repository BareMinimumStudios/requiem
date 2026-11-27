param(
    [string]$Version,

    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$properties = Join-Path $root 'gradle.properties'
if (!$Version) {
    $versionLine = Select-String -LiteralPath $properties -Pattern '^mod_version=(.+)$'
    $Version = $versionLine.Matches[0].Groups[1].Value.Trim()
}
if ($Version -cnotmatch '^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$') {
    throw "Requiem requires a stable X.Y.Z version: $Version"
}

Push-Location $root
try {
    $changelog = Get-Content -LiteralPath 'CHANGELOG.md' -Raw
    $heading = '(?m)^## \[' + [regex]::Escape($Version) + '\] - \d{4}-\d{2}-\d{2}[ \t]*\r?$'
    $matchesForVersion = [regex]::Matches($changelog, $heading)
    if ($matchesForVersion.Count -ne 1) { throw "Expected one dated changelog entry for $Version." }
    $entry = $matchesForVersion[0]
    $remaining = $changelog.Substring($entry.Index + $entry.Length)
    $notes = ($remaining -split '(?m)^(?:## |\[[^\]\r\n]+\]:)', 2)[0].Trim()
    if ($notes -notmatch '(?m)^### (Added|Changed|Deprecated|Removed|Fixed|Security)\s*$' -or $notes -notmatch '(?m)^- \S') {
        throw "The changelog entry for $Version needs a category and at least one change."
    }

    $wrapper = if ($IsWindows) { '.\gradlew.bat' } else { './gradlew' }
    $arguments = @('build', '--console=plain', "-Pmod_version=$Version")
    if ($Offline) { $arguments += '--offline' }
    & $wrapper @arguments
    if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed.' }

    $icon = 'assets/requiem_cinematics/icon.png'
    $expectedIcon = (Get-FileHash -LiteralPath 'src/main/resources/assets/requiem_cinematics/icon.png').Hash
    $jars = foreach ($loader in 'fabric', 'neoforge') {
        $jar = Get-Item -LiteralPath "build/libs/Requiem-$Version-$loader-1.21.1.jar"
        $archive = [IO.Compression.ZipFile]::OpenRead($jar.FullName)
        try {
            $metadata = if ($loader -eq 'fabric') { 'fabric.mod.json' } else { 'META-INF/neoforge.mods.toml' }
            $entry = $archive.GetEntry($metadata)
            if (!$entry) { throw "Missing $metadata in $($jar.Name)" }
            $reader = [IO.StreamReader]::new($entry.Open())
            try { $body = $reader.ReadToEnd() } finally { $reader.Dispose() }
            if ($loader -eq 'fabric') {
                $mod = ConvertFrom-Json $body
                if ($mod.id -cne 'requiem_cinematics' -or $mod.version -cne $Version -or $mod.icon -cne $icon) {
                    throw "Unexpected Fabric metadata in $($jar.Name)"
                }
            } else {
                foreach ($pair in @{ modId = 'requiem_cinematics'; version = $Version; logoFile = $icon }.GetEnumerator()) {
                    $pattern = '(?m)^\s*' + $pair.Key + '\s*=\s*"' + [regex]::Escape($pair.Value) + '"\s*$'
                    if ($body -cnotmatch $pattern) { throw "Unexpected NeoForge $($pair.Key) in $($jar.Name)" }
                }
            }
            $image = $archive.GetEntry($icon)
            if (!$image) { throw "Missing icon in $($jar.Name)" }
            $stream = $image.Open()
            try { $actualIcon = (Get-FileHash -InputStream $stream).Hash } finally { $stream.Dispose() }
            if ($actualIcon -ne $expectedIcon) { throw "Incorrect icon in $($jar.Name)" }
            $jar
        } finally { $archive.Dispose() }
    }

    $stage = New-Item -ItemType Directory -Path "build/release/$Version" -Force


    $stagedJars = foreach ($loader in 'fabric', 'neoforge') {
        $source = $jars | Where-Object Name -CEQ "Requiem-$Version-$loader-1.21.1.jar"
        $name = "Requiem-$Version+1.21.1-$loader.jar"
        $target = Join-Path $stage.FullName $name
        Copy-Item -LiteralPath $source.FullName -Destination $target -Force
        Get-Item -LiteralPath $target
    }
    @("# Requiem $Version", '', $notes) | Set-Content -LiteralPath (Join-Path $stage.FullName 'RELEASE-NOTES.md') -Encoding utf8
    $checksums = foreach ($jar in $stagedJars) {
        $hash = Get-FileHash -LiteralPath (Join-Path $stage.FullName $jar.Name)
        "$($hash.Hash.ToLowerInvariant())  $($jar.Name)"
    }
    $checksums | Set-Content -LiteralPath (Join-Path $stage.FullName 'SHA256SUMS') -Encoding utf8
    Write-Host "Verified release files staged in $($stage.FullName)"
} finally { Pop-Location }
