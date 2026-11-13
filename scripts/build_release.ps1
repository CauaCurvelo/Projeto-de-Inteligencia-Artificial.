$ErrorActionPreference = 'Stop'

$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\jpackage.exe'))) {
        return $env:JAVA_HOME
    }
    $jdkCandidates = Get-ChildItem 'C:\Program Files\Microsoft\jdk-17*' -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending
    foreach ($dir in $jdkCandidates) {
        if (Test-Path (Join-Path $dir.FullName 'bin\jpackage.exe')) {
            return $dir.FullName
        }
    }
    $jpkg = Get-Command jpackage.exe -ErrorAction SilentlyContinue
    if ($jpkg) {
        return Split-Path -Parent (Split-Path -Parent $jpkg.Source)
    }
    throw 'JDK 17 com jpackage.exe nao encontrado. Defina JAVA_HOME.'
}

function Resolve-MavenCmd {
    $mvnCmd = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($mvnCmd) { return $mvnCmd.Source }
    $localMvn = 'C:\Users\pedro\.gemini\antigravity\scratch\apache-maven-3.9.6\bin\mvn.cmd'
    if (Test-Path $localMvn) { return $localMvn }
    throw 'Apache Maven (mvn.cmd) nao encontrado no PATH.'
}

$JavaHome = Resolve-JavaHome
$env:JAVA_HOME = $JavaHome
$JpackageExe = Join-Path $JavaHome 'bin\jpackage.exe'
$MvnCmd = Resolve-MavenCmd

Write-Host '[1/4] Compilando e empacotando JAR do jogo...'
& $MvnCmd -f (Join-Path $RepoRoot 'pom.xml') clean package -q
if ($LASTEXITCODE -ne 0) { throw "Maven build falhou (exit $LASTEXITCODE)." }

Write-Host '[2/4] Preparando module-path limpo (JAR + JavaFX modular)...'
$CpFile = Join-Path $RepoRoot 'target\release-cp.txt'
& $MvnCmd -f (Join-Path $RepoRoot 'pom.xml') dependency:build-classpath "-Dmdep.outputFile=$CpFile" -q
if ($LASTEXITCODE -ne 0) { throw "Falha ao resolver classpath (exit $LASTEXITCODE)." }

$ModuleStage = Join-Path $RepoRoot 'target\release-modules'
if (Test-Path $ModuleStage) { Remove-Item $ModuleStage -Recurse -Force }
New-Item -ItemType Directory -Path $ModuleStage | Out-Null

$JarFiles = Get-ChildItem (Join-Path $RepoRoot 'target\*.jar') |
    Where-Object { $_.Name -notmatch '^(original-|sources|javadoc)' }
foreach ($jar in $JarFiles) {
    Copy-Item $jar.FullName -Destination $ModuleStage
}

$DepEntries = (Get-Content $CpFile -Raw) -split ';' | Where-Object { $_ -and (Test-Path $_) }
$WinJarBaseNames = @{}
foreach ($p in $DepEntries) {
    $name = Split-Path $p -Leaf
    if ($name -match '^(.+)-win\.jar$') {
        $WinJarBaseNames[$matches[1]] = $true
    }
}
foreach ($p in $DepEntries) {
    $name = Split-Path $p -Leaf
    if ($name -match '^(.+)\.jar$' -and $WinJarBaseNames.ContainsKey($matches[1])) {
        continue # ignora jar wrapper vazio do OpenJFX quando -win.jar esta presente
    }
    Copy-Item $p -Destination $ModuleStage
}

Write-Host '[3/4] Gerando executavel portatil com JRE embutido via jpackage...'
$JpkgOut = Join-Path $RepoRoot 'target\jpackage-out'
if (Test-Path $JpkgOut) { Remove-Item $JpkgOut -Recurse -Force }

$IconPath = Join-Path $RepoRoot 'packaging\InstintoSelvagem.ico'
$JpkgArgs = @(
    '--type', 'app-image',
    '--name', 'InstintoSelvagem',
    '--app-version', '1.0.0',
    '--vendor', 'UNEX - Sentinela',
    '--description', 'Instinto Selvagem (Wild Instinct)',
    '--module-path', $ModuleStage,
    '--module', 'br.edu.unex.sentinela/br.edu.unex.sentinela.app.Main',
    '--dest', $JpkgOut
)
if (Test-Path $IconPath) {
    $JpkgArgs += @('--icon', $IconPath)
}

& $JpackageExe @JpkgArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage falhou (exit $LASTEXITCODE)." }

Write-Host '[4/4] Empacotando distribuicao ZIP em dist\InstintoSelvagem-Windows.zip...'
$AppDir = Join-Path $JpkgOut 'InstintoSelvagem'
Copy-Item (Join-Path $RepoRoot 'packaging\LEIA-ME.txt') -Destination $AppDir -Force

$DistDir = Join-Path $RepoRoot 'dist'
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null

$DistAppDir = Join-Path $DistDir 'InstintoSelvagem'
if (Test-Path $DistAppDir) { Remove-Item $DistAppDir -Recurse -Force }
Copy-Item $AppDir -Destination $DistAppDir -Recurse

$ZipPath = Join-Path $DistDir 'InstintoSelvagem-Windows.zip'
if (Test-Path $ZipPath) { Remove-Item $ZipPath -Force }
Compress-Archive -Path $DistAppDir -DestinationPath $ZipPath -CompressionLevel Optimal

$ZipSizeMb = [math]::Round((Get-Item $ZipPath).Length / 1MB, 1)
Write-Host "Build portatil pronto: $ZipPath ($ZipSizeMb MB)"
