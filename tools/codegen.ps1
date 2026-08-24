param(
    [Parameter(Position = 0)]
    [string]$Profile = "system"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$pom = Join-Path $PSScriptRoot "mybatis-flex-codegen/pom.xml"
$isWindows = $env:OS -eq "Windows_NT"
$mvnw = if ($isWindows) { Join-Path $repoRoot "mvnw.cmd" } else { Join-Path $repoRoot "mvnw" }

if (-not (Test-Path $mvnw)) {
    throw "Maven Wrapper not found: $mvnw"
}

Push-Location $repoRoot
try {
    & $mvnw -f $pom compile exec:java "-Dexec.args=$Profile"
    if ($LASTEXITCODE -ne 0) {
        throw "Codegen failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}
