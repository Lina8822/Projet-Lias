param([string]$TomcatHome = $env:CATALINA_HOME, [string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if (!$JavaHome) { $JavaHome = 'C:\Program Files\Java\jdk-26.0.1' }
if (!$TomcatHome) { $TomcatHome = 'C:\Users\ELECTRO-RIMO\Downloads\apache-tomcat-11.0.21-windows-x64\apache-tomcat-11.0.21' }
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
foreach ($liasGenerated in @('build/classes','build/war')) {
    $liasTarget = [IO.Path]::GetFullPath((Join-Path $root $liasGenerated))
    $liasExpected = [IO.Path]::GetFullPath((Join-Path $root 'build')) + [IO.Path]::DirectorySeparatorChar
    if (!$liasTarget.StartsWith($liasExpected, [StringComparison]::OrdinalIgnoreCase)) { throw 'Dossier de compilation invalide' }
    if (Test-Path -LiteralPath $liasTarget) { Remove-Item -LiteralPath $liasTarget -Recurse -Force }
}
New-Item -ItemType Directory -Force build/classes,build/war,dist | Out-Null
$sources = Get-ChildItem src/main/java -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
& "$JavaHome/bin/javac.exe" -encoding UTF-8 --release 17 -cp "$TomcatHome/lib/servlet-api.jar" -d build/classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation Java échouée' }
Copy-Item src/main/webapp/* build/war -Recurse -Force
New-Item -ItemType Directory -Force build/war/WEB-INF/classes | Out-Null
Copy-Item build/classes/* build/war/WEB-INF/classes -Recurse -Force
& "$JavaHome/bin/jar.exe" --create --file dist/Lias-Jee.war -C build/war .
if ($LASTEXITCODE -ne 0) { throw 'Création du WAR échouée' }
Write-Output 'Compilation Java réussie : dist/Lias-Jee.war'
