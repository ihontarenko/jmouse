param(
    [switch] $Build,
    [int] $Port = 8092
)

$ErrorActionPreference = 'Stop'
$applicationRoot = $PSScriptRoot
$repositoryRoot = Split-Path -Parent $applicationRoot
$jarFile = Join-Path $applicationRoot 'target/jmouse-showcase-1.0.1.jar'

if ($Build -or -not (Test-Path -LiteralPath $jarFile)) {
    Push-Location $repositoryRoot
    try {
        & mvn -o -pl jmouse-showcase -am package '-DskipTests' '-Dmaven.javadoc.skip=true' '-Dgpg.skip=true'
        if ($LASTEXITCODE -ne 0) {
            throw 'Showcase build failed'
        }
    } finally {
        Pop-Location
    }
}

$env:JMOUSE_SHOWCASE_PORT = [string] $Port
Push-Location $applicationRoot
try {
    & java '-Djmouse.web.enable=true' '-Djmouse.mvc.internal-controller.enabled=true' '-Djmouse.web.app=showcase' -jar $jarFile
} finally {
    Pop-Location
}
