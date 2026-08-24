param(
    [Parameter(Position = 0)]
    [string]$Profile = "system"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$toolsPom = Join-Path $PSScriptRoot "pom.xml"
$codegenPom = Join-Path $PSScriptRoot "mybatis-flex-codegen/pom.xml"
$runningOnWindows = $env:OS -eq "Windows_NT"
$mvnw = if ($runningOnWindows) { Join-Path $repoRoot "mvnw.cmd" } else { Join-Path $repoRoot "mvnw" }

if (-not (Test-Path $mvnw)) {
    throw "Maven Wrapper not found: $mvnw"
}

Push-Location $repoRoot
try {
    & $mvnw -f $toolsPom -pl :mybatis-flex-codegen -am install -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Codegen bootstrap failed with exit code $LASTEXITCODE"
    }

    & $mvnw -f $codegenPom compile exec:java "-Dexec.args=$Profile"
    if ($LASTEXITCODE -ne 0) {
        throw "Codegen failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}
