param(
    [string]$Version,
    [switch]$RequireReleaseVersion,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$properties = Join-Path $root 'gradle.properties'
if (!$Version) {
    $versionLine = Select-String -LiteralPath $properties -Pattern '^mod_version=(.+)$'
    $Version = $versionLine.Matches[0].Groups[1].Value.Trim()
}
if ($Version -cnotmatch '^\d+\.\d+\.\d+(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?$') {
    throw "Invalid Requiem version: $Version"
}
if ($RequireReleaseVersion -and $Version.Contains('-')) {
    throw 'Publishing requires a stable X.Y.Z version.'
}

Push-Location $root
try {
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
    foreach ($jar in $jars) { Copy-Item -LiteralPath $jar.FullName -Destination $stage.FullName -Force }
    Copy-Item -LiteralPath 'RELEASE-NOTES.md' -Destination $stage.FullName -Force
    $checksums = foreach ($jar in $jars) {
        $hash = Get-FileHash -LiteralPath (Join-Path $stage.FullName $jar.Name)
        "$($hash.Hash.ToLowerInvariant())  $($jar.Name)"
    }
    $checksums | Set-Content -LiteralPath (Join-Path $stage.FullName 'SHA256SUMS') -Encoding utf8
    Write-Host "Verified release files staged in $($stage.FullName)"
} finally { Pop-Location }
